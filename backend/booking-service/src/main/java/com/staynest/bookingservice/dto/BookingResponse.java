package com.staynest.bookingservice.dto;

import com.staynest.bookingservice.entity.BookingStatus;
import com.staynest.bookingservice.entity.PaymentStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Value
@Builder
public class BookingResponse {
    Long id;
    Long propertyId;
    Long customerId;
    LocalDate checkInDate;
    LocalDate checkOutDate;
    Integer numberOfGuests;
    BigDecimal totalAmount;
    BookingStatus status;
    PaymentStatus paymentStatus;
    Instant createdAt;
    Instant updatedAt;
}
