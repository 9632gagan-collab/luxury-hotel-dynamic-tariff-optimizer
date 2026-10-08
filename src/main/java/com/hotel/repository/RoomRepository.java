package com.hotel.repository;

import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository interface for Room entities.
 */
@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {
    Optional<Room> findByRoomNumber(String roomNumber);
    List<Room> findByStatus(RoomStatus status);
    List<Room> findByCategory(RoomCategory category);
    List<Room> findByCategoryAndStatus(RoomCategory category, RoomStatus status);
    long countByStatus(RoomStatus status);
}
