package com.staynest.reviewservice.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;

import java.util.List;

@Value
@Builder
public class PropertyReviewPageResponse {
    List<ReviewResponse> content;
    int page;
    int size;
    long totalElements;
    int totalPages;
    double averageRating;
    long reviewCount;

    public static PropertyReviewPageResponse from(Page<ReviewResponse> reviews, double averageRating, long reviewCount) {
        return PropertyReviewPageResponse.builder()
                .content(reviews.getContent())
                .page(reviews.getNumber())
                .size(reviews.getSize())
                .totalElements(reviews.getTotalElements())
                .totalPages(reviews.getTotalPages())
                .averageRating(averageRating)
                .reviewCount(reviewCount)
                .build();
    }
}
