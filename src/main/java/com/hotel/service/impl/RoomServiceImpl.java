package com.hotel.service.impl;

import com.hotel.exception.RoomUnavailableException;
import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import com.hotel.repository.RoomRepository;
import com.hotel.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for Room inventory operations.
 */
@Service
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Autowired
    public RoomServiceImpl(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getAvailableRooms() {
        return roomRepository.findByStatus(RoomStatus.AVAILABLE);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> getRoomsByCategory(RoomCategory category) {
        return roomRepository.findByCategory(category);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Room> getRoomById(Long id) {
        return roomRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Room> getRoomByNumber(String roomNumber) {
        return roomRepository.findByRoomNumber(roomNumber);
    }

    @Override
    public Room saveRoom(Room room) {
        return roomRepository.save(room);
    }

    @Override
    public Room updateRoomStatus(Long roomId, RoomStatus newStatus) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RoomUnavailableException("Room with ID " + roomId + " not found."));
        room.setStatus(newStatus);
        return roomRepository.save(room);
    }

    @Override
    public void deleteRoom(Long id) {
        roomRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByStatus(RoomStatus status) {
        return roomRepository.countByStatus(status);
    }
}
