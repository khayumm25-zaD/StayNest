package com.staynest.reviewservice.client;

import com.staynest.reviewservice.dto.BookingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BookingServiceClient {

    private final RestClient restClient;

    public BookingServiceClient(RestClient.Builder builder,
                                @Value("${booking-service.url:http://BOOKING-SERVICE}") String baseUrl) {
        restClient = builder.baseUrl(baseUrl).build();
    }

    public BookingResponse getBooking(Long bookingId, String authorization) {
        try {
            BookingResponse booking = restClient.get()
                    .uri("/api/bookings/{id}", bookingId)
                    .header("Authorization", authorization)
                    .retrieve()
                    .body(BookingResponse.class);
            if (booking == null || booking.id() == null || booking.propertyId() == null
                    || booking.customerId() == null || booking.status() == null) {
                throw new BookingServiceUnavailableException("Booking Service returned incomplete booking data", null);
            }
            return booking;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new com.staynest.reviewservice.exception.BookingNotFoundException(bookingId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new com.staynest.reviewservice.exception.BookingNotFoundException(bookingId);
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Access to this booking is denied", ex);
            }
            throw new BookingServiceUnavailableException("Booking Service rejected the request", ex);
        } catch (RestClientException ex) {
            throw new BookingServiceUnavailableException("Booking Service is unavailable", ex);
        }
    }
}
