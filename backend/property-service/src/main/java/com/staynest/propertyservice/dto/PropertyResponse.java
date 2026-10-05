package com.staynest.propertyservice.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Set;

@Value
@Builder
public class PropertyResponse {
    Long id;
    Long hostId;
    String title;
    String description;
    String propertyType;
    String city;
    String state;
    String country;
    BigDecimal pricePerNight;
    Integer maxGuests;
    Integer bedrooms;
    Integer bathrooms;
    Set<String> amenities;
    String status;
}
