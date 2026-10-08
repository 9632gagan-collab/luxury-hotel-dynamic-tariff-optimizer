package com.hotel.service.impl;

import com.hotel.model.Guest;
import com.hotel.model.Hotel;
import com.hotel.model.PlannedHotel;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.PlannedHotelRepository;
import com.hotel.service.PlannedHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of PlannedHotelService.
 * Enforces server-side validation, duplicate prevention, and strict guest ownership scoping.
 */
@Service
@Transactional
public class PlannedHotelServiceImpl implements PlannedHotelService {

    private final PlannedHotelRepository plannedHotelRepository;
    private final GuestRepository guestRepository;
    private final HotelRepository hotelRepository;

    @Autowired
    public PlannedHotelServiceImpl(PlannedHotelRepository plannedHotelRepository,
                                  GuestRepository guestRepository,
                                  HotelRepository hotelRepository) {
        this.plannedHotelRepository = plannedHotelRepository;
        this.guestRepository = guestRepository;
        this.hotelRepository = hotelRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlannedHotel> getPlannedHotelsForGuest(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        return plannedHotelRepository.findByGuestUsernameOrderByPlannedAtDesc(username);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> getPlannedHotelIdsForGuest(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptySet();
        }
        return plannedHotelRepository.findPlannedHotelIdsByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isHotelPlannedByGuest(String username, Long hotelId) {
        if (username == null || username.isBlank() || hotelId == null) {
            return false;
        }
        return plannedHotelRepository.existsByGuestUsernameAndHotelId(username, hotelId);
    }

    @Override
    public boolean addHotelToPlannedList(String username, Long hotelId) {
        return addHotelToPlannedList(username, hotelId, null, null);
    }

    @Override
    public boolean addHotelToPlannedList(String username, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (username == null || username.isBlank() || hotelId == null) {
            return false;
        }

        // Duplicate prevention: if already present, update dates if provided
        Optional<PlannedHotel> existingOpt = plannedHotelRepository.findByGuestUsernameAndHotelId(username, hotelId);
        if (existingOpt.isPresent()) {
            if (checkInDate != null || checkOutDate != null) {
                PlannedHotel existing = existingOpt.get();
                existing.setPlannedCheckInDate(checkInDate);
                existing.setPlannedCheckOutDate(checkOutDate);
                plannedHotelRepository.save(existing);
            }
            return false; // Already in planned list, duplicate add prevented
        }

        Optional<Guest> guestOpt = guestRepository.findByUsername(username);
        if (guestOpt.isEmpty()) {
            guestOpt = guestRepository.findByUsernameIgnoreCase(username);
        }
        if (guestOpt.isEmpty()) {
            return false;
        }

        Optional<Hotel> hotelOpt = hotelRepository.findById(hotelId);
        if (hotelOpt.isEmpty()) {
            return false;
        }

        PlannedHotel plannedHotel = new PlannedHotel(guestOpt.get(), hotelOpt.get(), checkInDate, checkOutDate);
        plannedHotelRepository.save(plannedHotel);
        return true;
    }

    @Override
    public boolean updatePlannedDates(String username, Long hotelId, LocalDate checkInDate, LocalDate checkOutDate) {
        if (username == null || username.isBlank() || hotelId == null) {
            return false;
        }

        Optional<PlannedHotel> existingOpt = plannedHotelRepository.findByGuestUsernameAndHotelId(username, hotelId);
        if (existingOpt.isEmpty()) {
            return false;
        }

        PlannedHotel plannedHotel = existingOpt.get();
        plannedHotel.setPlannedCheckInDate(checkInDate);
        plannedHotel.setPlannedCheckOutDate(checkOutDate);
        plannedHotelRepository.save(plannedHotel);
        return true;
    }

    @Override
    public boolean removeHotelFromPlannedList(String username, Long hotelId) {
        if (username == null || username.isBlank() || hotelId == null) {
            return false;
        }

        if (!plannedHotelRepository.existsByGuestUsernameAndHotelId(username, hotelId)) {
            return false;
        }

        plannedHotelRepository.deleteByGuestUsernameAndHotelId(username, hotelId);
        return true;
    }

    @Override
    public boolean togglePlannedHotel(String username, Long hotelId) {
        if (isHotelPlannedByGuest(username, hotelId)) {
            removeHotelFromPlannedList(username, hotelId);
            return false; // Removed from Planned List
        } else {
            addHotelToPlannedList(username, hotelId);
            return true; // Added to Planned List
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getPlannedCountForGuest(String username) {
        if (username == null || username.isBlank()) {
            return 0;
        }
        return plannedHotelRepository.countByGuestUsername(username);
    }
}
