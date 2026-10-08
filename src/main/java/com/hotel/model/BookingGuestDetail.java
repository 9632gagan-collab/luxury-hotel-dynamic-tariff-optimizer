package com.hotel.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Embeddable entity representing individual guest details within a reservation.
 * Stores guest classification (Adult / Minor), age, and gender.
 */
@Embeddable
public class BookingGuestDetail implements Serializable {

    @Column(name = "name", length = 100)
    private String name;

    @Column(name = "guest_type", length = 30)
    private String guestType = "Adult"; // "Adult" or "Minor"

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", length = 30)
    private String gender; // "Male", "Female", "Other"

    public BookingGuestDetail() {
    }

    public BookingGuestDetail(String guestType, Integer age, String gender) {
        this("", guestType, age, gender);
    }

    public BookingGuestDetail(String name, String guestType, Integer age, String gender) {
        this.name = name;
        this.guestType = guestType;
        this.age = age;
        this.gender = gender;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGuestType() {
        return guestType;
    }

    public void setGuestType(String guestType) {
        this.guestType = guestType;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BookingGuestDetail that = (BookingGuestDetail) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(guestType, that.guestType) &&
                Objects.equals(age, that.age) &&
                Objects.equals(gender, that.gender);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, guestType, age, gender);
    }

    @Override
    public String toString() {
        return "BookingGuestDetail{" +
                "name='" + name + '\'' +
                ", guestType='" + guestType + '\'' +
                ", age=" + age +
                ", gender='" + gender + '\'' +
                '}';
    }
}
