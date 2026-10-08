package com.hotel.dto;

import com.hotel.model.BookingGuestDetail;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object for initial room booking requests.
 */
public class BookingRequestDto {

    private Long roomId;
    private Long guestId;
    private Long hotelId;

    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime checkInTime;

    private LocalDate checkOutDate;

    private int numberOfGuests = 1;
    private String specialRequests;
    private String roomType;
    private List<BookingGuestDetail> guestDetails = new ArrayList<>();

    public BookingRequestDto() {
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public Long getGuestId() {
        return guestId;
    }

    public void setGuestId(Long guestId) {
        this.guestId = guestId;
    }

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalTime getCheckInTime() {
        return checkInTime;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
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

    public List<BookingGuestDetail> getGuestDetails() {
        if (guestDetails == null) {
            guestDetails = new ArrayList<>();
        }
        return guestDetails;
    }

    public void setGuestDetails(List<BookingGuestDetail> guestDetails) {
        this.guestDetails = guestDetails != null ? guestDetails : new ArrayList<>();
    }
}
