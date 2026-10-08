package com.hotel.service;

import com.hotel.model.Hotel;
import com.hotel.model.Inspection;
import com.hotel.model.MaintenanceRecord;
import com.hotel.model.Staff;

import java.util.List;
import java.util.Map;

public interface AdminOperationsService {
    // Hotel / Room Operations Overview (Check Hotels & Dashboard Section B)
    Map<String, Object> getHotelOperationsOverview();

    // Detailed lists for click interaction in Check Hotels & Dashboard
    List<Map<String, Object>> getRoomsByOperationalStatus(String status);
    List<Map<String, Object>> getGuestFlowDetails(String flowType);

    // Districts Operational Overview & Hierarchical Drill-down (Districts Tab)
    Map<String, Object> getDistrictOperationsOverview(String state, String district);
    Map<String, Object> getHotelOperationalDetails(Long hotelId);

    // Maintenance Operations (Dashboard Section C)
    Map<String, Object> getMaintenanceOverview();
    List<Hotel> getMaintenanceAffectedHotels();
    List<MaintenanceRecord> getMaintenanceRecordsForHotel(Long hotelId);

    // Inspections Operations (Dashboard Section D)
    Map<String, Object> getInspectionsOverview();
    List<Inspection> getInspectionsByStatus(String status);

    // Staff Activity Operations (Dashboard Section E)
    Map<String, Object> getStaffActivityOverview();
    List<Staff> getStaffByActivityStatus(String status);

    // Guest Activity Operations (Dashboard Section A)
    Map<String, Object> getGuestActivityOverview();
    List<Map<String, Object>> getGuestActivityDetails(String category);
}
