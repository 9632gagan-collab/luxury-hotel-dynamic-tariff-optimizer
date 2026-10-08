package com.hotel.model;

import com.hotel.model.enums.RoomCategory;
import jakarta.persistence.*;

/**
 * GuestPreference entity capturing guest personal preferences for luxury accommodation.
 * Foundation for the Guest Preference Matcher algorithm in Stage 2.
 */
@Entity
@Table(name = "guest_preferences")
public class GuestPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private RoomCategory preferredCategory;

    @Column(nullable = false)
    private boolean smokingAllowed = false;

    @Column(nullable = false)
    private boolean highFloorPreferred = false;

    @Column(length = 30)
    private String bedType; // e.g. "KING", "TWIN"

    @Column(nullable = false)
    private boolean quietRoomRequested = true;

    @Column(length = 500)
    private String specialRequests;

    // 1. Default No-Arg Constructor
    public GuestPreference() {
    }

    // 2. Overloaded Constructor with basic preferences
    public GuestPreference(Guest guest, RoomCategory preferredCategory, String bedType) {
        this.guest = guest;
        this.preferredCategory = preferredCategory;
        this.bedType = bedType;
    }

    // 3. Overloaded Full Constructor
    public GuestPreference(Guest guest, RoomCategory preferredCategory, boolean smokingAllowed,
                           boolean highFloorPreferred, String bedType, boolean quietRoomRequested,
                           String specialRequests) {
        this.guest = guest;
        this.preferredCategory = preferredCategory;
        this.smokingAllowed = smokingAllowed;
        this.highFloorPreferred = highFloorPreferred;
        this.bedType = bedType;
        this.quietRoomRequested = quietRoomRequested;
        this.specialRequests = specialRequests;
    }

    // Encapsulation: Getters and Setters
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

    public RoomCategory getPreferredCategory() {
        return preferredCategory;
    }

    public void setPreferredCategory(RoomCategory preferredCategory) {
        this.preferredCategory = preferredCategory;
    }

    public boolean isSmokingAllowed() {
        return smokingAllowed;
    }

    public void setSmokingAllowed(boolean smokingAllowed) {
        this.smokingAllowed = smokingAllowed;
    }

    public boolean isHighFloorPreferred() {
        return highFloorPreferred;
    }

    public void setHighFloorPreferred(boolean highFloorPreferred) {
        this.highFloorPreferred = highFloorPreferred;
    }

    public String getBedType() {
        return bedType;
    }

    public void setBedType(String bedType) {
        this.bedType = bedType;
    }

    public boolean isQuietRoomRequested() {
        return quietRoomRequested;
    }

    public void setQuietRoomRequested(boolean quietRoomRequested) {
        this.quietRoomRequested = quietRoomRequested;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }
}
