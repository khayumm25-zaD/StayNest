package com.staynest.bookingservice.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class PaymentServiceKeyValidator {

    private final String configuredKey;

    public PaymentServiceKeyValidator(@Value("${payment-service.api-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    public void validate(String suppliedKey) {
        if (configuredKey == null || configuredKey.isBlank() || suppliedKey == null
                || !MessageDigest.isEqual(configuredKey.getBytes(StandardCharsets.UTF_8),
                suppliedKey.getBytes(StandardCharsets.UTF_8))) {
            throw new AccessDeniedException("A valid Payment Service credential is required");
        }
    }
}
