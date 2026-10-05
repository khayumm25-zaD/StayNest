package com.staynest.paymentservice.service;

import com.staynest.paymentservice.dto.PaymentRequest;
import com.staynest.paymentservice.dto.PaymentResponse;
import com.staynest.paymentservice.security.PaymentUserPrincipal;

import java.util.List;

public interface PaymentService {
    PaymentResponse create(PaymentRequest request, PaymentUserPrincipal user, String authorization);
    PaymentResponse getById(Long id, PaymentUserPrincipal user, String authorization);
    List<PaymentResponse> getByBooking(Long bookingId, PaymentUserPrincipal user, String authorization);
    List<PaymentResponse> getMyPayments(PaymentUserPrincipal user);
    PaymentResponse refund(Long id, PaymentUserPrincipal user, String authorization);
}
