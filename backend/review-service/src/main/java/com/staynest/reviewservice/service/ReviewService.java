package com.staynest.reviewservice.service;

import com.staynest.reviewservice.dto.PropertyReviewPageResponse;
import com.staynest.reviewservice.dto.ReviewRequest;
import com.staynest.reviewservice.dto.ReviewResponse;
import com.staynest.reviewservice.dto.ReviewUpdateRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ReviewService {
    ReviewResponse create(ReviewRequest request, Long customerId, String authorization);
    ReviewResponse getById(Long id, Long userId, boolean admin);
    PropertyReviewPageResponse getByProperty(Long propertyId, Pageable pageable);
    Page<ReviewResponse> getAll(Pageable pageable);
    List<ReviewResponse> getMyReviews(Long customerId);
    ReviewResponse update(Long id, ReviewUpdateRequest request, Long userId, boolean admin);
    void delete(Long id, Long userId, boolean admin);
}
