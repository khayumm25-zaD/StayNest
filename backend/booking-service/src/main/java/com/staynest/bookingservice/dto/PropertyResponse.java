package com.staynest.bookingservice.dto;

import java.math.BigDecimal;

public record PropertyResponse(Long id, Long hostId, BigDecimal pricePerNight, String status, Integer maxGuests) {
}
