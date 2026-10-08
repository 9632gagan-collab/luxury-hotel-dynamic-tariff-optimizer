package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Entity representing a hotel that a Guest has added to their Planned List / Travel Planner.
 * Represents the persistent relationship between a Guest and a Hotel.
 * Demonstrates:
 * - Proper JPA relational mapping (@ManyToOne)
 * - Unique constraint to prevent duplicate planned entries for the same guest and hotel
 * - Strict encapsulation with private fields and public accessors
 * - Optional planning dates (Check-in / Check-out) for travel planning
 */
@Entity
@Table(name = "planned_hotels", uniqueConstraints = {
    @UniqueConstraint(name = "uk_guest_hotel_planned", columnNames = {"guest_id", "hotel_id"})
})
public class PlannedHotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(name = "planned_check_in")
    private LocalDate plannedCheckInDate;

    @Column(name = "planned_check_out")
    private LocalDate plannedCheckOutDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime plannedAt;

    public PlannedHotel() {
        this.plannedAt = LocalDateTime.now();
    }

    public PlannedHotel(Guest guest, Hotel hotel) {
        this.guest = guest;
        this.hotel = hotel;
        this.plannedAt = LocalDateTime.now();
    }

    public PlannedHotel(Guest guest, Hotel hotel, LocalDate plannedCheckInDate, LocalDate plannedCheckOutDate) {
        this.guest = guest;
        this.hotel = hotel;
        this.plannedCheckInDate = plannedCheckInDate;
        this.plannedCheckOutDate = plannedCheckOutDate;
        this.plannedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Guest getGuest() {
        return guest;
    }

    public void setGuest(Guest guest) {
        this.guest = guest;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public LocalDate getPlannedCheckInDate() {
        return plannedCheckInDate;
    }

    public void setPlannedCheckInDate(LocalDate plannedCheckInDate) {
        this.plannedCheckInDate = plannedCheckInDate;
    }

    public LocalDate getPlannedCheckOutDate() {
        return plannedCheckOutDate;
    }

    public void setPlannedCheckOutDate(LocalDate plannedCheckOutDate) {
        this.plannedCheckOutDate = plannedCheckOutDate;
    }

    public LocalDateTime getPlannedAt() {
        return plannedAt;
    }

    public void setPlannedAt(LocalDateTime plannedAt) {
        this.plannedAt = plannedAt;
    }

    /**
     * Checks whether planned dates are present.
     */
    public boolean hasPlannedDates() {
        return plannedCheckInDate != null;
    }

    /**
     * Formatted string for planned stay duration (e.g., "10 Oct → 12 Oct" or "Dates not planned").
     */
    public String getFormattedPlannedStay() {
        if (plannedCheckInDate == null && plannedCheckOutDate == null) {
            return "Dates not planned";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH);
        if (plannedCheckInDate != null && plannedCheckOutDate != null) {
            return plannedCheckInDate.format(formatter) + " → " + plannedCheckOutDate.format(formatter);
        } else if (plannedCheckInDate != null) {
            return "From " + plannedCheckInDate.format(formatter);
        } else {
            return "Until " + plannedCheckOutDate.format(formatter);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlannedHotel that = (PlannedHotel) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
