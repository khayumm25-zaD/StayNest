package com.staynest.propertyservice.service;

import com.staynest.propertyservice.dto.PropertyRequest;
import com.staynest.propertyservice.dto.PropertyResponse;
import com.staynest.propertyservice.entity.Property;
import com.staynest.propertyservice.exception.ResourceNotFoundException;
import com.staynest.propertyservice.repository.PropertyRepository;
import com.staynest.propertyservice.security.PropertyUserPrincipal;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PropertyServiceImpl implements PropertyService {

    private static final Set<String> SORTABLE_FIELDS =
            Set.of("id", "title", "city", "state", "country", "propertyType", "pricePerNight", "maxGuests");

    private final PropertyRepository propertyRepository;

    @Override
    @Transactional
    public PropertyResponse create(PropertyRequest request, PropertyUserPrincipal principal) {
        Property property = new Property();
        applyRequest(property, request);
        property.setHostId(isAdmin(principal) && request.getHostId() != null
                ? request.getHostId()
                : principal.getId());
        property.setStatus(normalizeStatus(request.getStatus()));
        return toResponse(propertyRepository.save(property));
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        return toResponse(findProperty(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PropertyResponse> getAll() {
        return propertyRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PropertyResponse> search(String location, String propertyType, BigDecimal minPrice,
                                         BigDecimal maxPrice, Integer guests, String amenity,
                                         int page, int size, String sortBy, String direction) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("Minimum price cannot exceed maximum price");
        }
        if (!SORTABLE_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Unsupported sort field: " + sortBy);
        }

        Sort.Direction sortDirection;
        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Sort direction must be 'asc' or 'desc'");
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Specification<Property> specification = Specification.where(null);

        if (hasText(location)) {
            String pattern = "%" + location.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("city")), pattern),
                    cb.like(cb.lower(root.get("state")), pattern),
                    cb.like(cb.lower(root.get("country")), pattern)));
        }
        if (hasText(propertyType)) {
            String pattern = "%" + propertyType.trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("propertyType")), pattern));
        }
        if (minPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("pricePerNight"), minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("pricePerNight"), maxPrice));
        }
        if (guests != null) {
            specification = specification.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("maxGuests"), guests));
        }
        if (hasText(amenity)) {
            specification = specification.and((root, query, cb) -> {
                var amenities = query.subquery(Long.class);
                var amenityRoot = amenities.from(Property.class);
                Join<Property, String> collection = amenityRoot.join("amenities");
                amenities.select(amenityRoot.get("id"))
                        .where(cb.equal(amenityRoot.get("id"), root.get("id")),
                                cb.equal(cb.lower(collection), amenity.trim().toLowerCase()));
                return cb.exists(amenities);
            });
        }

        return propertyRepository.findAll(specification, pageable).map(this::toResponse);
    }

    @Override
    @Transactional
    public PropertyResponse update(Long id, PropertyRequest request, PropertyUserPrincipal principal) {
        Property property = findProperty(id);
        verifyOwnership(property, principal);
        applyRequest(property, request);
        if (isAdmin(principal) && request.getHostId() != null) {
            property.setHostId(request.getHostId());
        }
        property.setStatus(normalizeStatus(request.getStatus()));
        return toResponse(propertyRepository.save(property));
    }

    @Override
    @Transactional
    public void delete(Long id, PropertyUserPrincipal principal) {
        Property property = findProperty(id);
        verifyOwnership(property, principal);
        propertyRepository.delete(property);
    }

    private Property findProperty(Long id) {
        return propertyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Property not found with id: " + id));
    }

    private void verifyOwnership(Property property, PropertyUserPrincipal principal) {
        if (!isAdmin(principal) && !property.getHostId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have permission to manage this property");
        }
    }

    private boolean isAdmin(PropertyUserPrincipal principal) {
        return principal.getRoles().contains("ADMIN");
    }

    private String normalizeStatus(String status) {
        if (!hasText(status)) {
            return "ACTIVE";
        }
        String normalized = status.trim().toUpperCase();
        if (!Set.of("ACTIVE", "INACTIVE", "DRAFT").contains(normalized)) {
            throw new IllegalArgumentException("Status must be ACTIVE, INACTIVE, or DRAFT");
        }
        return normalized;
    }

    private void applyRequest(Property property, PropertyRequest request) {
        property.setTitle(request.getTitle().trim());
        property.setDescription(request.getDescription());
        property.setPropertyType(request.getPropertyType().trim());
        property.setCity(clean(request.getCity()));
        property.setState(clean(request.getState()));
        property.setCountry(clean(request.getCountry()));
        property.setPricePerNight(request.getPricePerNight());
        property.setMaxGuests(request.getMaxGuests());
        property.setBedrooms(request.getBedrooms());
        property.setBathrooms(request.getBathrooms());
        property.setAmenities(request.getAmenities() == null ? Set.of() : request.getAmenities().stream()
                .map(String::trim)
                .filter(PropertyServiceImpl::hasText)
                .collect(Collectors.toSet()));
    }

    private PropertyResponse toResponse(Property property) {
        return PropertyResponse.builder()
                .id(property.getId())
                .hostId(property.getHostId())
                .title(property.getTitle())
                .description(property.getDescription())
                .propertyType(property.getPropertyType())
                .city(property.getCity())
                .state(property.getState())
                .country(property.getCountry())
                .pricePerNight(property.getPricePerNight())
                .maxGuests(property.getMaxGuests())
                .bedrooms(property.getBedrooms())
                .bathrooms(property.getBathrooms())
                .amenities(Set.copyOf(property.getAmenities()))
                .status(property.getStatus())
                .build();
    }

    private String clean(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
