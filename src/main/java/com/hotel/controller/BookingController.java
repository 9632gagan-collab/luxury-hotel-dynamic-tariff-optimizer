package com.hotel.controller;

import com.hotel.dto.BookingRequestDto;
import com.hotel.exception.BookingException;
import com.hotel.model.Booking;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.service.BookingService;
import com.hotel.service.HotelService;
import com.hotel.service.RoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Controller handling Guest Room Booking workflows in Stage 5.
 * Coordinates hotel discovery, room selection, live availability checks,
 * price locking, and booking creation.
 * Strictly enforces that Check-in and Check-out dates are required.
 */
@Controller
@RequestMapping("/bookings")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final RoomService roomService;
    private final HotelService hotelService;
    private final com.hotel.service.DemoDataService demoDataService;

    @Autowired
    public BookingController(BookingService bookingService, RoomService roomService, HotelService hotelService, com.hotel.service.DemoDataService demoDataService) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.hotelService = hotelService;
        this.demoDataService = demoDataService;
    }

    @InitBinder
    public void initBinder(org.springframework.web.bind.WebDataBinder binder) {
        binder.registerCustomEditor(LocalDate.class, new java.beans.PropertyEditorSupport() {
            @Override
            public void setAsText(String text) throws IllegalArgumentException {
                setValue(com.hotel.util.DateTimeUtil.parseFlexibleLocalDate(text));
            }

            @Override
            public String getAsText() {
                LocalDate val = (LocalDate) getValue();
                return val != null ? val.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) : "";
            }
        });
    }

    /**
     * Show booking form / destination page for Guest.
     * When user clicks 'Book Now' or 'Book / Check Availability', they are directed here.
     * Empty dates remain empty by default - no automatic fallback to today's date.
     */
    @GetMapping("/new")
    public String showBookingForm(@RequestParam(required = false) Long roomId,
                                  @RequestParam(required = false) Long hotelId,
                                  @RequestParam(required = false) String roomType,
                                  @RequestParam(required = false) String error,
                                  @RequestParam(required = false) LocalDate checkIn,
                                  @RequestParam(required = false) LocalDate checkOut,
                                  @RequestParam(required = false) Integer guests,
                                  Model model,
                                  Principal principal) {
        BookingRequestDto bookingDto = new BookingRequestDto();
        // Preserves empty dates until guest explicitly selects them
        bookingDto.setCheckInDate(checkIn);
        bookingDto.setCheckOutDate(checkOut);
        bookingDto.setNumberOfGuests(guests != null && guests > 0 ? guests : 2);

        List<Room> allRooms = roomService.getAllRooms();
        model.addAttribute("allRooms", allRooms);

        if (hotelId != null) {
            bookingDto.setHotelId(hotelId);
            hotelService.getHotelById(hotelId).ifPresent(hotel -> model.addAttribute("selectedHotel", hotel));
        }

        Room selectedRoom = null;
        if (roomId != null) {
            Optional<Room> rOpt = roomService.getRoomById(roomId);
            if (rOpt.isPresent()) {
                selectedRoom = rOpt.get();
            }
        }

        // Automatic room matching based on the user's selected Room Type from Find Hotels (Stage 6)
        if (roomType != null && !roomType.isBlank()) {
            String rt = roomType.trim().toLowerCase();
            for (Room r : allRooms) {
                if (rt.contains("suite") || rt.contains("multi")) {
                    if (r.getCategory() == RoomCategory.SUITE || r.getCategory() == RoomCategory.PRESIDENTIAL) {
                        selectedRoom = r;
                        break;
                    }
                } else if (rt.contains("shared")) {
                    if (r.getMaxOccupancy() >= 2) {
                        selectedRoom = r;
                        break;
                    }
                } else if (rt.contains("private") || rt.contains("single") || rt.contains("deluxe")) {
                    if (r.getCategory() == RoomCategory.DELUXE) {
                        selectedRoom = r;
                        break;
                    }
                } else if (r.getCategory().name().equalsIgnoreCase(roomType.trim())
                        || r.getCategory().getDisplayName().equalsIgnoreCase(roomType.trim())) {
                    selectedRoom = r;
                    break;
                }
            }
        }

        if (selectedRoom == null && !allRooms.isEmpty()) {
            selectedRoom = allRooms.get(0);
        }

        if (selectedRoom != null) {
            bookingDto.setRoomId(selectedRoom.getId());
            model.addAttribute("selectedRoom", selectedRoom);
        }

        // Automatically preserve and assign room type from Find Hotels
        if (roomType != null && !roomType.isBlank()) {
            bookingDto.setRoomType(roomType.trim());
            model.addAttribute("selectedRoomType", roomType.trim());
        } else if (selectedRoom != null && selectedRoom.getCategory() != null) {
            bookingDto.setRoomType(selectedRoom.getCategory().getDisplayName());
            model.addAttribute("selectedRoomType", selectedRoom.getCategory().getDisplayName());
        } else {
            bookingDto.setRoomType("Private / Single Room");
            model.addAttribute("selectedRoomType", "Private / Single Room");
        }

        // Calculate dynamic estimated duration and stay total ONLY when both dates are provided
        if (checkIn != null && checkOut != null && checkOut.isAfter(checkIn)) {
            long estimatedNights = ChronoUnit.DAYS.between(checkIn, checkOut);
            model.addAttribute("estimatedNights", estimatedNights);

            if (selectedRoom != null) {
                double rate = selectedRoom.getCurrentTariff() > 0 ? selectedRoom.getCurrentTariff() : selectedRoom.getBasePricePerNight();
                model.addAttribute("estimatedTotal", rate * estimatedNights);
            }
        } else {
            model.addAttribute("estimatedNights", null);
            model.addAttribute("estimatedTotal", null);
        }

        // Display user-friendly error banners if redirected due to validation failure
        if (error != null) {
            switch (error) {
                case "missing_dates" -> model.addAttribute("errorMessage",
                        "Please select check-in and check-out dates.");
                case "missing_guest_details" -> model.addAttribute("errorMessage",
                        "Please provide complete details (Name, Guest Type, Age, and Gender) for all travelers.");
                case "invalid_guest_age" -> model.addAttribute("errorMessage",
                        "Adults must be age 18 or older, and Minors must be under 18 years old.");
                case "guest_count_mismatch" -> model.addAttribute("errorMessage",
                        "Number of traveler entries must match the selected guest count.");
                case "invalid_dates" -> model.addAttribute("errorMessage",
                        "Check-out date must be strictly after the check-in date.");
                case "past_date" -> model.addAttribute("errorMessage",
                        "Check-in date cannot be in the past.");
                case "unavailable" -> model.addAttribute("errorMessage",
                        "The selected room is unavailable for the selected dates. Another guest has an active reservation during this period. Please choose different dates or select another suite.");
                case "room_not_found" -> model.addAttribute("errorMessage",
                        "The requested room accommodation was not found. Please choose an available room from the list.");
                case "guest_required" -> model.addAttribute("errorMessage",
                        "Please sign in with a registered Guest account to confirm your room reservation.");
                default -> model.addAttribute("errorMessage",
                        "Unable to confirm booking. Please review your reservation details and try again.");
            }
        }

        // Pre-populate guestDetails for initial render matching guest count
        List<com.hotel.model.BookingGuestDetail> initialDetails = new java.util.ArrayList<>();
        int count = bookingDto.getNumberOfGuests();
        for (int i = 0; i < count; i++) {
            initialDetails.add(new com.hotel.model.BookingGuestDetail("", "Adult", null, ""));
        }
        bookingDto.setGuestDetails(initialDetails);

        model.addAttribute("bookingDto", bookingDto);
        return "guest/booking";
    }

    /**
     * Submit booking request and create real database record.
     * Strictly verifies that both check-in and check-out dates are present.
     */
    @PostMapping("/submit")
    public String submitBooking(@ModelAttribute BookingRequestDto bookingRequestDto,
                                Principal principal,
                                RedirectAttributes redirectAttributes,
                                HttpSession session) {
        if (principal == null) {
            return "redirect:/login?role=GUEST";
        }

        LocalDate checkIn = bookingRequestDto.getCheckInDate();
        LocalDate checkOut = bookingRequestDto.getCheckOutDate();

        // Case 1, 2, 3: Either check-in or check-out is missing
        if (checkIn == null || checkOut == null) {
            redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
            if (bookingRequestDto.getHotelId() != null) {
                redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
            }
            if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) {
                redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
            }
            if (checkIn != null) {
                redirectAttributes.addAttribute("checkIn", checkIn);
            }
            if (checkOut != null) {
                redirectAttributes.addAttribute("checkOut", checkOut);
            }
            if (bookingRequestDto.getNumberOfGuests() > 0) {
                redirectAttributes.addAttribute("guests", bookingRequestDto.getNumberOfGuests());
            }
            redirectAttributes.addAttribute("error", "missing_dates");
            return "redirect:/bookings/new";
        }

        // Case 5 & 6: Check-out date before or equal to check-in date
        if (!checkOut.isAfter(checkIn)) {
            redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
            if (bookingRequestDto.getHotelId() != null) {
                redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
            }
            if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) {
                redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
            }
            redirectAttributes.addAttribute("checkIn", checkIn);
            redirectAttributes.addAttribute("checkOut", checkOut);
            if (bookingRequestDto.getNumberOfGuests() > 0) {
                redirectAttributes.addAttribute("guests", bookingRequestDto.getNumberOfGuests());
            }
            redirectAttributes.addAttribute("error", "invalid_dates");
            return "redirect:/bookings/new";
        }

        // Validate guest details rows
        int guestCount = bookingRequestDto.getNumberOfGuests();
        if (guestCount <= 0) {
            guestCount = 1;
            bookingRequestDto.setNumberOfGuests(1);
        }

        List<com.hotel.model.BookingGuestDetail> guestDetails = bookingRequestDto.getGuestDetails();
        if (guestDetails == null || guestDetails.isEmpty()) {
            List<com.hotel.model.BookingGuestDetail> defaultList = new java.util.ArrayList<>();
            for (int i = 0; i < guestCount; i++) {
                defaultList.add(new com.hotel.model.BookingGuestDetail("Adult", 30, "Male"));
            }
            bookingRequestDto.setGuestDetails(defaultList);
        } else {
            if (guestDetails.size() < guestCount) {
                redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
                if (bookingRequestDto.getHotelId() != null) {
                    redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
                }
                if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) {
                    redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
                }
                if (checkIn != null) redirectAttributes.addAttribute("checkIn", checkIn);
                if (checkOut != null) redirectAttributes.addAttribute("checkOut", checkOut);
                redirectAttributes.addAttribute("guests", guestCount);
                redirectAttributes.addAttribute("error", "missing_guest_details");
                return "redirect:/bookings/new";
            }

            for (int i = 0; i < guestCount; i++) {
                com.hotel.model.BookingGuestDetail gd = guestDetails.get(i);
                if (gd == null
                        || (gd.getName() != null && (gd.getName().isBlank() || gd.getName().matches("(?i)^(traveler\\s*\\d+|enter\\s*(full\\s*)?name)$")))
                        || gd.getGuestType() == null || gd.getGuestType().isBlank()
                        || gd.getAge() == null || gd.getAge() <= 0 || gd.getAge() > 120
                        || gd.getGender() == null || gd.getGender().isBlank()) {
                    redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
                    if (bookingRequestDto.getHotelId() != null) {
                        redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
                    }
                    if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) {
                        redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
                    }
                    if (checkIn != null) redirectAttributes.addAttribute("checkIn", checkIn);
                    if (checkOut != null) redirectAttributes.addAttribute("checkOut", checkOut);
                    redirectAttributes.addAttribute("guests", guestCount);
                    redirectAttributes.addAttribute("error", "missing_guest_details");
                    return "redirect:/bookings/new";
                }

                // Sensible Adult vs Minor age validation
                String type = gd.getGuestType().trim();
                int age = gd.getAge();
                if ((type.equalsIgnoreCase("Minor") && age >= 18) || (type.equalsIgnoreCase("Adult") && age < 18)) {
                    redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
                    if (bookingRequestDto.getHotelId() != null) redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
                    if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
                    if (checkIn != null) redirectAttributes.addAttribute("checkIn", checkIn);
                    if (checkOut != null) redirectAttributes.addAttribute("checkOut", checkOut);
                    redirectAttributes.addAttribute("guests", guestCount);
                    redirectAttributes.addAttribute("error", "invalid_guest_age");
                    return "redirect:/bookings/new";
                }
            }

            // Trim excess details if more than guestCount were submitted
            if (guestDetails.size() > guestCount) {
                bookingRequestDto.setGuestDetails(new java.util.ArrayList<>(guestDetails.subList(0, guestCount)));
            }
        }

        String username = principal.getName();
        if (demoDataService.isDemoMode(session) || "demo_guest".equalsIgnoreCase(username)) {
            demoDataService.createDemoBooking(session, bookingRequestDto);
            redirectAttributes.addFlashAttribute("successMessage", "Your luxury room reservation was confirmed successfully! (Demo Mode)");
            return "redirect:/guest/my-bookings?booked=true";
        }
        try {
            Booking savedBooking = bookingService.createBooking(bookingRequestDto, username);
            log.info("Guest '{}' confirmed booking: {}", username, savedBooking.getBookingReference());
            return "redirect:/guest/my-bookings?booked=true";
        } catch (BookingException ex) {
            log.warn("Booking creation failed for guest '{}': {}", username, ex.getMessage());
            redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
            if (bookingRequestDto.getHotelId() != null) {
                redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
            }
            if (bookingRequestDto.getRoomType() != null && !bookingRequestDto.getRoomType().isBlank()) {
                redirectAttributes.addAttribute("roomType", bookingRequestDto.getRoomType());
            }
            if (bookingRequestDto.getCheckInDate() != null) {
                redirectAttributes.addAttribute("checkIn", bookingRequestDto.getCheckInDate());
            }
            if (bookingRequestDto.getCheckOutDate() != null) {
                redirectAttributes.addAttribute("checkOut", bookingRequestDto.getCheckOutDate());
            }
            if (bookingRequestDto.getNumberOfGuests() > 0) {
                redirectAttributes.addAttribute("guests", bookingRequestDto.getNumberOfGuests());
            }

            String msg = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
            if (msg.contains("guest") || msg.contains("age") || msg.contains("gender") || msg.contains("minor") || msg.contains("adult")) {
                redirectAttributes.addAttribute("error", "missing_guest_details");
            } else if (msg.contains("please select check-in and check-out dates") || msg.contains("missing") || msg.contains("required")) {
                redirectAttributes.addAttribute("error", "missing_dates");
            } else if (msg.contains("unavailable")) {
                redirectAttributes.addAttribute("error", "unavailable");
            } else if (msg.contains("past")) {
                redirectAttributes.addAttribute("error", "past_date");
            } else if (msg.contains("date")) {
                redirectAttributes.addAttribute("error", "invalid_dates");
            } else if (msg.contains("room")) {
                redirectAttributes.addAttribute("error", "room_not_found");
            } else {
                redirectAttributes.addAttribute("error", "general");
            }
            return "redirect:/bookings/new";
        } catch (Exception ex) {
            log.error("Unexpected error during booking confirmation", ex);
            redirectAttributes.addAttribute("roomId", bookingRequestDto.getRoomId());
            if (bookingRequestDto.getHotelId() != null) {
                redirectAttributes.addAttribute("hotelId", bookingRequestDto.getHotelId());
            }
            redirectAttributes.addAttribute("error", "general");
            return "redirect:/bookings/new";
        }
    }
}
