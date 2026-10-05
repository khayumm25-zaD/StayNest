package com.staynest.notificationservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.staynest.notificationservice.dto.NotificationRequest;
import com.staynest.notificationservice.entity.Notification;
import com.staynest.notificationservice.entity.NotificationType;
import com.staynest.notificationservice.repository.NotificationRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class NotificationControllerIntegrationTest {

    private static final String JWT_SECRET = "test-only-notification-jwt-signing-secret-32-bytes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void createsNotificationForAuthenticatedUser() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", token(12L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(NotificationType.BOOKING_CREATED, "Booking created", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(12))
                .andExpect(jsonPath("$.type").value("BOOKING_CREATED"))
                .andExpect(jsonPath("$.unread").value(true))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.readAt").doesNotExist());
    }

    @Test
    void doesNotAllowNonAdminToCreateForAnotherUser() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", token(12L, "HOST"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(NotificationType.PAYMENT_SUCCESS, "Payment complete", 99L)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateNotificationForAnotherUser() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", token(1L, "ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request(NotificationType.REVIEW_CREATED, "New review", 99L)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(99));
    }

    @Test
    void validatesNotificationTypeAndMessage() throws Exception {
        mockMvc.perform(post("/api/notifications")
                        .header("Authorization", token(12L, "CUSTOMER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"UNKNOWN\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsOnlyOwnNotificationsWithPaginationAndUnreadCount() throws Exception {
        save(12L, NotificationType.BOOKING_CREATED, null);
        save(12L, NotificationType.BOOKING_CONFIRMED, Instant.now());
        save(12L, NotificationType.PAYMENT_SUCCESS, null);
        save(13L, NotificationType.BOOKING_CANCELLED, null);

        mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", token(12L, "CUSTOMER"))
                        .param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.unreadCount").value(2));
    }

    @Test
    void filtersUnreadNotifications() throws Exception {
        save(12L, NotificationType.BOOKING_CREATED, null);
        save(12L, NotificationType.BOOKING_CONFIRMED, Instant.now());

        mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", token(12L, "CUSTOMER"))
                        .param("unreadOnly", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].unread").value(true))
                .andExpect(jsonPath("$.unreadCount").value(1));
    }

    @Test
    void notificationDetailsAreOwnerScoped() throws Exception {
        Notification notification = save(12L, NotificationType.BOOKING_CREATED, null);

        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", token(12L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(notification.getId()));
        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", token(13L, "CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanMarkNotificationReadIdempotently() throws Exception {
        Notification notification = save(12L, NotificationType.PAYMENT_SUCCESS, null);

        mockMvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", token(12L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(false))
                .andExpect(jsonPath("$.readAt").isNotEmpty());
        mockMvc.perform(put("/api/notifications/{id}/read", notification.getId())
                        .header("Authorization", token(12L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread").value(false));
    }

    @Test
    void marksAllOfTheAuthenticatedUsersNotificationsRead() throws Exception {
        Notification first = save(12L, NotificationType.BOOKING_CREATED, null);
        Notification second = save(12L, NotificationType.PAYMENT_SUCCESS, null);
        save(13L, NotificationType.REFUND_SUCCESS, null);

        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", token(12L, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedCount").value(2));
        assertNotNull(notificationRepository.findById(first.getId()).orElseThrow().getReadAt());
        assertNotNull(notificationRepository.findById(second.getId()).orElseThrow().getReadAt());
        org.junit.jupiter.api.Assertions.assertNull(notificationRepository.findAll().stream()
                .filter(notification -> notification.getUserId().equals(13L))
                .findFirst().orElseThrow().getReadAt());
    }

    @Test
    void adminCanReadAnotherUsersNotification() throws Exception {
        Notification notification = save(12L, NotificationType.REVIEW_CREATED, null);
        mockMvc.perform(get("/api/notifications/{id}", notification.getId())
                        .header("Authorization", token(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(12));
    }

    @Test
    void adminCanViewAllNotificationsButCustomerCannot() throws Exception {
        save(12L, NotificationType.BOOKING_CREATED, null);
        save(13L, NotificationType.REFUND_SUCCESS, Instant.now());

        mockMvc.perform(get("/api/notifications").header("Authorization", token(1L, "ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.unreadCount").value(1));
        mockMvc.perform(get("/api/notifications").header("Authorization", token(12L, "CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/api/notifications/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/notifications/my")
                        .header("Authorization", token(12L, "CUSTOMER"))
                        .param("size", "101"))
                .andExpect(status().isBadRequest());
    }

    private Notification save(Long userId, NotificationType type, Instant readAt) {
        return notificationRepository.save(Notification.builder()
                .userId(userId)
                .type(type)
                .message("Test " + type)
                .readAt(readAt)
                .build());
    }

    private String request(NotificationType type, String message, Long userId) throws Exception {
        NotificationRequest request = new NotificationRequest();
        request.setType(type);
        request.setMessage(message);
        request.setUserId(userId);
        return objectMapper.writeValueAsString(request);
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
