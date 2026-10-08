package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Inspection entity representing hotel quality, room standard, and hygiene inspections.
 */
@Entity
@Table(name = "inspections")
public class Inspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false, length = 50)
    private String roomOrArea;

    @Column(nullable = false, length = 100)
    private String inspectorStaffName;

    @Column(nullable = false)
    private LocalDate inspectionDate;

    /**
     * Operational status: COMPLETED, PENDING, DUE, INSPECTED
     */
    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 20)
    private String score; // e.g. "98%", "100%", "92%"

    @Column(length = 1000)
    private String notes;

    public Inspection() {
        this.inspectionDate = LocalDate.now();
        this.status = "INSPECTED";
        this.score = "98%";
    }

    public Inspection(Hotel hotel, String roomOrArea, String inspectorStaffName,
                      LocalDate inspectionDate, String status, String score, String notes) {
        this.hotel = hotel;
        this.roomOrArea = roomOrArea;
        this.inspectorStaffName = inspectorStaffName;
        this.inspectionDate = inspectionDate;
        this.status = status;
        this.score = score;
        this.notes = notes;
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

    public String getInspectorStaffName() {
        return inspectorStaffName;
    }

    public void setInspectorStaffName(String inspectorStaffName) {
        this.inspectorStaffName = inspectorStaffName;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
