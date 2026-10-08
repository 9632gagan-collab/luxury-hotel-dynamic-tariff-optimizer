package com.hotel.service.impl;

import com.hotel.dto.GuestRegistrationDto;
import com.hotel.exception.UserNotFoundException;
import com.hotel.model.Guest;
import com.hotel.model.GuestPreference;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.GuestPreferenceRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.service.GuestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Guest operations.
 * Demonstrates OOP Polymorphism by implementing GuestService.
 */
@Service
@Transactional
public class GuestServiceImpl implements GuestService {

    private final GuestRepository guestRepository;
    private final GuestPreferenceRepository preferenceRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public GuestServiceImpl(GuestRepository guestRepository,
                            GuestPreferenceRepository preferenceRepository,
                            PasswordEncoder passwordEncoder) {
        this.guestRepository = guestRepository;
        this.preferenceRepository = preferenceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Guest> getAllGuests() {
        return guestRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Guest> getGuestById(Long id) {
        return guestRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Guest> getGuestByUsername(String username) {
        return guestRepository.findByUsernameIgnoreCase(username != null ? username.trim() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Guest> getGuestByEmail(String email) {
        return guestRepository.findByEmailIgnoreCase(email != null ? email.trim() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return guestRepository.existsByUsernameIgnoreCase(username != null ? username.trim() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return guestRepository.existsByEmailIgnoreCase(email != null ? email.trim() : "");
    }

    @Override
    public Guest registerGuest(GuestRegistrationDto dto) {
        String cleanUsername = dto.getUsername() != null ? dto.getUsername().trim() : "";
        String cleanEmail = dto.getEmail() != null ? dto.getEmail().trim().toLowerCase() : "";

        if (existsByUsername(cleanUsername)) {
            throw new IllegalArgumentException("Username '" + cleanUsername + "' is already registered. Please choose another username.");
        }
        if (existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("Email address '" + cleanEmail + "' is already registered. Please use another email or sign in.");
        }
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Password and Confirm Password do not match.");
        }

        String fullName = (dto.getFullName() != null && !dto.getFullName().isBlank()) ? dto.getFullName().trim() : cleanUsername;
        String hashedPassword = passwordEncoder.encode(dto.getPassword());

        Guest guest = new Guest(cleanUsername, hashedPassword, fullName, cleanEmail);
        guest.setStatus(AccountStatus.ACTIVE);
        guest.setActive(true);

        return guestRepository.save(guest);
    }

    @Override
    public Guest saveGuest(Guest guest) {
        return guestRepository.save(guest);
    }

    @Override
    public void deleteGuest(Long id) {
        guestRepository.deleteById(id);
    }

    @Override
    public GuestPreference updatePreferences(Long guestId, GuestPreference preferences) {
        Guest guest = guestRepository.findById(guestId)
                .orElseThrow(() -> new UserNotFoundException("Guest with ID " + guestId + " not found."));
        preferences.setGuest(guest);
        return preferenceRepository.save(preferences);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalGuestCount() {
        return guestRepository.count();
    }
}
