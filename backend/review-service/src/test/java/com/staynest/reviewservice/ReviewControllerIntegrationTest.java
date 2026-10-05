package com.staynest.reviewservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.reviewservice.client.BookingServiceClient;
import com.staynest.reviewservice.dto.BookingResponse;
import com.staynest.reviewservice.entity.Review;
import com.staynest.reviewservice.exception.BookingNotFoundException;
import com.staynest.reviewservice.client.BookingServiceUnavailableException;
import com.staynest.reviewservice.repository.ReviewRepository;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerIntegrationTest {

    private static final String JWT_SECRET = "test-only-review-jwt-signing-secret-32-bytes-minimum";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookingServiceClient bookingServiceClient;

    @BeforeEach
    void setUp() {
        reviewRepository.deleteAll();
        reset(bookingServiceClient);
    }

    @Test
    void createsReviewForCompletedOwnedBooking() throws Exception {
        when(bookingServiceClient.getBooking(eq(10L), anyString()))
                .thenReturn(new BookingResponse(10L, 50L, 7L, "COMPLETED"));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 5)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.propertyId").value(50))
                .andExpect(jsonPath("$.bookingId").value(10))
                .andExpect(jsonPath("$.customerId").value(7))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void rejectsInvalidRating() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 6)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(bookingServiceClient);
    }

    @Test
    void hostCannotCreateReview() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "HOST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 5)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(bookingServiceClient);
    }

    @Test
    void rejectsReviewWhenBookingDoesNotExist() throws Exception {
        when(bookingServiceClient.getBooking(eq(404L), anyString()))
                .thenThrow(new BookingNotFoundException(404L));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 404, 5)))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsAnotherCustomersBooking() throws Exception {
        when(bookingServiceClient.getBooking(eq(10L), anyString()))
                .thenReturn(new BookingResponse(10L, 50L, 8L, "COMPLETED"));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 5)))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsIncompleteBooking() throws Exception {
        when(bookingServiceClient.getBooking(eq(10L), anyString()))
                .thenReturn(new BookingResponse(10L, 50L, 7L, "CONFIRMED"));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 5)))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsDuplicateReviewForBooking() throws Exception {
        when(bookingServiceClient.getBooking(eq(10L), anyString()))
                .thenReturn(new BookingResponse(10L, 50L, 7L, "COMPLETED"));
        String authorization = token(7L, "CUSTOMER");

        mockMvc.perform(post("/api/reviews").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(reviewRequest(50, 10, 4)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reviews").header("Authorization", authorization)
                        .contentType(MediaType.APPLICATION_JSON).content(reviewRequest(50, 10, 4)))
                .andExpect(status().isConflict());
    }

    @Test
    void customerCanUpdateOwnReviewButNotAnotherUsersReview() throws Exception {
        Review review = saveReview(50L, 10L, 7L, 3);
        mockMvc.perform(put("/api/reviews/{id}", review.getId())
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"title\":\"Updated\",\"comment\":\"Better\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.customerId").value(7));

        mockMvc.perform(put("/api/reviews/{id}", review.getId())
                        .header("Authorization", token(8L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":1,\"title\":\"No\",\"comment\":\"No\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCanDeleteOwnReview() throws Exception {
        Review review = saveReview(50L, 10L, 7L, 3);
        mockMvc.perform(delete("/api/reviews/{id}", review.getId())
                        .header("Authorization", token(7L, "CUSTOMER")))
                .andExpect(status().isNoContent());
        org.junit.jupiter.api.Assertions.assertFalse(reviewRepository.existsById(review.getId()));
    }

    @Test
    void adminCanManageAnyReview() throws Exception {
        Review review = saveReview(50L, 10L, 7L, 3);
        mockMvc.perform(put("/api/reviews/{id}", review.getId())
                        .header("Authorization", token(99L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"title\":\"Admin edit\",\"comment\":\"Managed\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5));
        mockMvc.perform(delete("/api/reviews/{id}", review.getId())
                        .header("Authorization", token(99L, "ADMIN")))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanListAllReviews() throws Exception {
        saveReview(50L, 10L, 7L, 3);
        saveReview(51L, 11L, 8L, 5);
        mockMvc.perform(get("/api/reviews").header("Authorization", token(99L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get("/api/reviews").header("Authorization", token(7L, "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void propertyReviewsArePublicPaginatedAndIncludeAggregate() throws Exception {
        saveReview(50L, 10L, 7L, 5);
        saveReview(50L, 11L, 8L, 3);
        saveReview(50L, 12L, 9L, 4);

        mockMvc.perform(get("/api/reviews/property/50").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(3));
        mockMvc.perform(get("/api/reviews/property/50").param("sort", "rating"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rating").value(5));
    }

    @Test
    void mapsBookingServiceUnavailableTo503() throws Exception {
        when(bookingServiceClient.getBooking(eq(10L), anyString()))
                .thenThrow(new BookingServiceUnavailableException("Booking Service is unavailable", null));

        mockMvc.perform(post("/api/reviews")
                        .header("Authorization", token(7L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reviewRequest(50, 10, 5)))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void returnsOwnReviewHistoryAndProtectsIndividualReview() throws Exception {
        Review own = saveReview(50L, 10L, 7L, 4);
        Review someoneElses = saveReview(50L, 11L, 8L, 5);

        mockMvc.perform(get("/api/reviews/my").header("Authorization", token(7L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(own.getId()));
        mockMvc.perform(get("/api/reviews/{id}", someoneElses.getId())
                        .header("Authorization", token(7L, "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    private Review saveReview(Long propertyId, Long bookingId, Long customerId, int rating) {
        return reviewRepository.save(Review.builder()
                .propertyId(propertyId)
                .bookingId(bookingId)
                .customerId(customerId)
                .rating(rating)
                .title("Test review")
                .comment("Test comment")
                .build());
    }

    private String reviewRequest(long propertyId, long bookingId, int rating) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of(
                "propertyId", propertyId, "bookingId", bookingId,
                "rating", rating, "title", "Nice stay", "comment", "Enjoyed it"));
    }

    private String token(Long userId, String role) {
        Instant now = Instant.now();
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return "Bearer " + Jwts.builder()
                .subject("user" + userId + "@example.com")
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(3600)))
                .signWith(key)
                .compact();
    }
}
