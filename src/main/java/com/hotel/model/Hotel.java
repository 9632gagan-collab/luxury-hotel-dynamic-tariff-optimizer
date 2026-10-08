package com.hotel.model;

import jakarta.persistence.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Hotel entity representing verified luxury properties located across India's states and union territories.
 * Adheres to Project P68 OOP principles:
 * - Strict encapsulation with private fields and public accessors
 * - Constructor overloading
 * - Strongly-typed attributes
 * - Source tracking and verified geo-location navigation
 */
@Entity
@Table(name = "hotels")
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 100)
    private String brand; // e.g. Taj / IHCL, The Leela, ITC Hotels, Oberoi Hotels, Marriott, Hyatt, Radisson, Welcomhotel

    @Column(length = 50)
    private String country = "India";

    @Column(nullable = false, length = 100)
    private String state = "Karnataka";

    @Column(nullable = false, length = 100)
    private String district;

    @Column(length = 100)
    private String city;

    @Column(nullable = false, length = 200)
    private String cityLocation;

    @Column(length = 400)
    private String address;

    @Column(length = 20)
    private String postalCode;

    @Column(length = 1000)
    private String shortDescription;

    @Column(length = 4000)
    private String detailedDescription;

    @Column(nullable = false, length = 100)
    private String hotelType;

    @Column(length = 50)
    private String contactNumber;

    @Column(length = 100)
    private String email;

    @Column(length = 500)
    private String officialWebsite;

    @Column(length = 500)
    private String officialSourceUrl;

    @Column(length = 50)
    private String sourceVerifiedDate;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String googleMapsUrl;

    private Double latitude;

    private Double longitude;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_amenities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "amenity")
    private List<String> selectedAmenities = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_experiences", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "experience")
    private List<String> experienceCategories = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "hotel_facilities", joinColumns = @JoinColumn(name = "hotel_id"))
    @Column(name = "facility")
    private List<String> facilityCategories = new ArrayList<>();

    @Column(length = 1000)
    private String nearbyAttractions;

    @Column(nullable = false)
    private double startingPricePerNight;

    @Column(nullable = false)
    private double rating;

    // 1. Default No-Arg Constructor (required by JPA)
    public Hotel() {
        this.country = "India";
        this.state = "Karnataka";
        this.rating = 4.8;
        this.startingPricePerNight = 12000.0;
        this.sourceVerifiedDate = "2026-10-01";
    }

    // 2. Overloaded Constructor with Core Identifiers (backward compatible)
    public Hotel(String name, String district, String cityLocation, String hotelType,
                 String contactNumber, String officialWebsite, String imageUrl) {
        this();
        this.name = name;
        this.district = district;
        this.cityLocation = cityLocation;
        this.city = cityLocation;
        this.hotelType = hotelType;
        this.contactNumber = contactNumber;
        this.officialWebsite = officialWebsite;
        this.officialSourceUrl = officialWebsite;
        this.imageUrl = imageUrl;
    }

    // 3. Overloaded Full Constructor (backward compatible with Stage 2 Karnataka data)
    public Hotel(String name, String district, String cityLocation, String address,
                 String shortDescription, String detailedDescription, String hotelType,
                 String contactNumber, String email, String officialWebsite, String imageUrl,
                 List<String> selectedAmenities, List<String> experienceCategories,
                 List<String> facilityCategories, String nearbyAttractions,
                 double startingPricePerNight, double rating) {
        this();
        this.name = name;
        this.district = district;
        this.cityLocation = cityLocation;
        this.city = cityLocation;
        this.address = address;
        this.shortDescription = shortDescription;
        this.detailedDescription = detailedDescription;
        this.hotelType = hotelType;
        this.contactNumber = contactNumber;
        this.email = email;
        this.officialWebsite = officialWebsite;
        this.officialSourceUrl = officialWebsite;
        this.imageUrl = imageUrl;
        this.selectedAmenities = selectedAmenities != null ? selectedAmenities : new ArrayList<>();
        this.experienceCategories = experienceCategories != null ? experienceCategories : new ArrayList<>();
        this.facilityCategories = facilityCategories != null ? facilityCategories : new ArrayList<>();
        this.nearbyAttractions = nearbyAttractions;
        this.startingPricePerNight = startingPricePerNight;
        this.rating = rating;
    }

    // 4. Overloaded Master Constructor with full India-wide verification attributes
    public Hotel(String name, String brand, String state, String district, String city,
                 String address, String postalCode, String shortDescription, String detailedDescription,
                 String hotelType, String contactNumber, String email, String officialWebsite,
                 String officialSourceUrl, String sourceVerifiedDate, String imageUrl,
                 Double latitude, Double longitude,
                 List<String> selectedAmenities, List<String> experienceCategories,
                 List<String> facilityCategories, String nearbyAttractions,
                 double startingPricePerNight, double rating) {
        this();
        this.name = name;
        this.brand = brand;
        this.country = "India";
        this.state = state != null ? state : "Karnataka";
        this.district = district;
        this.city = city;
        this.cityLocation = (city != null && !city.isBlank()) ? (city + ", " + district) : district;
        this.address = address;
        this.postalCode = postalCode;
        this.shortDescription = shortDescription;
        this.detailedDescription = detailedDescription;
        this.hotelType = hotelType;
        this.contactNumber = contactNumber;
        this.email = email;
        this.officialWebsite = officialWebsite;
        this.officialSourceUrl = officialSourceUrl != null ? officialSourceUrl : officialWebsite;
        this.sourceVerifiedDate = sourceVerifiedDate != null ? sourceVerifiedDate : "2026-10-01";
        this.imageUrl = imageUrl;
        this.latitude = latitude;
        this.longitude = longitude;
        this.selectedAmenities = selectedAmenities != null ? selectedAmenities : new ArrayList<>();
        this.experienceCategories = experienceCategories != null ? experienceCategories : new ArrayList<>();
        this.facilityCategories = facilityCategories != null ? facilityCategories : new ArrayList<>();
        this.nearbyAttractions = nearbyAttractions;
        this.startingPricePerNight = startingPricePerNight;
        this.rating = rating;
    }

    // 5. Overloaded Master Constructor with explicit verified Google Maps navigation URL
    public Hotel(String name, String brand, String state, String district, String city,
                 String address, String postalCode, String shortDescription, String detailedDescription,
                 String hotelType, String contactNumber, String email, String officialWebsite,
                 String officialSourceUrl, String sourceVerifiedDate, String imageUrl,
                 Double latitude, Double longitude, String googleMapsUrl,
                 List<String> selectedAmenities, List<String> experienceCategories,
                 List<String> facilityCategories, String nearbyAttractions,
                 double startingPricePerNight, double rating) {
        this(name, brand, state, district, city, address, postalCode, shortDescription, detailedDescription,
             hotelType, contactNumber, email, officialWebsite, officialSourceUrl, sourceVerifiedDate, imageUrl,
             latitude, longitude, selectedAmenities, experienceCategories, facilityCategories, nearbyAttractions,
             startingPricePerNight, rating);
        this.googleMapsUrl = googleMapsUrl;
    }

    /**
     * Returns the verified Google Maps navigation search URL.
     * Uses explicit official link if configured, or dynamically constructs a safe query from address.
     */
    public String getGoogleMapsUrl() {
        if (googleMapsUrl != null && !googleMapsUrl.isBlank()) {
            return googleMapsUrl;
        }
        try {
            String query;
            if (address != null && !address.isBlank()) {
                query = name + ", " + address;
            } else if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
                query = latitude + "," + longitude;
            } else {
                query = name + ", " + (city != null ? city : cityLocation) + ", " + state + ", India";
            }
            return "https://www.google.com/maps/search/?api=1&query=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "https://www.google.com/maps";
        }
    }

    public void setGoogleMapsUrl(String googleMapsUrl) {
        this.googleMapsUrl = googleMapsUrl;
    }

    // Getters and Setters
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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCityLocation() {
        return cityLocation;
    }

    public void setCityLocation(String cityLocation) {
        this.cityLocation = cityLocation;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDetailedDescription() {
        return detailedDescription;
    }

    public void setDetailedDescription(String detailedDescription) {
        this.detailedDescription = detailedDescription;
    }

    public String getHotelType() {
        return hotelType;
    }

    public void setHotelType(String hotelType) {
        this.hotelType = hotelType;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getOfficialWebsite() {
        return officialWebsite;
    }

    public void setOfficialWebsite(String officialWebsite) {
        this.officialWebsite = officialWebsite;
    }

    public String getOfficialSourceUrl() {
        return officialSourceUrl;
    }

    public void setOfficialSourceUrl(String officialSourceUrl) {
        this.officialSourceUrl = officialSourceUrl;
    }

    public String getSourceVerifiedDate() {
        return sourceVerifiedDate;
    }

    public void setSourceVerifiedDate(String sourceVerifiedDate) {
        this.sourceVerifiedDate = sourceVerifiedDate;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<String> getSelectedAmenities() {
        return selectedAmenities;
    }

    public void setSelectedAmenities(List<String> selectedAmenities) {
        this.selectedAmenities = selectedAmenities;
    }

    public List<String> getExperienceCategories() {
        return experienceCategories;
    }

    public void setExperienceCategories(List<String> experienceCategories) {
        this.experienceCategories = experienceCategories;
    }

    public List<String> getFacilityCategories() {
        return facilityCategories;
    }

    public void setFacilityCategories(List<String> facilityCategories) {
        this.facilityCategories = facilityCategories;
    }

    public String getNearbyAttractions() {
        return nearbyAttractions;
    }

    public void setNearbyAttractions(String nearbyAttractions) {
        this.nearbyAttractions = nearbyAttractions;
    }

    public double getStartingPricePerNight() {
        return startingPricePerNight;
    }

    public void setStartingPricePerNight(double startingPricePerNight) {
        this.startingPricePerNight = startingPricePerNight;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getPhone() {
        if (contactNumber == null || contactNumber.isBlank()) {
            return "+91 80 2222 1234";
        }
        String p = contactNumber.trim();
        if (p.startsWith("+91") || p.startsWith("1800")) {
            return p;
        }
        if (p.startsWith("0")) {
            return "+91-" + p.substring(1);
        }
        return "+91-" + p;
    }

    public List<String> getAmenitiesList() {
        return selectedAmenities != null ? selectedAmenities : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Hotel hotel)) return false;
        return Objects.equals(name, hotel.name) &&
                Objects.equals(district, hotel.district) &&
                Objects.equals(state, hotel.state);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, district, state);
    }

    @Override
    public String toString() {
        return name + " (" + district + ", " + state + ", India)";
    }
}
