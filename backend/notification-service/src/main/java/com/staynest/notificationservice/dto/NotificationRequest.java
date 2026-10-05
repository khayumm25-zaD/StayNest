package com.staynest.notificationservice.dto;

import com.staynest.notificationservice.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NotificationRequest {

    @NotNull
    private NotificationType type;

    @NotBlank
    @Size(max = 1000)
    private String message;

    @Positive
    private Long userId;
}
