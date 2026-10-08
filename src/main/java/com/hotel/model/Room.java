package com.hotel.model;

import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Room entity representing a luxury hotel room.
 * Demonstrates:
 * - OOP Encapsulation (private fields with public getters/setters)
 * - Constructor Overloading
 * - Entity Relationships (@ManyToMany with Amenity)
 */
@Entity
@Table(name = "rooms")
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomStatus status = RoomStatus.AVAILABLE;

    @Column(nullable = false)
    private double basePricePerNight;

    /**
     * Dynamic tariff placeholder (calculated based on occupancy / demand in Stage 2).
     */
    @Column(nullable = false)
    private double currentTariff;

    @Column(nullable = false)
    private int floorNumber;

    @Column(nullable = false)
    private int maxOccupancy;

    @Column(length = 1000)
    private String description;

    @Column(length = 255)
    private String imageUrl;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "room_amenities",
            joinColumns = @JoinColumn(name = "room_id"),
            inverseJoinColumns = @JoinColumn(name = "amenity_id")
    )
    private Set<Amenity> amenities = new HashSet<>();

    // 1. Default No-Arg Constructor
    public Room() {
        this.status = RoomStatus.AVAILABLE;
    }

    // 2. Overloaded Constructor (Basic Room Setup)
    public Room(String roomNumber, RoomCategory category, double basePricePerNight, int floorNumber, int maxOccupancy) {
        this();
        this.roomNumber = roomNumber;
        this.category = category;
        this.basePricePerNight = basePricePerNight;
        this.currentTariff = basePricePerNight;
        this.floorNumber = floorNumber;
        this.maxOccupancy = maxOccupancy;
    }

    // 3. Overloaded Full Constructor
    public Room(String roomNumber, RoomCategory category, RoomStatus status, double basePricePerNight,
                int floorNumber, int maxOccupancy, String description, String imageUrl) {
        this(roomNumber, category, basePricePerNight, floorNumber, maxOccupancy);
        this.status = status;
        this.description = description;
        this.imageUrl = imageUrl;
    }

    // Encapsulation: Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(String roomNumber) {
        this.roomNumber = roomNumber;
    }

    public RoomCategory getCategory() {
        return category;
    }

    public void setCategory(RoomCategory category) {
        this.category = category;
    }

    public RoomStatus getStatus() {
        return status;
    }

    public void setStatus(RoomStatus status) {
        this.status = status;
    }

    public double getBasePricePerNight() {
        return basePricePerNight;
    }

    public void setBasePricePerNight(double basePricePerNight) {
        this.basePricePerNight = basePricePerNight;
    }

    public double getCurrentTariff() {
        return currentTariff;
    }

    public void setCurrentTariff(double currentTariff) {
        this.currentTariff = currentTariff;
    }

    public int getFloorNumber() {
        return floorNumber;
    }

    public void setFloorNumber(int floorNumber) {
        this.floorNumber = floorNumber;
    }

    public int getMaxOccupancy() {
        return maxOccupancy;
    }

    public void setMaxOccupancy(int maxOccupancy) {
        this.maxOccupancy = maxOccupancy;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Set<Amenity> getAmenities() {
        return amenities;
    }

    public void setAmenities(Set<Amenity> amenities) {
        this.amenities = amenities;
    }

    public void addAmenity(Amenity amenity) {
        this.amenities.add(amenity);
    }

    public boolean isAvailable() {
        return this.status == RoomStatus.AVAILABLE;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Room room)) return false;
        return Objects.equals(roomNumber, room.roomNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roomNumber);
    }

    @Override
    public String toString() {
        return "Room #" + roomNumber + " [" + category + " - " + status + "]";
    }
}
