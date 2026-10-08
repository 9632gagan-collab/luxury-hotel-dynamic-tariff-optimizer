package com.hotel.model;

import com.hotel.model.enums.UserRole;
import jakarta.persistence.*;

/**
 * Admin entity representing luxury hotel administrators.
 * Demonstrates OOP Inheritance: extends User with administration-specific privileges.
 */
@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "user_id")
public class Admin extends User {

    @Column(nullable = false, length = 30)
    private String adminLevel; // e.g. "SUPER_ADMIN", "REVENUE_ADMIN"

    @Column(nullable = false)
    private boolean canManageTariffs = true;

    @Column(nullable = false)
    private boolean canManageUsers = true;

    // 1. Default No-Arg Constructor
    public Admin() {
        super();
        setRole(UserRole.ADMIN);
    }

    // 2. Stage 2 Constructor (Admin Name and Password)
    public Admin(String username, String password) {
        super(username, password, username, UserRole.ADMIN);
        this.adminLevel = "ADMIN";
        this.canManageTariffs = true;
        this.canManageUsers = true;
    }

    // 3. Overloaded Constructor (Basic Admin)
    public Admin(String username, String password, String fullName, String email, String phoneNumber) {
        super(username, password, fullName, email, phoneNumber, UserRole.ADMIN);
        this.adminLevel = "ADMIN";
        this.canManageTariffs = true;
        this.canManageUsers = true;
    }

    // 3. Overloaded Full Constructor
    public Admin(String username, String password, String fullName, String email, String phoneNumber,
                 String adminLevel, boolean canManageTariffs, boolean canManageUsers) {
        super(username, password, fullName, email, phoneNumber, UserRole.ADMIN);
        this.adminLevel = adminLevel;
        this.canManageTariffs = canManageTariffs;
        this.canManageUsers = canManageUsers;
    }

    // Encapsulation: Getters and Setters
    public String getAdminLevel() {
        return adminLevel;
    }

    public void setAdminLevel(String adminLevel) {
        this.adminLevel = adminLevel;
    }

    public boolean isCanManageTariffs() {
        return canManageTariffs;
    }

    public void setCanManageTariffs(boolean canManageTariffs) {
        this.canManageTariffs = canManageTariffs;
    }

    public boolean isCanManageUsers() {
        return canManageUsers;
    }

    public void setCanManageUsers(boolean canManageUsers) {
        this.canManageUsers = canManageUsers;
    }
}
