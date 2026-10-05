package com.staynest.notificationservice.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record NotificationPageResponse(
        List<NotificationResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        long unreadCount
) {
    public static NotificationPageResponse from(Page<NotificationResponse> page, long unreadCount) {
        return new NotificationPageResponse(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), unreadCount);
    }
}
