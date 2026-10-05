package com.staynest.notificationservice.service;

import com.staynest.notificationservice.dto.NotificationPageResponse;
import com.staynest.notificationservice.dto.NotificationRequest;
import com.staynest.notificationservice.dto.NotificationResponse;
import com.staynest.notificationservice.entity.Notification;
import com.staynest.notificationservice.exception.InvalidNotificationRequestException;
import com.staynest.notificationservice.exception.NotificationNotFoundException;
import com.staynest.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public NotificationResponse create(NotificationRequest request, Long authenticatedUserId, boolean admin) {
        if (request.getUserId() != null && !request.getUserId().equals(authenticatedUserId) && !admin) {
            throw new AccessDeniedException("Only ADMIN can create a notification for another user");
        }
        Long recipientId = admin && request.getUserId() != null ? request.getUserId() : authenticatedUserId;
        Notification notification = Notification.builder()
                .userId(recipientId)
                .type(request.getType())
                .message(request.getMessage().trim())
                .build();
        return toResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getMyNotifications(Long userId, int page, int size, boolean unreadOnly) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<NotificationResponse> notifications = (unreadOnly
                ? notificationRepository.findByUserIdAndReadAtIsNull(userId, pageable)
                : notificationRepository.findByUserId(userId, pageable)).map(this::toResponse);
        return NotificationPageResponse.from(notifications, notificationRepository.countByUserIdAndReadAtIsNull(userId));
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationPageResponse getAllNotifications(int page, int size, boolean unreadOnly) {
        validatePage(page, size);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<NotificationResponse> notifications = (unreadOnly
                ? notificationRepository.findByReadAtIsNull(pageable)
                : notificationRepository.findAll(pageable)).map(this::toResponse);
        return NotificationPageResponse.from(notifications, notificationRepository.countByReadAtIsNull());
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(Long id, Long authenticatedUserId, boolean admin) {
        return toResponse(findAccessible(id, authenticatedUserId, admin));
    }

    @Override
    @Transactional
    public NotificationResponse markRead(Long id, Long authenticatedUserId, boolean admin) {
        Notification notification = findAccessible(id, authenticatedUserId, admin);
        if (notification.getReadAt() == null) {
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }
        return toResponse(notification);
    }

    @Override
    @Transactional
    public long markAllRead(Long authenticatedUserId) {
        return notificationRepository.markAllRead(authenticatedUserId, Instant.now());
    }

    private Notification findAccessible(Long id, Long authenticatedUserId, boolean admin) {
        if (admin) {
            return notificationRepository.findById(id).orElseThrow(() -> new NotificationNotFoundException(id));
        }
        return notificationRepository.findByIdAndUserId(id, authenticatedUserId)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidNotificationRequestException("Page must be non-negative and size must be between 1 and 100");
        }
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getUserId(), notification.getType(),
                notification.getMessage(), notification.getCreatedAt(), notification.getReadAt(),
                notification.getReadAt() == null);
    }
}
