package com.hotel.model;

import com.hotel.model.enums.UserRole;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Guest entity representing a hotel customer.
 * Demonstrates OOP Inheritance: extends User class and adds guest-specific state and behavior.
 */
@Entity
@Table(name = "guests")
@PrimaryKeyJoinColumn(name = "user_id")
public class Guest extends User {

    @Column(length = 50)
    private String identificationNumber;

    @Column(length = 255)
    private String address;

    @Column(length = 50)
    private String nationality;

    @Column(nullable = false)
    private int loyaltyPoints = 0;

    @OneToOne(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private GuestPreference preference;

    @OneToMany(mappedBy = "guest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Booking> bookings = new ArrayList<>();

    // 1. Default No-Arg Constructor
    public Guest() {
        super();
        setRole(UserRole.GUEST);
    }

    // 2. Stage 2 Registration Constructor
    public Guest(String username, String password, String fullName, String email) {
        super(username, password, fullName, email, UserRole.GUEST);
        this.loyaltyPoints = 0;
    }

    // 3. Overloaded Constructor (Credentials & basic info)
    public Guest(String username, String password, String fullName, String email, String phoneNumber) {
        super(username, password, fullName, email, phoneNumber, UserRole.GUEST);
        this.loyaltyPoints = 0;
    }

    // 3. Overloaded Full Constructor (Including guest specific details)
    public Guest(String username, String password, String fullName, String email, String phoneNumber,
                 String identificationNumber, String address, String nationality) {
        super(username, password, fullName, email, phoneNumber, UserRole.GUEST);
        this.identificationNumber = identificationNumber;
        this.address = address;
        this.nationality = nationality;
        this.loyaltyPoints = 0;
    }

    // Encapsulation: Getters and Setters
    public String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(String identificationNumber) {
        this.identificationNumber = identificationNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getNationality() {
        return nationality;
    }

    public void setNationality(String nationality) {
        this.nationality = nationality;
    }

    public int getLoyaltyPoints() {
        return loyaltyPoints;
    }

    public void setLoyaltyPoints(int loyaltyPoints) {
        this.loyaltyPoints = loyaltyPoints;
    }

    public GuestPreference getPreference() {
        return preference;
    }

    public void setPreference(GuestPreference preference) {
        this.preference = preference;
        if (preference != null) {
            preference.setGuest(this);
        }
    }

    public List<Booking> getBookings() {
        return bookings;
    }

    public void setBookings(List<Booking> bookings) {
        this.bookings = bookings;
    }

    public void addBooking(Booking booking) {
        this.bookings.add(booking);
        booking.setGuest(this);
    }

    public void addLoyaltyPoints(int points) {
        if (points > 0) {
            this.loyaltyPoints += points;
        }
    }
}
