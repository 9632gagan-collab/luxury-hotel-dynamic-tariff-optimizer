package com.hotel.model;

import com.hotel.model.enums.UserRole;
import jakarta.persistence.*;

/**
 * Staff entity representing hotel operations personnel (Front Desk, Housekeeping, Manager).
 * Demonstrates OOP Inheritance: extends User with staff-specific attributes.
 */
@Entity
@Table(name = "staff")
@PrimaryKeyJoinColumn(name = "user_id")
public class Staff extends User {

    @Column(unique = true, length = 30)
    private String employeeId;

    @Column(length = 50)
    private String department = "Operations";

    @Column(length = 50)
    private String designation = "Staff";

    @Column(length = 30)
    private String shiftTiming;

    @Column(length = 30)
    private String activityStatus = "Working";

    // 1. Default No-Arg Constructor
    public Staff() {
        super();
        setRole(UserRole.STAFF);
    }

    // 2. Stage 2 Constructor (Staff Name and Password from Admin Add Staff)
    public Staff(String staffName, String password) {
        super(staffName, password, staffName, UserRole.STAFF);
        this.employeeId = "STF-" + (System.currentTimeMillis() % 100000);
        this.department = "Operations";
        this.designation = "Staff";
    }

    // 3. Overloaded Constructor (Basic Staff Info)
    public Staff(String username, String password, String fullName, String email, String phoneNumber,
                 String employeeId, String department, String designation) {
        super(username, password, fullName, email, phoneNumber, UserRole.STAFF);
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
    }

    // 4. Overloaded Full Constructor (Including shift)
    public Staff(String username, String password, String fullName, String email, String phoneNumber,
                 String employeeId, String department, String designation, String shiftTiming) {
        this(username, password, fullName, email, phoneNumber, employeeId, department, designation);
        this.shiftTiming = shiftTiming;
    }

    // Encapsulation: Getters and Setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getShiftTiming() {
        return shiftTiming;
    }

    public void setShiftTiming(String shiftTiming) {
        this.shiftTiming = shiftTiming;
    }

    public String getActivityStatus() {
        return activityStatus != null ? activityStatus : "Working";
    }

    public void setActivityStatus(String activityStatus) {
        this.activityStatus = activityStatus;
    }
}
