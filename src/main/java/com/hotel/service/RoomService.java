package com.hotel.service;

import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import java.util.List;
import java.util.Optional;

/**
 * Service interface for luxury room inventory and status management.
 */
public interface RoomService {
    List<Room> getAllRooms();
    List<Room> getAvailableRooms();
    List<Room> getRoomsByCategory(RoomCategory category);
    Optional<Room> getRoomById(Long id);
    Optional<Room> getRoomByNumber(String roomNumber);
    Room saveRoom(Room room);
    Room updateRoomStatus(Long roomId, RoomStatus newStatus);
    void deleteRoom(Long id);
    long countByStatus(RoomStatus status);
}
