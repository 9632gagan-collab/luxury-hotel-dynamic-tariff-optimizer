package com.hotel.model;

import com.hotel.model.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Booking entity representing a room reservation made by a guest.
 * Demonstrates:
 * - OOP Encapsulation
 * - Many-To-One JPA Relationships with Guest and Room
 * - Constructor Overloading
 */
@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String bookingReference;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "guest_id", nullable = false)
    private Guest guest;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id")
    private Hotel hotel;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_in_time")
    private LocalTime checkInTime = LocalTime.of(14, 0);

    @Column(nullable = false)
    private LocalDate checkOutDate;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime = LocalTime.of(11, 0);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "booking_guest_details", joinColumns = @JoinColumn(name = "booking_id"))
    private List<BookingGuestDetail> guestDetails = new ArrayList<>();

    @Column(nullable = false)
    private int numberOfGuests;

    @Column(nullable = false)
    private double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 500)
    private String specialRequests;

    @Column(name = "room_type", length = 100)
    private String roomType;

    @Column(nullable = false, updatable = false)
    private LocalDateTime bookingDate;

    // 1. Default No-Arg Constructor
    public Booking() {
        this.bookingDate = LocalDateTime.now();
        this.status = BookingStatus.PENDING;
    }

    // 2. Overloaded Constructor (Standard booking details)
    public Booking(String bookingReference, Guest guest, Room room, LocalDate checkInDate, LocalDate checkOutDate,
                   int numberOfGuests, double totalAmount) {
        this();
        this.bookingReference = bookingReference;
        this.guest = guest;
        this.room = room;
        this.checkInDate = checkInDate;
        this.checkOutDate = checkOutDate;
        this.numberOfGuests = numberOfGuests;
        this.totalAmount = totalAmount;
        this.status = BookingStatus.CONFIRMED;
    }

    // 3. Overloaded Constructor with Hotel
    public Booking(String bookingReference, Guest guest, Hotel hotel, Room room, LocalDate checkInDate, LocalDate checkOutDate,
                   int numberOfGuests, double totalAmount) {
        this(bookingReference, guest, room, checkInDate, checkOutDate, numberOfGuests, totalAmount);
        this.hotel = hotel;
    }

    // 4. Overloaded Full Constructor (With special requests and custom status)
    public Booking(String bookingReference, Guest guest, Room room, LocalDate checkInDate, LocalDate checkOutDate,
                   int numberOfGuests, double totalAmount, BookingStatus status, String specialRequests) {
        this(bookingReference, guest, room, checkInDate, checkOutDate, numberOfGuests, totalAmount);
        this.status = status;
        this.specialRequests = specialRequests;
    }

    // 5. Overloaded Full Constructor with Hotel
    public Booking(String bookingReference, Guest guest, Hotel hotel, Room room, LocalDate checkInDate, LocalDate checkOutDate,
                   int numberOfGuests, double totalAmount, BookingStatus status, String specialRequests) {
        this(bookingReference, guest, hotel, room, checkInDate, checkOutDate, numberOfGuests, totalAmount);
        this.status = status;
        this.specialRequests = specialRequests;
    }

    @PrePersist
    protected void onCreate() {
        if (this.bookingDate == null) {
            this.bookingDate = LocalDateTime.now();
        }
    }

    // OOP Domain Logic Method
    public long calculateTotalNights() {
        if (checkInDate != null && checkOutDate != null) {
            long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
            return nights > 0 ? nights : 1;
        }
        return 1;
    }

    // Encapsulation: Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBookingReference() {
        return bookingReference;
    }

    public void setBookingReference(String bookingReference) {
        this.bookingReference = bookingReference;
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

    public String getHotelName() {
        if (hotel != null && hotel.getName() != null && !hotel.getName().isBlank()) {
            return hotel.getName();
        }
        return "Royal Indian Stays Luxury Property";
    }

    public String getHotelLocation() {
        if (hotel != null) {
            if (hotel.getCityLocation() != null && !hotel.getCityLocation().isBlank()) {
                return hotel.getCityLocation();
            }
            if (hotel.getDistrict() != null && !hotel.getDistrict().isBlank()) {
                return hotel.getDistrict() + ", " + hotel.getState();
            }
        }
        return "Karnataka, India";
    }

    public boolean isActiveStay() {
        if (status == BookingStatus.CANCELLED || status == BookingStatus.COMPLETED || status == BookingStatus.CHECKED_OUT) {
            return false;
        }
        if (checkOutDate != null && checkOutDate.isBefore(LocalDate.now())) {
            return false;
        }
        return true;
    }

    public boolean isPastStay() {
        return !isActiveStay();
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime != null ? checkInTime : LocalTime.of(14, 0);
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public LocalDateTime getCheckInDateTime() {
        if (checkInDate == null) {
            return null;
        }
        return LocalDateTime.of(checkInDate, getCheckInTime());
    }

    public long getCheckInEpochMillis() {
        if (checkInDate == null) {
            return 0L;
        }
        return getCheckInDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public boolean isCheckInReached() {
        if (checkInDate == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(getCheckInDateTime());
    }

    public LocalTime getCheckOutTime() {
        return checkOutTime != null ? checkOutTime : LocalTime.of(11, 0);
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public LocalDateTime getCheckOutDateTime() {
        if (checkOutDate == null) {
            return null;
        }
        return LocalDateTime.of(checkOutDate, getCheckOutTime());
    }

    public long getCheckOutEpochMillis() {
        if (checkOutDate == null) {
            return 0L;
        }
        return getCheckOutDateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public boolean isCheckOutReached() {
        if (checkOutDate == null) {
            return false;
        }
        return !LocalDateTime.now().isBefore(getCheckOutDateTime());
    }

    public boolean isCurrentStay() {
        if (status == BookingStatus.CHECKED_IN) {
            return true;
        }
        return isCheckInReached() && !isCheckOutReached() && status != BookingStatus.COMPLETED && status != BookingStatus.CANCELLED;
    }

    public boolean isStayCompleted() {
        return status == BookingStatus.COMPLETED || status == BookingStatus.CHECKED_OUT || isCheckOutReached();
    }

    public String getInitialReminderLabel() {
        if (isStayCompleted()) {
            return "STAY COMPLETED";
        }
        if (isCurrentStay()) {
            return "CHECK-OUT REMINDER";
        }
        return "REMINDER";
    }

    public String getInitialReminderCountdown() {
        if (checkInDate == null) {
            return "Date not set";
        }
        long now = System.currentTimeMillis();
        if (isStayCompleted()) {
            return "Completed";
        }
        if (isCurrentStay()) {
            long target = getCheckOutEpochMillis();
            long diffSec = (target - now) / 1000;
            if (diffSec <= 0) {
                return "Completed";
            }
            long days = diffSec / 86400;
            long hours = (diffSec % 86400) / 3600;
            long minutes = (diffSec % 3600) / 60;
            String dayLabel = days == 1 ? "Day" : "Days";
            return String.format("%d %s %d Hours %d Minutes", days, dayLabel, hours, minutes);
        } else {
            long target = getCheckInEpochMillis();
            long diffSec = (target - now) / 1000;
            if (diffSec <= 0) {
                return "Check-in time reached";
            }
            long days = diffSec / 86400;
            long hours = (diffSec % 86400) / 3600;
            long minutes = (diffSec % 3600) / 60;
            String dayLabel = days == 1 ? "Day" : "Days";
            return String.format("%d %s %d Hours %d Minutes", days, dayLabel, hours, minutes);
        }
    }

    public String getInitialReminderSubtext() {
        if (isStayCompleted()) {
            return "No active countdown";
        }
        if (isCurrentStay()) {
            return "until check-out";
        }
        return "until check-in";
    }

    public List<BookingGuestDetail> getGuestDetails() {
        if (guestDetails == null) {
            guestDetails = new ArrayList<>();
        }
        return guestDetails;
    }

    public void setGuestDetails(List<BookingGuestDetail> guestDetails) {
        this.guestDetails = guestDetails != null ? guestDetails : new ArrayList<>();
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public int getNumberOfGuests() {
        return numberOfGuests;
    }

    public void setNumberOfGuests(int numberOfGuests) {
        this.numberOfGuests = numberOfGuests;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }

    public String getSpecialRequests() {
        return specialRequests;
    }

    public void setSpecialRequests(String specialRequests) {
        this.specialRequests = specialRequests;
    }

    public String getRoomType() {
        return roomType;
    }

    public void setRoomType(String roomType) {
        this.roomType = roomType;
    }

    public LocalDateTime getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(LocalDateTime bookingDate) {
        this.bookingDate = bookingDate;
    }
}
