package com.staynest.bookingservice.service;

import com.staynest.bookingservice.dto.BookingRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityRequest;
import com.staynest.bookingservice.dto.BookingAvailabilityResponse;
import com.staynest.bookingservice.entity.PaymentStatus;
import com.staynest.bookingservice.dto.BookingResponse;
import com.staynest.bookingservice.entity.BookingStatus;
import com.staynest.bookingservice.security.BookingUserPrincipal;

import java.util.List;

public interface BookingService {
    BookingAvailabilityResponse checkAvailability(BookingAvailabilityRequest request);
    BookingResponse create(BookingRequest request, BookingUserPrincipal user);
    BookingResponse getById(Long id, BookingUserPrincipal user);
    List<BookingResponse> getMyBookings(BookingUserPrincipal user);
    List<BookingResponse> getHostBookings(BookingUserPrincipal user);
    List<BookingResponse> getPropertyBookings(Long propertyId, BookingUserPrincipal user);
    BookingResponse cancel(Long id, BookingUserPrincipal user);
    BookingResponse updateStatus(Long id, BookingStatus status, BookingUserPrincipal user);
    BookingResponse updatePaymentStatus(Long id, PaymentStatus paymentStatus, BookingUserPrincipal user);
}
