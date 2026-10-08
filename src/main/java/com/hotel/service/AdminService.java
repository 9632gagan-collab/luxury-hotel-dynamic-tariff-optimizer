package com.hotel.service;

import com.hotel.model.Admin;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for Admin operations and system dashboards.
 */
public interface AdminService {
    List<Admin> getAllAdmins();
    Optional<Admin> getAdminById(Long id);
    Optional<Admin> getAdminByUsername(String username);
    boolean existsByUsername(String username);
    Admin saveAdmin(Admin admin);
    Map<String, Object> getDashboardMetrics();
}
