package com.staynest.reviewservice.service;

import com.staynest.reviewservice.client.BookingServiceClient;
import com.staynest.reviewservice.dto.BookingResponse;
import com.staynest.reviewservice.dto.PropertyReviewPageResponse;
import com.staynest.reviewservice.dto.ReviewRequest;
import com.staynest.reviewservice.dto.ReviewResponse;
import com.staynest.reviewservice.dto.ReviewUpdateRequest;
import com.staynest.reviewservice.entity.Review;
import com.staynest.reviewservice.exception.BookingNotEligibleException;
import com.staynest.reviewservice.exception.DuplicateReviewException;
import com.staynest.reviewservice.exception.ReviewNotFoundException;
import com.staynest.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingServiceClient bookingServiceClient;

    @Override
    @Transactional
    public ReviewResponse create(ReviewRequest request, Long customerId, String authorization) {
        BookingResponse booking = bookingServiceClient.getBooking(request.getBookingId(), authorization);
        if (!booking.customerId().equals(customerId)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You can only review bookings made by your account");
        }
        if (!booking.propertyId().equals(request.getPropertyId())) {
            throw new BookingNotEligibleException("Booking does not belong to the requested property");
        }
        if (!"COMPLETED".equalsIgnoreCase(booking.status())) {
            throw new BookingNotEligibleException("Only completed bookings can be reviewed");
        }
        if (reviewRepository.existsByBookingIdAndPropertyIdAndCustomerId(
                booking.id(), booking.propertyId(), customerId)) {
            throw new DuplicateReviewException("A review already exists for this booking");
        }

        Review review = Review.builder()
                .propertyId(booking.propertyId())
                .bookingId(booking.id())
                .customerId(customerId)
                .rating(request.getRating())
                .title(request.getTitle().trim())
                .comment(request.getComment() == null ? null : request.getComment().trim())
                .build();
        try {
            return toResponse(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            throw new DuplicateReviewException("A review already exists for this booking");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getById(Long id, Long userId, boolean admin) {
        Review review = findReview(id);
        if (!admin && !review.getCustomerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot access this review");
        }
        return toResponse(review);
    }

    @Override
    @Transactional(readOnly = true)
    public PropertyReviewPageResponse getByProperty(Long propertyId, Pageable pageable) {
        Page<ReviewResponse> reviews = reviewRepository.findByPropertyId(propertyId, pageable).map(this::toResponse);
        Double average = reviewRepository.findAverageRatingByPropertyId(propertyId);
        long count = reviewRepository.countByPropertyId(propertyId);
        return PropertyReviewPageResponse.from(reviews, average == null ? 0.0 : average, count);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getAll(Pageable pageable) {
        return reviewRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews(Long customerId) {
        return reviewRepository.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse update(Long id, ReviewUpdateRequest request, Long userId, boolean admin) {
        Review review = findReview(id);
        requireOwnerOrAdmin(review, userId, admin);
        review.setRating(request.getRating());
        review.setTitle(request.getTitle().trim());
        review.setComment(request.getComment() == null ? null : request.getComment().trim());
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void delete(Long id, Long userId, boolean admin) {
        Review review = findReview(id);
        requireOwnerOrAdmin(review, userId, admin);
        reviewRepository.delete(review);
    }

    private Review findReview(Long id) {
        return reviewRepository.findById(id).orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private void requireOwnerOrAdmin(Review review, Long userId, boolean admin) {
        if (!admin && !review.getCustomerId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("You cannot modify this review");
        }
    }

    private ReviewResponse toResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .propertyId(review.getPropertyId())
                .bookingId(review.getBookingId())
                .customerId(review.getCustomerId())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
