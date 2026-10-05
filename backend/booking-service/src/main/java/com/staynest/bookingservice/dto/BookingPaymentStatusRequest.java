package com.staynest.bookingservice.dto;

import com.staynest.bookingservice.entity.PaymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingPaymentStatusRequest {
    @NotNull
    private PaymentStatus paymentStatus;
}
