package com.staynest.bookingservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.bookingservice.client.PropertyServiceClient;
import com.staynest.bookingservice.dto.BookingRequest;
import com.staynest.bookingservice.dto.PropertyResponse;
import com.staynest.bookingservice.entity.Booking;
import com.staynest.bookingservice.entity.BookingStatus;
import com.staynest.bookingservice.entity.PaymentStatus;
import com.staynest.bookingservice.repository.BookingRepository;
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
import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "payment-service.api-key=test-payment-service-key")
@AutoConfigureMockMvc
class BookingControllerIntegrationTest {

    private static final String JWT_SECRET = "test-secret-key-1234567890abcdefghijklmnopqrstuvwxyz";
    private static final String PAYMENT_SERVICE_KEY = "test-payment-service-key";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyServiceClient propertyServiceClient;

    @BeforeEach
    void setUp() {
        bookingRepository.deleteAll();
        when(propertyServiceClient.getProperty(7L))
                .thenReturn(new PropertyResponse(7L, 200L, new BigDecimal("125.50"), "ACTIVE", 4));
        when(propertyServiceClient.getProperty(8L))
                .thenReturn(new PropertyResponse(8L, 201L, new BigDecimal("80.00"), "ACTIVE", 2));
        when(propertyServiceClient.getPropertiesForHost(200L)).thenReturn(List.of(7L));
        when(propertyServiceClient.getPropertiesForHost(201L)).thenReturn(List.of(8L));
    }

