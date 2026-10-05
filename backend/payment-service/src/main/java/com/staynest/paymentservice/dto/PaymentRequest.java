package com.staynest.paymentservice.dto;

import com.staynest.paymentservice.entity.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {

    @NotNull
    @Positive
    private Long bookingId;

    @DecimalMin(value = "0.01", message = "If provided, amount must be positive")
    private BigDecimal amount;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter uppercase ISO code")
    private String currency;

    @NotNull
    private PaymentMethod paymentMethod;

    private boolean simulateFailure;
}
