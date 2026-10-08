package com.hotel.repository;

import com.hotel.model.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {
    List<MaintenanceRecord> findByStatusIgnoreCase(String status);
    List<MaintenanceRecord> findByHotelId(Long hotelId);
}
