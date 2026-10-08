package com.hotel.model;

import jakarta.persistence.*;
import java.util.Objects;

/**
 * State entity representing an Indian State or Union Territory.
 * Part of hierarchical location browsing: INDIA -> STATE -> DISTRICT -> CITY -> HOTELS.
 */
@Entity
@Table(name = "states")
public class State {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 10)
    private String code; // State code (e.g., KA, MH, DL, RJ)

    @Column(nullable = false)
    private boolean unionTerritory = false;

    @Column(length = 100)
    private String capital;

    public State() {
    }

    public State(String name, String code) {
        this.name = name;
        this.code = code;
        this.unionTerritory = false;
    }

    public State(String name, String code, boolean unionTerritory, String capital) {
        this.name = name;
        this.code = code;
        this.unionTerritory = unionTerritory;
        this.capital = capital;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isUnionTerritory() {
        return unionTerritory;
    }

    public void setUnionTerritory(boolean unionTerritory) {
        this.unionTerritory = unionTerritory;
    }

    public String getCapital() {
        return capital;
    }

    public void setCapital(String capital) {
        this.capital = capital;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof State state)) return false;
        return Objects.equals(name, state.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return name + " (" + code + ")";
    }
}
