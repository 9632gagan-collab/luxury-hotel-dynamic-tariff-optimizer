package com.hotel.service;

import com.hotel.dto.StaffCreateDto;
import com.hotel.model.Staff;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for Staff management and Admin-controlled Staff provisioning.
 */
public interface StaffService {
    List<Staff> getAllStaff();
    Optional<Staff> getStaffById(Long id);
    Optional<Staff> getStaffByUsername(String username);
    Optional<Staff> getStaffByEmployeeId(String employeeId);
    boolean existsByUsername(String username);
    Staff createStaff(StaffCreateDto dto);
    Staff saveStaff(Staff staff);
    void deleteStaff(Long id);
    long getTotalStaffCount();
}
