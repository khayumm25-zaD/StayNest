package com.staynest.reviewservice.exception;

public class BookingNotEligibleException extends RuntimeException {
    public BookingNotEligibleException(String message) {
        super(message);
    }
}
