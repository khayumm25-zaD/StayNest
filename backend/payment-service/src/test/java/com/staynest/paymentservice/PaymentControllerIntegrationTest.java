package com.staynest.paymentservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.paymentservice.client.BookingServiceClient;
import com.staynest.paymentservice.dto.BookingResponse;
import com.staynest.paymentservice.dto.PaymentRequest;
import com.staynest.paymentservice.entity.Payment;
import com.staynest.paymentservice.entity.PaymentMethod;
import com.staynest.paymentservice.entity.PaymentStatus;
import com.staynest.paymentservice.exception.BookingNotFoundException;
import com.staynest.paymentservice.repository.PaymentRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "booking-service.payment-update-key=test-payment-service-key")
@AutoConfigureMockMvc
class PaymentControllerIntegrationTest {

    private static final String JWT_SECRET = "test-secret-key-1234567890abcdefghijklmnopqrstuvwxyz";
    private static final String CUSTOMER_AUTH = bearer(10L, "CUSTOMER");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingServiceClient bookingServiceClient;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        reset(bookingServiceClient);
        when(bookingServiceClient.getBooking(anyLong(), eq(CUSTOMER_AUTH)))
                .thenAnswer(invocation -> booking(invocation.getArgument(0), 10L));
        when(bookingServiceClient.getBooking(anyLong(), eq(bearer(1L, "ADMIN"))))
                .thenAnswer(invocation -> booking(invocation.getArgument(0), 10L));
        doNothing().when(bookingServiceClient).updatePaymentStatus(anyLong(), eq("PAID"), eq(CUSTOMER_AUTH));
        doNothing().when(bookingServiceClient).updatePaymentStatus(anyLong(), eq("PAID"), eq(bearer(1L, "ADMIN")));
        doNothing().when(bookingServiceClient).updatePaymentStatus(anyLong(), eq("REFUNDED"), eq(CUSTOMER_AUTH));
        doNothing().when(bookingServiceClient).updatePaymentStatus(anyLong(), eq("REFUNDED"), eq(bearer(1L, "ADMIN")));
    }

    @Test
    void successfulMockPaymentUsesBookingAmountAndUpdatesBookingPaid() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(50L, "99999.00", false)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value(240.75))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.transactionReference").isNotEmpty())
                .andExpect(jsonPath("$.currency").value("USD"));

        verify(bookingServiceClient).updatePaymentStatus(50L, "PAID", CUSTOMER_AUTH);
    }

    @Test
    void mockFailureReturnsFailedPaymentWithoutUpdatingBookingPaid() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(50L, null, true)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failureReason").value("DEMO_FAILURE_REQUESTED"));

        org.assertj.core.api.Assertions.assertThat(paymentRepository.count()).isEqualTo(1);
    }

    @Test
    void paymentForMissingBookingReturnsNotFound() throws Exception {
        when(bookingServiceClient.getBooking(404L, CUSTOMER_AUTH))
                .thenThrow(new BookingNotFoundException(404L));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(404L, null, false)))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerCannotPayForAnotherCustomersBooking() throws Exception {
        when(bookingServiceClient.getBooking(51L, CUSTOMER_AUTH)).thenReturn(booking(51L, 11L));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(51L, null, false)))
                .andExpect(status().isForbidden());
    }

    @Test
    void successfulPaymentCannotBeDuplicated() throws Exception {
        postPayment(52L, CUSTOMER_AUTH);

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(52L, null, false)))
                .andExpect(status().isConflict());
    }

    @Test
    void paymentRetrievalAndCustomerHistoryReturnDtos() throws Exception {
        Payment payment = savePayment(53L, 10L, PaymentStatus.SUCCESS);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(53))
                .andExpect(jsonPath("$.amount").value(240.75))
                .andExpect(jsonPath("$.password").doesNotExist());

        mockMvc.perform(get("/api/payments/my")
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(10));

        mockMvc.perform(get("/api/payments/booking/53")
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(payment.getId()));
    }

    @Test
    void customerCannotReadAnotherCustomersPaymentOrHistoryByBooking() throws Exception {
        Payment payment = savePayment(54L, 11L, PaymentStatus.SUCCESS);
        when(bookingServiceClient.getBooking(54L, CUSTOMER_AUTH)).thenReturn(booking(54L, 11L));

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/payments/booking/54")
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void successfulPaymentCanBeRefundedAndUpdatesBooking() throws Exception {
        Payment payment = savePayment(55L, 10L, PaymentStatus.SUCCESS);

        mockMvc.perform(post("/api/payments/{id}/refund", payment.getId())
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REFUNDED"));

        verify(bookingServiceClient).updatePaymentStatus(55L, "REFUNDED", CUSTOMER_AUTH);
    }

    @Test
    void duplicateRefundIsPrevented() throws Exception {
        Payment payment = savePayment(56L, 10L, PaymentStatus.REFUNDED);

        mockMvc.perform(post("/api/payments/{id}/refund", payment.getId())
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCannotRefundAnotherCustomersPayment() throws Exception {
        Payment payment = savePayment(57L, 11L, PaymentStatus.SUCCESS);

        mockMvc.perform(post("/api/payments/{id}/refund", payment.getId())
                        .header("Authorization", CUSTOMER_AUTH))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessPaymentsAcrossUsers() throws Exception {
        Payment payment = savePayment(58L, 11L, PaymentStatus.SUCCESS);

        mockMvc.perform(get("/api/payments/{id}", payment.getId())
                        .header("Authorization", bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(11));

        mockMvc.perform(get("/api/payments/my")
                        .header("Authorization", bearer(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(11));
    }

    @Test
    void bookingServiceUnavailableReturnsServiceUnavailable() throws Exception {
        when(bookingServiceClient.getBooking(59L, CUSTOMER_AUTH))
                .thenThrow(new com.staynest.paymentservice.client.BookingServiceUnavailableException(
                        "Booking Service is unavailable", new IllegalStateException()));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(59L, null, false)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void invalidCurrencyIsRejected() throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", CUSTOMER_AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingId\":50,\"currency\":\"ZZZ\",\"paymentMethod\":\"MOCK\"}"))
                .andExpect(status().isBadRequest());
    }

    private void postPayment(Long bookingId, String authorization) throws Exception {
        mockMvc.perform(post("/api/payments")
                        .header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(bookingId, null, false)))
                .andExpect(status().isCreated());
    }

    private Payment savePayment(Long bookingId, Long userId, PaymentStatus status) {
        return paymentRepository.save(Payment.builder()
                .bookingId(bookingId)
                .userId(userId)
                .amount(new BigDecimal("240.75"))
                .currency("USD")
                .paymentMethod(PaymentMethod.MOCK)
                .status(status)
                .transactionReference("MOCK-" + bookingId)
                .build());
    }

    private String request(Long bookingId, String amount, boolean simulateFailure) throws Exception {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(bookingId);
        request.setAmount(amount == null ? null : new BigDecimal(amount));
        request.setCurrency("USD");
        request.setPaymentMethod(PaymentMethod.MOCK);
        request.setSimulateFailure(simulateFailure);
        return objectMapper.writeValueAsString(request);
    }

    private BookingResponse booking(Long bookingId, Long customerId) {
        return new BookingResponse(bookingId, customerId, new BigDecimal("240.75"), "UNPAID");
    }

    private static String bearer(Long userId, String role) {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        Instant now = Instant.now();
        String token = Jwts.builder()
                .subject("user" + userId + "@example.com")
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
        return "Bearer " + token;
    }
}
