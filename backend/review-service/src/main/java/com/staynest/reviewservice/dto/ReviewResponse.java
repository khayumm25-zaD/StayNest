package com.staynest.reviewservice.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ReviewResponse {
    Long id;
    Long propertyId;
    Long bookingId;
    Long customerId;
    Integer rating;
    String title;
    String comment;
    Instant createdAt;
    Instant updatedAt;
}
