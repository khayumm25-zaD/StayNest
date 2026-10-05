package com.staynest.bookingservice.client;

public class PropertyServiceUnavailableException extends RuntimeException {
    public PropertyServiceUnavailableException(String message) {
        super(message);
    }

    public PropertyServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
