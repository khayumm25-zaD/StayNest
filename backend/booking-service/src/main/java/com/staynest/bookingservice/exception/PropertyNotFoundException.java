package com.staynest.bookingservice.exception;

public class PropertyNotFoundException extends RuntimeException {
    public PropertyNotFoundException(Long id) {
        super("Property not found with id: " + id);
    }
}
