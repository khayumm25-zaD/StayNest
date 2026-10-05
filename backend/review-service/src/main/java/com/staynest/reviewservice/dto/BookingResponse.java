package com.staynest.reviewservice.dto;

public record BookingResponse(Long id, Long propertyId, Long customerId, String status) {
}
