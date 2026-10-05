package com.staynest.bookingservice.controller;

import com.staynest.bookingservice.dto.BookingRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityResponse;
import com.staynest.bookingservice.dto.BookingPaymentStatusRequest;
import com.staynest.bookingservice.dto.BookingResponse;
import com.staynest.bookingservice.dto.BookingStatusRequest;
import com.staynest.bookingservice.security.BookingUserPrincipal;
import com.staynest.bookingservice.security.PaymentServiceKeyValidator;
import com.staynest.bookingservice.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final PaymentServiceKeyValidator paymentServiceKeyValidator;

    @GetMapping("/availability")
    public ResponseEntity<BookingAvailabilityResponse> checkAvailability(
            @Valid @ModelAttribute BookingAvailabilityRequest request) {
        return ResponseEntity.ok(bookingService.checkAvailability(request));
    }

    @PostMapping
    public ResponseEntity<BookingResponse> create(@Valid @RequestBody BookingRequest request,
                                                  Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bookingService.create(request, principal(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingResponse> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(bookingService.getById(id, principal(authentication)));
    }

    @GetMapping("/my")
    public ResponseEntity<List<BookingResponse>> getMyBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getMyBookings(principal(authentication)));
    }

    @GetMapping("/host")
    public ResponseEntity<List<BookingResponse>> getHostBookings(Authentication authentication) {
        return ResponseEntity.ok(bookingService.getHostBookings(principal(authentication)));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<List<BookingResponse>> getPropertyBookings(@PathVariable Long propertyId,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(bookingService.getPropertyBookings(propertyId, principal(authentication)));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<BookingResponse> cancel(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(bookingService.cancel(id, principal(authentication)));
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<BookingResponse> updateStatus(@PathVariable Long id,
                                                         @Valid @RequestBody BookingStatusRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(bookingService.updateStatus(id, request.getStatus(), principal(authentication)));
    }

    @PutMapping("/{id}/payment-status")
    public ResponseEntity<BookingResponse> updatePaymentStatus(
            @PathVariable Long id,
            @Valid @RequestBody BookingPaymentStatusRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-Payment-Service-Key", required = false) String paymentServiceKey) {
        paymentServiceKeyValidator.validate(paymentServiceKey);
        return ResponseEntity.ok(bookingService.updatePaymentStatus(
                id, request.getPaymentStatus(), principal(authentication)));
    }

    private BookingUserPrincipal principal(Authentication authentication) {
        return (BookingUserPrincipal) authentication.getPrincipal();
    }
}
