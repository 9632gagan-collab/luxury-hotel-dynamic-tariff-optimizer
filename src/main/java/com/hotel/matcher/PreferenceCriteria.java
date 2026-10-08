package com.hotel.matcher;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates guest search criteria and preferences for the Smart Matching Engine.
 * Adheres to Project P68 Clean OOP principles:
 * - Immutable data holder pattern
 * - Builder pattern for flexible construction
 * - Null-safe accessors
 */
public class PreferenceCriteria {

    private final String state;
    private final String district;
    private final String experience;
    private final List<String> facilities;
    private final int guests;
    private final String roomType;
    private final String budget;
    private final Double minPrice;
    private final Double maxPrice;
    private final LocalDate checkInDate;
    private final LocalDate checkOutDate;

    public PreferenceCriteria(Builder builder) {
        this.state = builder.state;
        this.district = builder.district;
        this.experience = builder.experience;
        this.facilities = builder.facilities != null ? Collections.unmodifiableList(new ArrayList<>(builder.facilities)) : Collections.emptyList();
        this.guests = builder.guests > 0 ? builder.guests : 2;
        this.roomType = builder.roomType != null && !builder.roomType.isBlank() ? builder.roomType.trim() : "Private / Single Room";
        this.budget = builder.budget;
        this.minPrice = builder.minPrice;
        this.maxPrice = builder.maxPrice;
        this.checkInDate = builder.checkInDate;
        this.checkOutDate = builder.checkOutDate;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getState() {
        return state;
    }

    public boolean hasState() {
        return state != null && !state.isBlank() && !"All India".equalsIgnoreCase(state.trim());
    }

    public String getDistrict() {
        return district;
    }

    public boolean hasDistrict() {
        return district != null && !district.isBlank() && !district.toLowerCase().startsWith("all ");
    }

    public String getExperience() {
        return experience;
    }

    public boolean hasExperience() {
        return experience != null && !experience.isBlank() && !"All Experiences".equalsIgnoreCase(experience.trim());
    }

    public List<String> getFacilities() {
        return facilities;
    }

    public boolean hasFacilities() {
        return facilities != null && !facilities.isEmpty();
    }

    public int getGuests() {
        return guests;
    }

    public String getRoomType() {
        return roomType;
    }

    public boolean isSharedRoom() {
        return roomType != null && roomType.toLowerCase().contains("shared");
    }

    public String getBudget() {
        return budget;
    }

    public boolean hasBudget() {
        return (budget != null && !budget.isBlank() && !"all".equalsIgnoreCase(budget.trim()))
                || minPrice != null || maxPrice != null;
    }

    public Double getMinPrice() {
        return minPrice;
    }

    public Double getMaxPrice() {
        return maxPrice;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public boolean hasDates() {
        return checkInDate != null && checkOutDate != null && checkOutDate.isAfter(checkInDate);
    }

    public static class Builder {
        private String state;
        private String district;
        private String experience;
        private List<String> facilities = new ArrayList<>();
        private int guests = 2;
        private String roomType = "Private / Single Room";
        private String budget;
        private Double minPrice;
        private Double maxPrice;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;

        public Builder state(String state) {
            this.state = state;
            return this;
        }

        public Builder district(String district) {
            this.district = district;
            return this;
        }

        public Builder experience(String experience) {
            this.experience = experience;
            return this;
        }

        public Builder facilities(List<String> facilities) {
            if (facilities != null) {
                this.facilities = new ArrayList<>(facilities);
            }
            return this;
        }

        public Builder addFacility(String facility) {
            if (facility != null && !facility.isBlank()) {
                this.facilities.add(facility.trim());
            }
            return this;
        }

        public Builder guests(Integer guests) {
            if (guests != null && guests > 0) {
                this.guests = guests;
            }
            return this;
        }

        public Builder roomType(String roomType) {
            this.roomType = roomType;
            return this;
        }

        public Builder budget(String budget) {
            this.budget = budget;
            return this;
        }

        public Builder minPrice(Double minPrice) {
            this.minPrice = minPrice;
            return this;
        }

        public Builder maxPrice(Double maxPrice) {
            this.maxPrice = maxPrice;
            return this;
        }

        public Builder checkIn(String checkIn) {
            if (checkIn != null && !checkIn.isBlank()) {
                try {
                    this.checkInDate = LocalDate.parse(checkIn.trim());
                } catch (Exception ignored) {}
            }
            return this;
        }

        public Builder checkOut(String checkOut) {
            if (checkOut != null && !checkOut.isBlank()) {
                try {
                    this.checkOutDate = LocalDate.parse(checkOut.trim());
                } catch (Exception ignored) {}
            }
            return this;
        }

        public Builder dates(LocalDate checkIn, LocalDate checkOut) {
            this.checkInDate = checkIn;
            this.checkOutDate = checkOut;
            return this;
        }

        public PreferenceCriteria build() {
            return new PreferenceCriteria(this);
        }
    }
}
