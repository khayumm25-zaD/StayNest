package com.staynest.bookingservice.dto;

import com.staynest.bookingservice.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingStatusRequest {
    @NotNull
    private BookingStatus status;
}
