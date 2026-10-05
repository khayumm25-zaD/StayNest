package com.staynest.propertyservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
public class PropertyRequest {

    @NotBlank
    @Size(max = 150)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank
    @Size(max = 80)
    private String propertyType;

    @Size(max = 100)
    private String city;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String country;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal pricePerNight;

    @NotNull
    @Min(1)
    private Integer maxGuests;

    @Min(0)
    private Integer bedrooms;

    @Min(0)
    private Integer bathrooms;

    @Size(max = 40)
    private Set<@NotBlank @Size(max = 80) String> amenities;

    @Size(max = 20)
    private String status;

    private Long hostId;
}
