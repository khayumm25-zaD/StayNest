package com.staynest.bookingservice.repository;

import com.staynest.bookingservice.entity.Booking;
import com.staynest.bookingservice.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByGuestIdOrderByCreatedAtDesc(Long guestId);

    List<Booking> findByPropertyIdOrderByCheckInDateAsc(Long propertyId);

    List<Booking> findByPropertyIdInOrderByCheckInDateAsc(List<Long> propertyIds);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.propertyId = :propertyId " +
            "and b.status <> :cancelled " +
            "and b.checkInDate < :checkOut and b.checkOutDate > :checkIn")
    List<Booking> findOverlappingBookingsForUpdate(@Param("propertyId") Long propertyId,
                                                    @Param("checkIn") java.time.LocalDate checkIn,
                                                    @Param("checkOut") java.time.LocalDate checkOut,
                                                    @Param("cancelled") BookingStatus cancelled);

    boolean existsByPropertyIdAndStatusNotAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Long propertyId, BookingStatus status, java.time.LocalDate checkOut, java.time.LocalDate checkIn);
}
