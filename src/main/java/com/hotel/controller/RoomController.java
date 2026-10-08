package com.hotel.controller;

import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for Room browsing and details.
 * Contains placeholder endpoints for Stage 1.
 */
@Controller
@RequestMapping("/rooms")
public class RoomController {

    private final RoomService roomService;

    @Autowired
    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public String listRooms(@RequestParam(required = false) RoomCategory category, Model model) {
        if (category != null) {
            model.addAttribute("rooms", roomService.getRoomsByCategory(category));
            model.addAttribute("selectedCategory", category);
        } else {
            model.addAttribute("rooms", roomService.getAllRooms());
        }
        model.addAttribute("categories", RoomCategory.values());
        return "guest/search-rooms";
    }

    @GetMapping("/{id}")
    public String viewRoomDetails(@PathVariable Long id, Model model) {
        Optional<Room> room = roomService.getRoomById(id);
        if (room.isPresent()) {
            model.addAttribute("room", room.get());
            return "guest/search-rooms"; // In Stage 2, a dedicated room-detail template will be added
        }
        return "redirect:/rooms";
    }
}
