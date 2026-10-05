package com.staynest.reviewservice.client;

public class BookingServiceUnavailableException extends RuntimeException {
    public BookingServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
