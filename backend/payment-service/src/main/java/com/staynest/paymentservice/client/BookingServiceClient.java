package com.staynest.paymentservice.client;

import com.staynest.paymentservice.dto.BookingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Component
public class BookingServiceClient {

    private final RestClient restClient;
    private final String paymentUpdateKey;

    public BookingServiceClient(RestClient.Builder builder,
                                @Value("${booking-service.url:http://BOOKING-SERVICE}") String baseUrl,
                                @Value("${booking-service.payment-update-key:}") String paymentUpdateKey) {
        if (paymentUpdateKey == null || paymentUpdateKey.isBlank()) {
            throw new IllegalStateException("PAYMENT_SERVICE_API_KEY must be configured");
        }
        this.restClient = builder.baseUrl(baseUrl).build();
        this.paymentUpdateKey = paymentUpdateKey;
    }

    public BookingResponse getBooking(Long bookingId, String authorization) {
        try {
            BookingResponse booking = restClient.get()
                    .uri("/api/bookings/{id}", bookingId)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(BookingResponse.class);
            if (booking == null || booking.totalAmount() == null || booking.customerId() == null) {
                throw new BookingServiceUnavailableException("Booking Service returned incomplete booking data", null);
            }
            return booking;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new com.staynest.paymentservice.exception.BookingNotFoundException(bookingId);
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new org.springframework.security.access.AccessDeniedException("Access to this booking is denied", ex);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new com.staynest.paymentservice.exception.BookingNotFoundException(bookingId);
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new org.springframework.security.access.AccessDeniedException("Access to this booking is denied", ex);
            }
            throw new BookingServiceUnavailableException("Booking Service rejected the request", ex);
        } catch (RestClientException ex) {
            throw new BookingServiceUnavailableException("Booking Service is unavailable", ex);
        }
    }

    public void updatePaymentStatus(Long bookingId, String paymentStatus, String authorization) {
        try {
            restClient.put()
                    .uri("/api/bookings/{id}/payment-status", bookingId)
                    .header("Authorization", authorization)
                    .header("X-Payment-Service-Key", paymentUpdateKey)
                    .body(Map.of("paymentStatus", paymentStatus))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Forbidden ex) {
            throw new org.springframework.security.access.AccessDeniedException("Access to this booking is denied", ex);
        } catch (HttpClientErrorException ex) {
            throw new BookingServiceUnavailableException("Booking Service rejected the payment status update", ex);
        } catch (RestClientException ex) {
            throw new BookingServiceUnavailableException("Booking Service is unavailable", ex);
        }
    }
}
