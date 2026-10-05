package com.staynest.notificationservice.controller;

import com.staynest.notificationservice.dto.NotificationPageResponse;
import com.staynest.notificationservice.dto.NotificationRequest;
import com.staynest.notificationservice.dto.NotificationResponse;
import com.staynest.notificationservice.security.NotificationUserPrincipal;
import com.staynest.notificationservice.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public NotificationPageResponse getAllNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.getAllNotifications(page, size, unreadOnly);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public NotificationResponse create(@Valid @RequestBody NotificationRequest request,
                                       @AuthenticationPrincipal NotificationUserPrincipal principal) {
        return notificationService.create(request, principal.getId(), principal.getRoles().contains("ADMIN"));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public NotificationPageResponse getMyNotifications(
            @AuthenticationPrincipal NotificationUserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        return notificationService.getMyNotifications(principal.getId(), page, size, unreadOnly);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public NotificationResponse getById(@PathVariable @Positive Long id,
                                        @AuthenticationPrincipal NotificationUserPrincipal principal) {
        return notificationService.getById(id, principal.getId(), principal.getRoles().contains("ADMIN"));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public NotificationResponse markRead(@PathVariable @Positive Long id,
                                         @AuthenticationPrincipal NotificationUserPrincipal principal) {
        return notificationService.markRead(id, principal.getId(), principal.getRoles().contains("ADMIN"));
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public MarkAllReadResponse markAllRead(@AuthenticationPrincipal NotificationUserPrincipal principal) {
        return new MarkAllReadResponse(notificationService.markAllRead(principal.getId()));
    }

    public record MarkAllReadResponse(long updatedCount) {
    }
}
