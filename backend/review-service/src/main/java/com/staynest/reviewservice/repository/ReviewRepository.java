package com.staynest.reviewservice.repository;

import com.staynest.reviewservice.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByBookingIdAndPropertyIdAndCustomerId(Long bookingId, Long propertyId, Long customerId);

    Page<Review> findByPropertyId(Long propertyId, Pageable pageable);

    List<Review> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    long countByPropertyId(Long propertyId);

    @Query("select avg(r.rating) from Review r where r.propertyId = :propertyId")
    Double findAverageRatingByPropertyId(@Param("propertyId") Long propertyId);
}
