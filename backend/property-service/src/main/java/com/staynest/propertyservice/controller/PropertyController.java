package com.staynest.propertyservice.controller;

import com.staynest.propertyservice.dto.PropertyRequest;
import com.staynest.propertyservice.dto.PropertyResponse;
import com.staynest.propertyservice.security.PropertyUserPrincipal;
import com.staynest.propertyservice.service.PropertyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    @PostMapping
    public ResponseEntity<PropertyResponse> create(
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(propertyService.create(request, principal(authentication)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(propertyService.getById(id));
    }

    @GetMapping
    public ResponseEntity<List<PropertyResponse>> getAll() {
        return ResponseEntity.ok(propertyService.getAll());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<PropertyResponse>> search(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String propertyType,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true) BigDecimal minPrice,
            @RequestParam(required = false) @DecimalMin(value = "0.0", inclusive = true) BigDecimal maxPrice,
            @RequestParam(required = false) @Min(1) Integer guests,
            @RequestParam(required = false) String amenity,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "12") @Min(1) int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(propertyService.search(
                location, propertyType, minPrice, maxPrice, guests, amenity, page, size, sortBy, direction));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(propertyService.update(id, request, principal(authentication)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        propertyService.delete(id, principal(authentication));
        return ResponseEntity.noContent().build();
    }

    private PropertyUserPrincipal principal(Authentication authentication) {
        return (PropertyUserPrincipal) authentication.getPrincipal();
    }
}
