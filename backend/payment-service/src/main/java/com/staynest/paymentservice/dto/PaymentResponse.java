package com.staynest.paymentservice.dto;

import com.staynest.paymentservice.entity.PaymentMethod;
import com.staynest.paymentservice.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentResponse {
    Long id;
    Long bookingId;
    Long customerId;
    BigDecimal amount;
    String currency;
    PaymentStatus status;
    PaymentMethod paymentMethod;
    String transactionReference;
    String failureReason;
    Instant createdAt;
    Instant updatedAt;
}
