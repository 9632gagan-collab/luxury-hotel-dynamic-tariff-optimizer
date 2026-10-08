package com.hotel.exception;

/**
 * Exception thrown when an action is attempted on a room that is not in AVAILABLE status.
 * Demonstrates custom OOP unchecked exception hierarchy.
 */
public class RoomUnavailableException extends RuntimeException {

    private final String roomNumber;

    public RoomUnavailableException(String message) {
        super(message);
        this.roomNumber = null;
    }

    public RoomUnavailableException(String roomNumber, String message) {
        super(message);
        this.roomNumber = roomNumber;
    }

    public String getRoomNumber() {
        return roomNumber;
    }
}
