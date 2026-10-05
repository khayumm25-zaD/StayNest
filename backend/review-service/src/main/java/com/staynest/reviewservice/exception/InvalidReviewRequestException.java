package com.staynest.reviewservice.exception;

public class InvalidReviewRequestException extends RuntimeException {
    public InvalidReviewRequestException(String message) {
        super(message);
    }
}
