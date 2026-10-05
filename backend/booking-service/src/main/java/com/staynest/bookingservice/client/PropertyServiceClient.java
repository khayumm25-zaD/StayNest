package com.staynest.bookingservice.client;

import com.staynest.bookingservice.dto.PropertyResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Arrays;
import java.util.List;

@Component
public class PropertyServiceClient {

    private final RestClient restClient;

    public PropertyServiceClient(RestClient.Builder builder,
                                 @Value("${property-service.url:http://PROPERTY-SERVICE}") String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    public PropertyResponse getProperty(Long propertyId) {
        try {
            PropertyResponse response = restClient.get()
                    .uri("/api/properties/{id}", propertyId)
                    .retrieve()
                    .body(PropertyResponse.class);
            if (response == null || response.pricePerNight() == null || response.hostId() == null) {
                throw new PropertyServiceUnavailableException("Property Service returned incomplete property data");
            }
            return response;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new com.staynest.bookingservice.exception.PropertyNotFoundException(propertyId);
        } catch (HttpClientErrorException ex) {
            if (ex.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new com.staynest.bookingservice.exception.PropertyNotFoundException(propertyId);
            }
            throw new PropertyServiceUnavailableException("Property Service rejected the request", ex);
        } catch (RestClientException ex) {
            throw new PropertyServiceUnavailableException("Property Service is unavailable", ex);
        }
    }

    public List<Long> getPropertiesForHost(Long hostId) {
        try {
            PropertyResponse[] properties = restClient.get()
                    .uri("/api/properties")
                    .retrieve()
                    .body(PropertyResponse[].class);
            if (properties == null) {
                throw new PropertyServiceUnavailableException("Property Service returned an empty response");
            }
            return Arrays.stream(properties)
                    .filter(property -> hostId.equals(property.hostId()))
                    .map(PropertyResponse::id)
                    .toList();
        } catch (HttpClientErrorException ex) {
            throw new PropertyServiceUnavailableException("Property Service rejected the request", ex);
        } catch (RestClientException ex) {
            throw new PropertyServiceUnavailableException("Property Service is unavailable", ex);
        }
    }
}
