package com.hotel.exception;

/**
 * Exception thrown when a user (Guest, Staff, Admin) cannot be found by ID or username.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }

    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
