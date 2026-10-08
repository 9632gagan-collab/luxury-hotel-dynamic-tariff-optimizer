package com.hotel.repository;

import com.hotel.model.Staff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Staff entities.
 */
@Repository
public interface StaffRepository extends JpaRepository<Staff, Long> {
    Optional<Staff> findByEmployeeId(String employeeId);
    List<Staff> findByDepartment(String department);
    Optional<Staff> findByUsername(String username);
    Optional<Staff> findByUsernameIgnoreCase(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
