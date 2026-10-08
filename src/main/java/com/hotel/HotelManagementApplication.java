package com.hotel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main Entry Point for Luxury Hotel Dynamic Tariff Optimizer & Guest Preference Matcher.
 * Project ID: P68
 * B.Tech Academic OOP Project
 */
@SpringBootApplication
@EnableScheduling
public class HotelManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelManagementApplication.class, args);
    }
}
