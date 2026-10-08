package com.hotel.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * District entity representing an administrative district within a State or Union Territory.
 */
@Entity
@Table(name = "districts")
public class District {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String stateName;

    @Column(length = 100)
    private String headquarter;

    public District() {
    }

    public District(String name, String stateName) {
        this.name = name;
        this.stateName = stateName;
    }

    public District(String name, String stateName, String headquarter) {
        this.name = name;
        this.stateName = stateName;
        this.headquarter = headquarter;
    }

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

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    public String getHeadquarter() {
        return headquarter;
    }

    public void setHeadquarter(String headquarter) {
        this.headquarter = headquarter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof District district)) return false;
        return Objects.equals(name, district.name) && Objects.equals(stateName, district.stateName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, stateName);
    }

    @Override
    public String toString() {
        return name + ", " + stateName;
    }
}
