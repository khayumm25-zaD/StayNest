package com.staynest.bookingservice.dto;

import java.math.BigDecimal;

public record BookingAvailabilityResponse(
        boolean available,
        long numberOfNights,
        BigDecimal pricePerNight,
        BigDecimal estimatedTotal
) {
}
