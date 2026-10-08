package com.hotel.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * City entity representing cities and localities where luxury properties reside.
 */
@Entity
@Table(name = "cities")
public class City {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String districtName;

    @Column(nullable = false, length = 100)
    private String stateName;

    public City() {
    }

    public City(String name, String districtName, String stateName) {
        this.name = name;
        this.districtName = districtName;
        this.stateName = stateName;
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

    public String getDistrictName() {
        return districtName;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }

    public String getStateName() {
        return stateName;
    }

    public void setStateName(String stateName) {
        this.stateName = stateName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof City city)) return false;
        return Objects.equals(name, city.name) && Objects.equals(districtName, city.districtName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, districtName);
    }

    @Override
    public String toString() {
        return name + " (" + districtName + ", " + stateName + ")";
    }
}
