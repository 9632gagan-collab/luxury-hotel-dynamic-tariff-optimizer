package com.hotel.service.impl;

import com.hotel.model.Admin;
import com.hotel.model.Staff;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.AdminRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.StaffRepository;
import com.hotel.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service implementation for Admin operations and dashboard statistics.
 */
@Service
@Transactional
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final GuestRepository guestRepository;

    @Autowired
    public AdminServiceImpl(AdminRepository adminRepository,
                            StaffRepository staffRepository,
                            GuestRepository guestRepository) {
        this.adminRepository = adminRepository;
        this.staffRepository = staffRepository;
        this.guestRepository = guestRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> getAdminById(Long id) {
        return adminRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Admin> getAdminByUsername(String username) {
        return adminRepository.findByUsernameIgnoreCase(username != null ? username.trim() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return adminRepository.existsByUsernameIgnoreCase(username != null ? username.trim() : "");
    }

    @Override
    public Admin saveAdmin(Admin admin) {
        return adminRepository.save(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        long totalGuests = guestRepository.count();
        long totalStaff = staffRepository.count();
        long activeStaff = staffRepository.findAll().stream()
                .filter(s -> s.getStatus() == AccountStatus.ACTIVE && s.isActive())
                .count();
        long totalAdmins = adminRepository.count();

        metrics.put("totalGuests", totalGuests);
        metrics.put("totalStaff", totalStaff);
        metrics.put("activeStaff", activeStaff);
        metrics.put("totalAdmins", totalAdmins);

        return metrics;
    }
}
