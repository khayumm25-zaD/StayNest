package com.staynest.notificationservice.dto;

import com.staynest.notificationservice.entity.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String message,
        Instant createdAt,
        Instant readAt,
        boolean unread
) {
}
