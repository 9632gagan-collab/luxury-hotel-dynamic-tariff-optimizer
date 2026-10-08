package com.hotel.repository;

import com.hotel.model.GuestPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository interface for GuestPreference entities.
 */
@Repository
public interface GuestPreferenceRepository extends JpaRepository<GuestPreference, Long> {
    Optional<GuestPreference> findByGuestId(Long guestId);
}
