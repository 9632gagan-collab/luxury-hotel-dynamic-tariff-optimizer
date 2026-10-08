package com.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Admin creating a Staff account in Stage 2.
 */
public class StaffCreateDto {

    @NotBlank(message = "Staff Name is required")
    @Size(min = 3, max = 50, message = "Staff Name must be between 3 and 50 characters")
    private String staffName;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Please confirm the password")
    private String confirmPassword;

    public StaffCreateDto() {
    }

    public StaffCreateDto(String staffName, String password, String confirmPassword) {
        this.staffName = staffName;
        this.password = password;
        this.confirmPassword = confirmPassword;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
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
