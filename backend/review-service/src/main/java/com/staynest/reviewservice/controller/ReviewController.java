package com.staynest.reviewservice.controller;

import com.staynest.reviewservice.dto.PropertyReviewPageResponse;
import com.staynest.reviewservice.dto.ReviewRequest;
import com.staynest.reviewservice.dto.ReviewResponse;
import com.staynest.reviewservice.dto.ReviewUpdateRequest;
import com.staynest.reviewservice.exception.InvalidReviewRequestException;
import com.staynest.reviewservice.security.ReviewUserPrincipal;
import com.staynest.reviewservice.service.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.data.domain.Page<ReviewResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidReviewRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        return reviewService.getAll(PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"))));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse create(@Valid @RequestBody ReviewRequest request,
                                 @AuthenticationPrincipal ReviewUserPrincipal principal,
                                 @RequestHeader("Authorization") String authorization) {
        return reviewService.create(request, principal.getId(), authorization);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'HOST', 'ADMIN')")
    public ReviewResponse getById(@PathVariable @Positive Long id,
                                  @AuthenticationPrincipal ReviewUserPrincipal principal) {
        return reviewService.getById(id, principal.getId(), principal.getRoles().contains("ADMIN"));
    }

    @GetMapping("/property/{propertyId}")
    public PropertyReviewPageResponse getByProperty(
            @PathVariable @Positive Long propertyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "newest") String sort) {
        if (page < 0 || size < 1 || size > 50) {
            throw new InvalidReviewRequestException("Page must be non-negative and size must be between 1 and 50");
        }
        Pageable pageable;
        if ("newest".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        } else if ("rating".equalsIgnoreCase(sort)) {
            pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt")));
        } else {
            throw new InvalidReviewRequestException("Sort must be either 'newest' or 'rating'");
        }
        return reviewService.getByProperty(propertyId, pageable);
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public List<ReviewResponse> getMyReviews(@AuthenticationPrincipal ReviewUserPrincipal principal) {
        return reviewService.getMyReviews(principal.getId());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ReviewResponse update(@PathVariable @Positive Long id,
                                 @Valid @RequestBody ReviewUpdateRequest request,
                                 @AuthenticationPrincipal ReviewUserPrincipal principal) {
        return reviewService.update(id, request, principal.getId(), principal.getRoles().contains("ADMIN"));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public void delete(@PathVariable @Positive Long id,
                       @AuthenticationPrincipal ReviewUserPrincipal principal) {
        reviewService.delete(id, principal.getId(), principal.getRoles().contains("ADMIN"));
    }
}
