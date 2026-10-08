package com.hotel.model;

import com.hotel.model.enums.AccountStatus;
import com.hotel.model.enums.UserRole;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Base User class representing a system actor in the Luxury Hotel Management System.
 * Demonstrates Core Java OOP Concepts:
 * - Encapsulation (private fields, public getters/setters)
 * - Inheritance (Base entity for Guest, Staff, and Admin using JPA Joined Table strategy)
 * - Constructor Overloading
 */
@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.JOINED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 100)
    private String fullName;

    // Email is mandatory for Guests, but not required for Staff or Admin
    @Column(unique = true, length = 100)
    private String email;

    @Column(length = 20)
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 1. Default No-Argument Constructor (Required by JPA)
    public User() {
        this.createdAt = LocalDateTime.now();
        this.status = AccountStatus.ACTIVE;
        this.active = true;
    }

    // 2. Overloaded Constructor (Role without email - for Staff/Admin)
    public User(String username, String password, String fullName, UserRole role) {
        this();
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    // 3. Overloaded Constructor (Minimal credentials with email - for Guest)
    public User(String username, String password, String fullName, String email, UserRole role) {
        this(username, password, fullName, role);
        this.email = email;
    }

    // 4. Overloaded Full Constructor
    public User(String username, String password, String fullName, String email, String phoneNumber, UserRole role) {
        this(username, password, fullName, email, role);
        this.phoneNumber = phoneNumber;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    // Encapsulation: Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public AccountStatus getStatus() {
        return status != null ? status : (active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE);
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
        this.active = (status == AccountStatus.ACTIVE);
    }

    public boolean isActive() {
        return (status == AccountStatus.ACTIVE) || active;
    }

    public void setActive(boolean active) {
        this.active = active;
        this.status = active ? AccountStatus.ACTIVE : AccountStatus.INACTIVE;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", fullName='" + fullName + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", status=" + status +
                ", active=" + active +
                '}';
    }
}
