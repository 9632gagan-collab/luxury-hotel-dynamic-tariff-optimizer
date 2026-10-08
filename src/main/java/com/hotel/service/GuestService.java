package com.hotel.service;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.model.Guest;
import com.hotel.model.GuestPreference;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Guest operations and self-registration.
 * Demonstrates OOP Abstraction via Java Interfaces.
 */
public interface GuestService {
    List<Guest> getAllGuests();
    Optional<Guest> getGuestById(Long id);
    Optional<Guest> getGuestByUsername(String username);
    Optional<Guest> getGuestByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    Guest registerGuest(GuestRegistrationDto registrationDto);
    Guest saveGuest(Guest guest);
    void deleteGuest(Long id);
    GuestPreference updatePreferences(Long guestId, GuestPreference preferences);
    long getTotalGuestCount();
}
