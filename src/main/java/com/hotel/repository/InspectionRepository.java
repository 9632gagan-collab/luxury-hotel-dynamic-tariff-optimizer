package com.hotel.repository;

import com.hotel.model.Inspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByStatusIgnoreCase(String status);
    List<Inspection> findByHotelId(Long hotelId);
}
