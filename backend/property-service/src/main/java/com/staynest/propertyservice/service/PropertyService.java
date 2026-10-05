package com.staynest.propertyservice.service;

import com.staynest.propertyservice.dto.PropertyRequest;
import com.staynest.propertyservice.dto.PropertyResponse;
import com.staynest.propertyservice.security.PropertyUserPrincipal;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

public interface PropertyService {
    PropertyResponse create(PropertyRequest request, PropertyUserPrincipal principal);
    PropertyResponse getById(Long id);
    List<PropertyResponse> getAll();
    Page<PropertyResponse> search(String location, String propertyType, BigDecimal minPrice,
                                  BigDecimal maxPrice, Integer guests, String amenity,
                                  int page, int size, String sortBy, String direction);
    PropertyResponse update(Long id, PropertyRequest request, PropertyUserPrincipal principal);
    void delete(Long id, PropertyUserPrincipal principal);
}
