package com.hotel.repository;

import com.hotel.model.PlannedHotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Spring Data JPA repository for PlannedHotel entities.
 * Enforces secure queries scoped strictly to the authenticated guest's identity.
 */
@Repository
public interface PlannedHotelRepository extends JpaRepository<PlannedHotel, Long> {

    List<PlannedHotel> findByGuestIdOrderByPlannedAtDesc(Long guestId);

    List<PlannedHotel> findByGuestUsernameOrderByPlannedAtDesc(String username);

    Optional<PlannedHotel> findByGuestIdAndHotelId(Long guestId, Long hotelId);

    Optional<PlannedHotel> findByGuestUsernameAndHotelId(String username, Long hotelId);

    boolean existsByGuestIdAndHotelId(Long guestId, Long hotelId);

    boolean existsByGuestUsernameAndHotelId(String username, Long hotelId);

    void deleteByGuestIdAndHotelId(Long guestId, Long hotelId);

    void deleteByGuestUsernameAndHotelId(String username, Long hotelId);

    long countByGuestUsername(String username);

    @Query("SELECT p.hotel.id FROM PlannedHotel p WHERE p.guest.username = :username")
    Set<Long> findPlannedHotelIdsByUsername(@Param("username") String username);
}
