package com.staynest.paymentservice.service;

import org.springframework.stereotype.Component;

@Component
public class MockPaymentProcessor {

    public MockResult execute(boolean simulateFailure) {
        if (simulateFailure) {
            return new MockResult(false, "DEMO_FAILURE_REQUESTED");
        }
        return new MockResult(true, null);
    }

    public record MockResult(boolean successful, String failureReason) {
    }
}
