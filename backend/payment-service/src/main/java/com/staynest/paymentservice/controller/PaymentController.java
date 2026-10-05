package com.staynest.paymentservice.controller;

import com.staynest.paymentservice.dto.PaymentRequest;
import com.staynest.paymentservice.dto.PaymentResponse;
import com.staynest.paymentservice.security.PaymentUserPrincipal;
import com.staynest.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request,
                                                  Authentication authentication,
                                                  @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paymentService.create(request, principal(authentication), authorization));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponse> getById(@PathVariable Long id,
                                                   Authentication authentication,
                                                   @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(paymentService.getById(id, principal(authentication), authorization));
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponse>> getByBooking(@PathVariable Long bookingId,
                                                              Authentication authentication,
                                                              @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(paymentService.getByBooking(bookingId, principal(authentication), authorization));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        return ResponseEntity.ok(paymentService.getMyPayments(principal(authentication)));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentResponse> refund(@PathVariable Long id,
                                                  Authentication authentication,
                                                  @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(paymentService.refund(id, principal(authentication), authorization));
    }

    private PaymentUserPrincipal principal(Authentication authentication) {
        return (PaymentUserPrincipal) authentication.getPrincipal();
    }
}
