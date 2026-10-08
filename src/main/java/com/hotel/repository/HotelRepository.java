package com.hotel.repository;

import com.hotel.model.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for luxury Hotel entities across India.
 */
@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findAllByOrderByRatingDesc();

    List<Hotel> findByStateIgnoreCaseOrderByRatingDesc(String state);

    List<Hotel> findByStateIgnoreCaseAndDistrictIgnoreCaseOrderByRatingDesc(String state, String district);

    List<Hotel> findByStateIgnoreCaseAndDistrictIgnoreCase(String state, String district);

    List<Hotel> findByDistrictIgnoreCase(String district);

    Optional<Hotel> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    long countByStateIgnoreCase(String state);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT h.district FROM Hotel h WHERE LOWER(h.state) = LOWER(:state) AND h.district IS NOT NULL AND TRIM(h.district) != '' ORDER BY h.district ASC")
    List<String> findDistinctDistrictsByState(@org.springframework.data.repository.query.Param("state") String state);

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT h.district FROM Hotel h WHERE h.district IS NOT NULL AND TRIM(h.district) != '' ORDER BY h.district ASC")
    List<String> findAllDistinctDistricts();

    @org.springframework.data.jpa.repository.Query("SELECT DISTINCT h.state FROM Hotel h WHERE h.state IS NOT NULL AND TRIM(h.state) != '' ORDER BY h.state ASC")
    List<String> findAllDistinctStates();
}
