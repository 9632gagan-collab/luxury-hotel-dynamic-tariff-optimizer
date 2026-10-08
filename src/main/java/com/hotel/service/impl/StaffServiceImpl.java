package com.hotel.service.impl;

import com.hotel.dto.StaffCreateDto;
import com.hotel.model.Staff;
import com.hotel.model.enums.AccountStatus;
import com.hotel.repository.StaffRepository;
import com.hotel.repository.UserRepository;
import com.hotel.service.StaffService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Staff management and Admin provisioning.
 */
@Service
@Transactional
public class StaffServiceImpl implements StaffService {

    private final StaffRepository staffRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public StaffServiceImpl(StaffRepository staffRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.staffRepository = staffRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Staff> getAllStaff() {
        return staffRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Staff> getStaffById(Long id) {
        return staffRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Staff> getStaffByUsername(String username) {
        return staffRepository.findByUsernameIgnoreCase(username != null ? username.trim() : "");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Staff> getStaffByEmployeeId(String employeeId) {
        return staffRepository.findByEmployeeId(employeeId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return staffRepository.existsByUsernameIgnoreCase(username != null ? username.trim() : "")
                || userRepository.existsByUsername(username != null ? username.trim() : "");
    }

    @Override
    public Staff createStaff(StaffCreateDto dto) {
        String cleanName = dto.getStaffName() != null ? dto.getStaffName().trim() : "";

        if (cleanName.isBlank()) {
            throw new IllegalArgumentException("Staff name cannot be blank.");
        }
        if (existsByUsername(cleanName)) {
            throw new IllegalArgumentException("Staff member with username '" + cleanName + "' already exists.");
        }
        if (!dto.isPasswordMatching()) {
            throw new IllegalArgumentException("Password and Confirm Password do not match.");
        }

        String hashedPassword = passwordEncoder.encode(dto.getPassword());
        Staff staff = new Staff(cleanName, hashedPassword);
        staff.setStatus(AccountStatus.ACTIVE);
        staff.setActive(true);

        return staffRepository.save(staff);
    }

    @Override
    public Staff saveStaff(Staff staff) {
        return staffRepository.save(staff);
    }

    @Override
    public void deleteStaff(Long id) {
        staffRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalStaffCount() {
        return staffRepository.count();
    }
}
