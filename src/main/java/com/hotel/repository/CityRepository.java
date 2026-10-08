package com.hotel.repository;

import com.hotel.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByDistrictNameIgnoreCase(String districtName);
    List<City> findByStateNameIgnoreCase(String stateName);
}
