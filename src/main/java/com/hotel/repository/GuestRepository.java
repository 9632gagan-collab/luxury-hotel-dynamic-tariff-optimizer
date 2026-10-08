package com.hotel.repository;

import com.hotel.model.Guest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Guest entities.
 */
@Repository
public interface GuestRepository extends JpaRepository<Guest, Long> {
    Optional<Guest> findByIdentificationNumber(String identificationNumber);
    Optional<Guest> findByEmail(String email);
    Optional<Guest> findByEmailIgnoreCase(String email);
    Optional<Guest> findByUsername(String username);
    Optional<Guest> findByUsernameIgnoreCase(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmail(String email);
    boolean existsByEmailIgnoreCase(String email);
}
