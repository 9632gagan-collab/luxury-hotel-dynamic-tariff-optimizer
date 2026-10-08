package com.hotel.repository;

import com.hotel.model.Booking;
import com.hotel.model.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Booking entities.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByBookingReference(String bookingReference);
    boolean existsByBookingReference(String bookingReference);
    List<Booking> findByGuestId(Long guestId);
    List<Booking> findByGuestIdAndStatusIn(Long guestId, Collection<BookingStatus> statuses);
    List<Booking> findByRoomId(Long roomId);
    List<Booking> findByRoomIdAndStatusIn(Long roomId, Collection<BookingStatus> statuses);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusIn(Collection<BookingStatus> statuses);
    long countByStatus(BookingStatus status);

    @Query("SELECT b FROM Booking b WHERE b.room.id = :roomId AND b.status IN :statuses " +
           "AND b.checkInDate < :checkOutDate AND b.checkOutDate > :checkInDate")
    List<Booking> findOverlappingBookings(@Param("roomId") Long roomId,
                                         @Param("statuses") Collection<BookingStatus> statuses,
                                         @Param("checkInDate") LocalDate checkInDate,
                                         @Param("checkOutDate") LocalDate checkOutDate);
}
