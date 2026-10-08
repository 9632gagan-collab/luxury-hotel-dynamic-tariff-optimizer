package com.hotel.dto;

import com.hotel.model.enums.RoomCategory;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

/**
 * Data Transfer Object for Room Search requests.
 * Used by guests and staff to filter rooms.
 */
public class RoomSearchDto {

    private RoomCategory category;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkInDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOutDate;

    private Integer guests = 1;

    public RoomSearchDto() {
    }

    public RoomCategory getCategory() {
        return category;
    }

    public void setCategory(RoomCategory category) {
        this.category = category;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Integer getGuests() {
        return guests;
    }

    public void setGuests(Integer guests) {
        this.guests = guests;
    }
}
