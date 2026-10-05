package com.staynest.paymentservice.dto;

import java.math.BigDecimal;

public record BookingResponse(Long id, Long customerId, BigDecimal totalAmount, String paymentStatus) {
}
