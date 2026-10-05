package com.staynest.paymentservice.service;

import com.staynest.paymentservice.client.BookingServiceClient;
import com.staynest.paymentservice.dto.BookingResponse;
import com.staynest.paymentservice.dto.PaymentRequest;
import com.staynest.paymentservice.dto.PaymentResponse;
import com.staynest.paymentservice.entity.Payment;
import com.staynest.paymentservice.entity.PaymentStatus;
import com.staynest.paymentservice.exception.DuplicatePaymentException;
import com.staynest.paymentservice.exception.InvalidPaymentRequestException;
import com.staynest.paymentservice.exception.InvalidPaymentStateException;
import com.staynest.paymentservice.exception.PaymentNotFoundException;
import com.staynest.paymentservice.exception.RefundNotAllowedException;
import com.staynest.paymentservice.repository.PaymentRepository;
import com.staynest.paymentservice.security.PaymentUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingServiceClient bookingServiceClient;
    private final MockPaymentProcessor mockPaymentProcessor;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public PaymentResponse create(PaymentRequest request, PaymentUserPrincipal user, String authorization) {
        if (!hasRole(user, "CUSTOMER") && !hasRole(user, "ADMIN")) {
            throw new AccessDeniedException("Only customers or admins can create payments");
        }
        validateCurrency(request.getCurrency());

        BookingResponse booking = bookingServiceClient.getBooking(request.getBookingId(), authorization);
        if (!hasRole(user, "ADMIN") && !user.getId().equals(booking.customerId())) {
            throw new AccessDeniedException("You cannot pay for another customer's booking");
        }

        lockBooking(request.getBookingId());
        booking = bookingServiceClient.getBooking(request.getBookingId(), authorization);
        if (!hasRole(user, "ADMIN") && !user.getId().equals(booking.customerId())) {
            throw new AccessDeniedException("You cannot pay for another customer's booking");
        }
        if (!"UNPAID".equals(booking.paymentStatus())) {
            throw new DuplicatePaymentException("Booking payment status is already " + booking.paymentStatus());
        }
        if (paymentRepository.existsByBookingIdAndStatus(request.getBookingId(), PaymentStatus.SUCCESS)
                || paymentRepository.existsByBookingIdAndStatus(request.getBookingId(), PaymentStatus.REFUNDED)) {
            throw new DuplicatePaymentException("A successful payment already exists for this booking");
        }

        Payment payment = Payment.builder()
                .bookingId(booking.id())
                .userId(booking.customerId())
                .amount(booking.totalAmount())
                .currency(request.getCurrency())
                .paymentMethod(request.getPaymentMethod())
                .status(PaymentStatus.INITIATED)
                .transactionReference("MOCK-" + UUID.randomUUID())
                .build();
        payment = paymentRepository.saveAndFlush(payment);

        MockPaymentProcessor.MockResult result = mockPaymentProcessor.execute(request.isSimulateFailure());
        if (!result.successful()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(result.failureReason());
            return toResponse(paymentRepository.save(payment));
        }

        bookingServiceClient.updatePaymentStatus(booking.id(), "PAID", authorization);
        payment.setStatus(PaymentStatus.SUCCESS);
        return toResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id, PaymentUserPrincipal user, String authorization) {
        Payment payment = findPayment(id);
        authorizePaymentOwner(payment, user);
        return toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getByBooking(Long bookingId, PaymentUserPrincipal user, String authorization) {
        BookingResponse booking = bookingServiceClient.getBooking(bookingId, authorization);
        if (!hasRole(user, "ADMIN") && !user.getId().equals(booking.customerId())) {
            throw new AccessDeniedException("You cannot view another customer's payment information");
        }
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getMyPayments(PaymentUserPrincipal user) {
        if (!hasRole(user, "CUSTOMER") && !hasRole(user, "ADMIN")) {
            throw new AccessDeniedException("Only customers or admins can view payment history");
        }
        if (hasRole(user, "ADMIN")) {
            return paymentRepository.findAll().stream().map(this::toResponse).toList();
        }
        return paymentRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse refund(Long id, PaymentUserPrincipal user, String authorization) {
        Payment payment = paymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new PaymentNotFoundException(id));
        authorizePaymentOwner(payment, user);
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            throw new InvalidPaymentStateException("Payment has already been refunded");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new RefundNotAllowedException("Only successful payments can be refunded");
        }
        bookingServiceClient.updatePaymentStatus(payment.getBookingId(), "REFUNDED", authorization);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setFailureReason(null);
        return toResponse(paymentRepository.save(payment));
    }

    private Payment findPayment(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new PaymentNotFoundException(id));
    }

    private void authorizePaymentOwner(Payment payment, PaymentUserPrincipal user) {
        if (!hasRole(user, "ADMIN")
                && (!hasRole(user, "CUSTOMER") || !payment.getUserId().equals(user.getId()))) {
            throw new AccessDeniedException("You cannot access another customer's payment");
        }
    }

    private void validateCurrency(String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            if (!currency.getCurrencyCode().equals(currencyCode)) {
                throw new InvalidPaymentRequestException("Currency must be a valid uppercase ISO 4217 code");
            }
        } catch (IllegalArgumentException ex) {
            throw new InvalidPaymentRequestException("Currency must be a valid uppercase ISO 4217 code");
        }
    }

    private void lockBooking(Long bookingId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql")) {
                try (var statement = connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
                    statement.setLong(1, bookingId);
                    statement.execute();
                }
            }
            return null;
        });
    }

    private boolean hasRole(PaymentUserPrincipal user, String role) {
        return user.getRoles().contains(role);
    }

    private PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .bookingId(payment.getBookingId())
                .customerId(payment.getUserId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .failureReason(payment.getFailureReason())
                .createdAt(payment.getCreatedAt())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}
