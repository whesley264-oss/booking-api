package com.template.booking.repository;

import com.template.booking.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.status != 'CANCELLED' " +
           "AND b.startTime < :endTime AND b.endTime > :startTime")
    Optional<Booking> findConflictingBooking(
            @Param("resourceId") Long resourceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);

    @Query("SELECT b FROM Booking b WHERE b.resource.id = :resourceId " +
           "AND b.status != 'CANCELLED' " +
           "AND DATE(b.startTime) = DATE(:date) " +
           "ORDER BY b.startTime ASC")
    List<Booking> findByResourceIdAndDate(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDateTime date);

    List<Booking> findByUserIdAndStatus(String userId, Booking.BookingStatus status);
}
