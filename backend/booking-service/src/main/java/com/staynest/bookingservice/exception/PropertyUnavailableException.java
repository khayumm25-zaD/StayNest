package com.staynest.bookingservice.exception;

public class PropertyUnavailableException extends RuntimeException {
    public PropertyUnavailableException(String message) {
        super(message);
    }
}
