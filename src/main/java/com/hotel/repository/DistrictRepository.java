package com.hotel.repository;

import com.hotel.model.District;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface DistrictRepository extends JpaRepository<District, Long> {
    List<District> findByStateNameIgnoreCaseOrderByNameAsc(String stateName);

    Optional<District> findByNameIgnoreCase(String name);

    Optional<District> findByNameIgnoreCaseAndStateNameIgnoreCase(String name, String stateName);

    @Query("SELECT DISTINCT d.name FROM District d WHERE LOWER(d.stateName) = LOWER(:stateName) ORDER BY d.name ASC")
    List<String> findDistinctNamesByStateName(@Param("stateName") String stateName);
}