    @Test
    void customerCanCreateBookingAndServerCalculatesPrice() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(5), 2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId").value(7))
                .andExpect(jsonPath("$.customerId").value(10))
                .andExpect(jsonPath("$.totalAmount").value(376.5))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.paymentStatus").value("UNPAID"));
    }

    @Test
    void publicAvailabilityCheckReturnsNightCountAndQuote() throws Exception {
        mockMvc.perform(get("/api/bookings/availability")
                        .param("propertyId", "7")
                        .param("checkInDate", LocalDate.now().plusDays(5).toString())
                        .param("checkOutDate", LocalDate.now().plusDays(8).toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true))
                .andExpect(jsonPath("$.numberOfNights").value(3))
                .andExpect(jsonPath("$.pricePerNight").value(125.5))
                .andExpect(jsonPath("$.estimatedTotal").value(376.5));
    }

    @Test
    void publicAvailabilityCheckRejectsOverlappingDatesAndInvalidRanges() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(10);
        bookingRepository.saveAndFlush(Booking.builder()
                .propertyId(7L)
                .guestId(20L)
                .checkInDate(checkIn)
                .checkOutDate(checkIn.plusDays(2))
                .numberOfGuests(2)
                .totalAmount(new BigDecimal("251.00"))
                .status(BookingStatus.CONFIRMED)
                .paymentStatus(PaymentStatus.UNPAID)
                .build());

        mockMvc.perform(get("/api/bookings/availability")
                        .param("propertyId", "7")
                        .param("checkInDate", checkIn.plusDays(1).toString())
                        .param("checkOutDate", checkIn.plusDays(3).toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
        mockMvc.perform(get("/api/bookings/availability")
                        .param("propertyId", "7")
                        .param("checkInDate", checkIn.toString())
                        .param("checkOutDate", checkIn.toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidDateRangeIsRejected() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(2), 1)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, LocalDate.now().minusDays(1), LocalDate.now().plusDays(1), 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void overlappingBookingIsRejectedButCancelledBookingDoesNotBlockAvailability() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(5);
        LocalDate checkOut = checkIn.plusDays(3);
        Booking existing = booking(7L, 11L, checkIn, checkOut, BookingStatus.CONFIRMED);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, checkIn.plusDays(1), checkOut.plusDays(1), 1)))
                .andExpect(status().isConflict());

        existing.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(existing);

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, checkIn.plusDays(1), checkOut.plusDays(1), 1)))
                .andExpect(status().isCreated());
    }

    @Test
    void customerCanViewOwnButNotAnotherCustomersBooking() throws Exception {
        Booking booking = booking(7L, 10L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                BookingStatus.PENDING);

        mockMvc.perform(get("/api/bookings/my")
                        .header("Authorization", bearer(10L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value(10));

        mockMvc.perform(get("/api/bookings/{id}", booking.getId())
                        .header("Authorization", bearer(11L, "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void hostCanViewOwnPropertyBookingsButNotAnotherHostsBookings() throws Exception {
        Booking hostBooking = booking(7L, 10L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                BookingStatus.PENDING);

        mockMvc.perform(get("/api/bookings/host")
                        .header("Authorization", bearer(200L, "HOST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(hostBooking.getId()));

        mockMvc.perform(get("/api/bookings/property/7")
                        .header("Authorization", bearer(201L, "HOST")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanViewAnyBookingAndManageItsStatus() throws Exception {
        Booking booking = booking(7L, 10L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                BookingStatus.PENDING);

        mockMvc.perform(get("/api/bookings/{id}", booking.getId())
                        .header("Authorization", bearer(1L, "ADMIN")))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/bookings/{id}/status", booking.getId())
                        .header("Authorization", bearer(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void customerCannotUpdatePaymentStatusWithoutPaymentServiceCredential() throws Exception {
        Booking booking = booking(7L, 10L, LocalDate.now().plusDays(3), LocalDate.now().plusDays(4),
                BookingStatus.PENDING);

        mockMvc.perform(put("/api/bookings/{id}/payment-status", booking.getId())
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/bookings/{id}/payment-status", booking.getId())
                        .header("Authorization", bearer(11L, "CUSTOMER"))
                        .header("X-Payment-Service-Key", PAYMENT_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/bookings/{id}/payment-status", booking.getId())
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .header("X-Payment-Service-Key", PAYMENT_SERVICE_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentStatus\":\"PAID\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentStatus").value("PAID"));
    }

    @Test
    void customerCanCancelOwnBookingAndCancellationFreesDates() throws Exception {
        LocalDate checkIn = LocalDate.now().plusDays(8);
        LocalDate checkOut = checkIn.plusDays(2);
        Booking booking = booking(7L, 10L, checkIn, checkOut, BookingStatus.PENDING);

        mockMvc.perform(put("/api/bookings/{id}/cancel", booking.getId())
                        .header("Authorization", bearer(10L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(12L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, checkIn, checkOut, 2)))
                .andExpect(status().isCreated());

    }

    @Test
    void rejectsGuestCountAbovePropertyCapacity() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(10L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), 5)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void hostCannotCreateCustomerBooking() throws Exception {
        mockMvc.perform(post("/api/bookings")
                        .header("Authorization", bearer(200L, "HOST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(7L, LocalDate.now().plusDays(2), LocalDate.now().plusDays(3), 1)))
                .andExpect(status().isForbidden());
    }

    private Booking booking(Long propertyId, Long guestId, LocalDate checkIn, LocalDate checkOut,
                            BookingStatus status) {
        return bookingRepository.save(Booking.builder()
                .propertyId(propertyId)
                .guestId(guestId)
                .checkInDate(checkIn)
                .checkOutDate(checkOut)
                .numberOfGuests(1)
                .totalAmount(new BigDecimal("125.50"))
                .status(status)
                .paymentStatus(PaymentStatus.UNPAID)
                .build());
    }

    private String request(Long propertyId, LocalDate checkIn, LocalDate checkOut, Integer guests) throws Exception {
        BookingRequest request = new BookingRequest();
        request.setPropertyId(propertyId);
        request.setCheckInDate(checkIn);
        request.setCheckOutDate(checkOut);
        request.setNumberOfGuests(guests);
        return objectMapper.writeValueAsString(request);
    }

    private String bearer(Long userId, String role) {
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
