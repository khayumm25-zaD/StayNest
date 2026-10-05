package com.staynest.bookingservice.service;

import com.staynest.bookingservice.client.PropertyServiceClient;
import com.staynest.bookingservice.dto.BookingRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityResponse;
import com.staynest.bookingservice.dto.BookingResponse;
import com.staynest.bookingservice.dto.PropertyResponse;
import com.staynest.bookingservice.entity.Booking;
import com.staynest.bookingservice.entity.BookingStatus;
import com.staynest.bookingservice.entity.PaymentStatus;
import com.staynest.bookingservice.exception.BookingNotFoundException;
import com.staynest.bookingservice.exception.InvalidBookingRequestException;
import com.staynest.bookingservice.exception.InvalidBookingStatusException;
import com.staynest.bookingservice.exception.PropertyUnavailableException;
import com.staynest.bookingservice.repository.BookingRepository;
import com.staynest.bookingservice.security.BookingUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final PropertyServiceClient propertyServiceClient;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public BookingAvailabilityResponse checkAvailability(BookingAvailabilityRequest request) {
        validateDates(request.checkInDate(), request.checkOutDate());
        PropertyResponse property = propertyServiceClient.getProperty(request.propertyId());
        long nights = ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate());
        BigDecimal estimatedTotal = property.pricePerNight().multiply(BigDecimal.valueOf(nights));
        if ((property.status() != null && !"ACTIVE".equalsIgnoreCase(property.status()))
                || (property.maxGuests() != null && request.numberOfGuests() > property.maxGuests())) {
            return new BookingAvailabilityResponse(false, nights, property.pricePerNight(), estimatedTotal);
        }

        acquirePropertyBookingLock(request.propertyId());
        boolean available = bookingRepository.findOverlappingBookingsForUpdate(
                request.propertyId(), request.checkInDate(), request.checkOutDate(), BookingStatus.CANCELLED).isEmpty();
        return new BookingAvailabilityResponse(available, nights, property.pricePerNight(), estimatedTotal);
    }

    @Override
    @Transactional
    public BookingResponse create(BookingRequest request, BookingUserPrincipal user) {
        if (!hasRole(user, "CUSTOMER") && !hasRole(user, "ADMIN")) {
            throw new AccessDeniedException("Only customers can create bookings");
        }
        validateDates(request.getCheckInDate(), request.getCheckOutDate());

        PropertyResponse property = propertyServiceClient.getProperty(request.getPropertyId());
        if (property.status() != null && !"ACTIVE".equalsIgnoreCase(property.status())) {
            throw new PropertyUnavailableException("Property is not available for booking");
        }
        if (property.maxGuests() != null && request.getNumberOfGuests() > property.maxGuests()) {
            throw new InvalidBookingRequestException("Guest count exceeds property capacity");
        }

        acquirePropertyBookingLock(request.getPropertyId());
        List<Booking> overlapping = bookingRepository.findOverlappingBookingsForUpdate(
                request.getPropertyId(), request.getCheckInDate(), request.getCheckOutDate(), BookingStatus.CANCELLED);
        if (!overlapping.isEmpty()) {
            throw new PropertyUnavailableException("Property is already booked for the selected dates");
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());
        BigDecimal totalAmount = property.pricePerNight().multiply(BigDecimal.valueOf(nights));
        Booking booking = Booking.builder()
                .propertyId(request.getPropertyId())
                .guestId(user.getId())
                .checkInDate(request.getCheckInDate())
                .checkOutDate(request.getCheckOutDate())
                .numberOfGuests(request.getNumberOfGuests())
                .totalAmount(totalAmount)
                .status(BookingStatus.PENDING)
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getById(Long id, BookingUserPrincipal user) {
        Booking booking = findBooking(id);
        authorizeBookingRead(booking, user);
        return toResponse(booking);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(BookingUserPrincipal user) {
        if (!hasRole(user, "CUSTOMER") && !hasRole(user, "ADMIN")) {
            throw new AccessDeniedException("Only customers can view their bookings");
        }
        return bookingRepository.findByGuestIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getHostBookings(BookingUserPrincipal user) {
        if (!hasRole(user, "HOST") && !hasRole(user, "ADMIN")) {
            throw new AccessDeniedException("Only hosts can view host bookings");
        }
        if (hasRole(user, "ADMIN")) {
            return bookingRepository.findAll().stream().map(this::toResponse).toList();
        }
        List<Long> propertyIds = propertyServiceClient.getPropertiesForHost(user.getId());
        if (propertyIds.isEmpty()) {
            return List.of();
        }
        return bookingRepository.findByPropertyIdInOrderByCheckInDateAsc(propertyIds).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getPropertyBookings(Long propertyId, BookingUserPrincipal user) {
        if (hasRole(user, "ADMIN")) {
            return bookingRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).stream()
                    .map(this::toResponse).toList();
        }
        if (!hasRole(user, "HOST")) {
            throw new AccessDeniedException("Only the property's host or an admin can view its bookings");
        }
        PropertyResponse property = propertyServiceClient.getProperty(propertyId);
        if (!user.getId().equals(property.hostId())) {
            throw new AccessDeniedException("You do not own this property");
        }
        return bookingRepository.findByPropertyIdOrderByCheckInDateAsc(propertyId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public BookingResponse cancel(Long id, BookingUserPrincipal user) {
        Booking booking = findBooking(id);
        if (!hasRole(user, "ADMIN")
                && (!hasRole(user, "CUSTOMER") || !booking.getGuestId().equals(user.getId()))) {
            throw new AccessDeniedException("You cannot cancel another customer's booking");
        }
        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStatusException("Only pending or confirmed bookings can be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updateStatus(Long id, BookingStatus status, BookingUserPrincipal user) {
        Booking booking = findBooking(id);
        if (!hasRole(user, "ADMIN")) {
            if (!hasRole(user, "HOST")) {
                throw new AccessDeniedException("Only a host or admin can update booking status");
            }
            PropertyResponse property = propertyServiceClient.getProperty(booking.getPropertyId());
            if (!user.getId().equals(property.hostId())) {
                throw new AccessDeniedException("You do not own this property's booking");
            }
        }
        if (status == BookingStatus.CANCELLED) {
            throw new InvalidBookingStatusException("Use the cancellation endpoint to cancel a booking");
        }
        if (!isAllowedTransition(booking.getStatus(), status)) {
            throw new InvalidBookingStatusException(
                    "Cannot change booking status from " + booking.getStatus() + " to " + status);
        }
        booking.setStatus(status);
        return toResponse(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus, BookingUserPrincipal user) {
        Booking booking = findBooking(id);
        if (!hasRole(user, "ADMIN")
                && (!hasRole(user, "CUSTOMER") || !booking.getGuestId().equals(user.getId()))) {
            throw new AccessDeniedException("Only the booking customer or an admin can update payment status");
        }
        boolean validTransition = (booking.getPaymentStatus() == PaymentStatus.UNPAID
                && paymentStatus == PaymentStatus.PAID)
                || (booking.getPaymentStatus() == PaymentStatus.PAID
                && paymentStatus == PaymentStatus.REFUNDED);
        if (booking.getPaymentStatus() != paymentStatus && !validTransition) {
            throw new InvalidBookingStatusException(
                    "Cannot change payment status from " + booking.getPaymentStatus() + " to " + paymentStatus);
        }
        booking.setPaymentStatus(paymentStatus);
        return toResponse(bookingRepository.save(booking));
    }

    private void authorizeBookingRead(Booking booking, BookingUserPrincipal user) {
        if (hasRole(user, "ADMIN")) {
            return;
        }
        if (hasRole(user, "CUSTOMER") && booking.getGuestId().equals(user.getId())) {
            return;
        }
        if (hasRole(user, "HOST")) {
            PropertyResponse property = propertyServiceClient.getProperty(booking.getPropertyId());
            if (user.getId().equals(property.hostId())) {
                return;
            }
        }
        throw new AccessDeniedException("You cannot view this booking");
    }

    private Booking findBooking(Long id) {
        return bookingRepository.findById(id).orElseThrow(() -> new BookingNotFoundException(id));
    }

    private boolean isAllowedTransition(BookingStatus current, BookingStatus requested) {
        return (current == BookingStatus.PENDING && requested == BookingStatus.CONFIRMED)
                || (current == BookingStatus.CONFIRMED && requested == BookingStatus.COMPLETED);
    }

    private boolean hasRole(BookingUserPrincipal user, String role) {
        return user.getRoles().contains(role);
    }

    private void validateDates(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null) {
            throw new InvalidBookingRequestException("Check-in and check-out dates are required");
        }
        if (checkIn.isBefore(LocalDate.now())) {
            throw new InvalidBookingRequestException("Check-in date cannot be in the past");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new InvalidBookingRequestException("Check-out date must be after check-in date");
        }
    }

    private void acquirePropertyBookingLock(Long propertyId) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            if (connection.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql")) {
                try (var statement = connection.prepareStatement("select pg_advisory_xact_lock(?)")) {
                    statement.setLong(1, propertyId);
                    statement.execute();
                }
            }
            return null;
        });
    }

    private BookingResponse toResponse(Booking booking) {
        return BookingResponse.builder()
                .id(booking.getId())
                .propertyId(booking.getPropertyId())
                .customerId(booking.getGuestId())
                .checkInDate(booking.getCheckInDate())
                .checkOutDate(booking.getCheckOutDate())
                .numberOfGuests(booking.getNumberOfGuests())
                .totalAmount(booking.getTotalAmount())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
