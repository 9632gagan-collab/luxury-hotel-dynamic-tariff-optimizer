package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * MaintenanceRecord entity representing room and facility maintenance operations.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 60)
    private String roomOrArea;

    @Column(nullable = false, length = 100)
    private String issueType;

    /**
     * Operational status: SCHEDULED, IN_PROGRESS, COMPLETED
     */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private LocalDate reportedDate;

    @Column(length = 100)
    private String technicianName;

    @Column(length = 1000)
    private String description;

    public MaintenanceRecord() {
        this.reportedDate = LocalDate.now();
        this.status = "IN_PROGRESS";
    }

    public MaintenanceRecord(Hotel hotel, String roomOrArea, String issueType,
                             String status, LocalDate reportedDate, String technicianName, String description) {
        this.hotel = hotel;
        this.roomOrArea = roomOrArea;
        this.issueType = issueType;
        this.status = status;
        this.reportedDate = reportedDate;
        this.technicianName = technicianName;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hotel getHotel() {
        return hotel;
    }

    public void setHotel(Hotel hotel) {
        this.hotel = hotel;
    }

    public String getRoomOrArea() {
        return roomOrArea;
    }

    public void setRoomOrArea(String roomOrArea) {
        this.roomOrArea = roomOrArea;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getReportedDate() {
        return reportedDate;
    }

    public void setReportedDate(LocalDate reportedDate) {
        this.reportedDate = reportedDate;
    }

    public String getTechnicianName() {
        return technicianName;
    }

    public void setTechnicianName(String technicianName) {
        this.technicianName = technicianName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
