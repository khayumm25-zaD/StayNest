package com.staynest.notificationservice.service;

import com.staynest.notificationservice.dto.NotificationPageResponse;
import com.staynest.notificationservice.dto.NotificationRequest;
import com.staynest.notificationservice.dto.NotificationResponse;

public interface NotificationService {
    NotificationResponse create(NotificationRequest request, Long authenticatedUserId, boolean admin);
    NotificationPageResponse getMyNotifications(Long userId, int page, int size, boolean unreadOnly);
    NotificationPageResponse getAllNotifications(int page, int size, boolean unreadOnly);
    NotificationResponse getById(Long id, Long authenticatedUserId, boolean admin);
    NotificationResponse markRead(Long id, Long authenticatedUserId, boolean admin);
    long markAllRead(Long authenticatedUserId);
}
