package com.hotel.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * Amenity entity representing hotel luxury features (e.g., Jacuzzi, Ocean View, King Bed, Butler Service).
 * Demonstrates basic encapsulation and entity modeling.
 */
@Entity
@Table(name = "amenities")
public class Amenity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 50)
    private String iconClass; // CSS icon class (e.g. 'fa-wifi', 'fa-tv', 'fa-hot-tub')

    @Column(nullable = false)
    private double additionalCost = 0.0;

    // 1. Default No-Arg Constructor
    public Amenity() {
    }

    // 2. Overloaded Constructor (Name only)
    public Amenity(String name) {
        this.name = name;
        this.additionalCost = 0.0;
    }

    // 3. Overloaded Full Constructor
    public Amenity(String name, String description, String iconClass, double additionalCost) {
        this.name = name;
        this.description = description;
        this.iconClass = iconClass;
        this.additionalCost = additionalCost;
    }

    // Encapsulation: Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconClass() {
        return iconClass;
    }

    public void setIconClass(String iconClass) {
        this.iconClass = iconClass;
    }

    public double getAdditionalCost() {
        return additionalCost;
    }

    public void setAdditionalCost(double additionalCost) {
        this.additionalCost = additionalCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Amenity amenity)) return false;
        return Objects.equals(name, amenity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
