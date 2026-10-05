package com.staynest.propertyservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long hostId;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private String propertyType;

    private String city;
    private String state;
    private String country;

    @Column(nullable = false)
    private BigDecimal pricePerNight;

    private Integer maxGuests;
    private Integer bedrooms;
    private Integer bathrooms;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "property_amenities", joinColumns = @JoinColumn(name = "property_id"))
    @Column(name = "amenity", nullable = false)
    @Builder.Default
    private Set<String> amenities = new HashSet<>();

    @Column(nullable = false)
    private String status;
}