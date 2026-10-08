package com.hotel.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Offer entity representing luxury promotional packages and privileges across Royal Indian Stays.
 * Managed directly by Admin and displayed across both Admin and Guest portals.
 */
@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 80)
    private String tag;

    @Column(length = 250)
    private String subtitle;

    @Column(length = 2000)
    private String perks;

    @Column(nullable = false, length = 50)
    private String promoCode;

    @Column(nullable = false)
    private double discountPercentage = 15.0;

    @Column(length = 100)
    private String targetDistrict;

    @Column(length = 255)
    private String targetUrl;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDate validUntil;

    public Offer() {
        this.active = true;
        this.validUntil = LocalDate.now().plusMonths(6);
    }

    public Offer(String title, String tag, String subtitle, String perks,
                 String promoCode, double discountPercentage, String targetDistrict, String targetUrl) {
        this();
        this.title = title;
        this.tag = tag;
        this.subtitle = subtitle;
        this.perks = perks;
        this.promoCode = promoCode;
        this.discountPercentage = discountPercentage;
        this.targetDistrict = targetDistrict;
        this.targetUrl = targetUrl;
        this.active = true;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getPerks() {
        return perks;
    }

    public void setPerks(String perks) {
        this.perks = perks;
    }

    public String getPromoCode() {
        return promoCode;
    }

    public void setPromoCode(String promoCode) {
        this.promoCode = promoCode;
    }

    public double getDiscountPercentage() {
        return discountPercentage;
    }

    public void setDiscountPercentage(double discountPercentage) {
        this.discountPercentage = discountPercentage;
    }

    public String getTargetDistrict() {
        return targetDistrict;
    }

    public void setTargetDistrict(String targetDistrict) {
        this.targetDistrict = targetDistrict;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    /**
     * Helper to retrieve perks as an array or list of bullet points for UI rendering.
     */
    public String[] getPerkItems() {
        if (perks == null || perks.isBlank()) {
            return new String[0];
        }
        return perks.split("\\r?\\n");
    }
}
