package com.hotel.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Data Transfer Object for guest account self-registration in Stage 2.
 * Validates username, email, password, and confirmation.
 */
public class GuestRegistrationDto {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Username may only contain letters, numbers, dots, dashes, and underscores")
    private String username;

    private String fullName;

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d|.*[^A-Za-z0-9]).+$", message = "Password must contain at least one letter and at least one number or special character")
    private String password;

    @NotBlank(message = "Please confirm your password")
    private String confirmPassword;

    public GuestRegistrationDto() {
    }

    public GuestRegistrationDto(String username, String email, String password, String confirmPassword) {
        this.username = username;
        this.fullName = username;
        this.email = email;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getUsername() {
        return username != null ? username.trim() : null;
    }

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
        if (this.fullName == null || this.fullName.isBlank()) {
            this.fullName = this.username;
        }
    }

    public String getFullName() {
        return fullName != null && !fullName.isBlank() ? fullName.trim() : getUsername();
    }

    public void setFullName(String fullName) {
        this.fullName = fullName != null ? fullName.trim() : null;
    }

    public String getEmail() {
        return email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase(Locale.ROOT) : null;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isPasswordMatching() {
        return password != null && password.equals(confirmPassword);
    }
}
