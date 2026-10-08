package com.hotel;

import com.hotel.dto.BookingRequestDto;
import com.hotel.dto.GuestRegistrationDto;
import com.hotel.exception.BookingException;
import com.hotel.model.Booking;
import com.hotel.model.Guest;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.enums.BookingStatus;
import com.hotel.model.enums.RoomCategory;
import com.hotel.model.enums.RoomStatus;
import com.hotel.repository.BookingRepository;
import com.hotel.repository.GuestRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.RoomRepository;
import com.hotel.service.BookingService;
import com.hotel.service.GuestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Stage 5 Test Suite: Verifies Complete Guest Booking Lifecycle.
 * Covers:
 * - Room availability check across date ranges
 * - Overlapping double-booking prevention
 * - Non-overlapping subsequent reservation acceptance
 * - Booking record persistence with relationships (Guest, Hotel, Room)
 * - Historical price locking (immutable total amount)
 * - Active stays vs. completed booking history segmentation
 * - Automatic completion transitions when check-out date passes
 * - Room availability restoration following completed stays
 * - Strict multi-tenant security isolation between guests
 * - Staff and Admin centralized oversight
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class Stage5BookingLifecycleTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestService guestService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private HotelRepository hotelRepository;

    private Guest guestA;
    private Guest guestB;
    private Room testRoom;
    private Hotel testHotel;

    @BeforeEach
    void setUp() {
        // Setup Guest A
        guestA = guestRepository.findByUsernameIgnoreCase("stage5_guest_a").orElse(null);
        if (guestA == null) {
            guestA = guestService.registerGuest(new GuestRegistrationDto(
                    "stage5_guest_a", "stage5a@royalindianstays.com", "Password123!", "Password123!"));
        }

        // Setup Guest B
        guestB = guestRepository.findByUsernameIgnoreCase("stage5_guest_b").orElse(null);
        if (guestB == null) {
            guestB = guestService.registerGuest(new GuestRegistrationDto(
                    "stage5_guest_b", "stage5b@royalindianstays.com", "Password123!", "Password123!"));
        }

        // Setup Test Room
        testRoom = roomRepository.findAll().stream().findFirst().orElseGet(() -> {
            Room r = new Room("501", RoomCategory.DELUXE, RoomStatus.AVAILABLE, 5000.0, 5, 2, "Test Room", null);
            return roomRepository.save(r);
        });

        // Setup Test Hotel
        testHotel = hotelRepository.findAll().stream().findFirst().orElse(null);
    }

    @Test
    @DisplayName("Stage 5.1: Create real booking record with relationships, locked price, and reference")
    void testCreateBookingSuccess() {
        LocalDate in = LocalDate.now().plusDays(2);
        LocalDate out = LocalDate.now().plusDays(5); // 3 nights

        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setHotelId(testHotel != null ? testHotel.getId() : null);
        dto.setCheckInDate(in);
        dto.setCheckOutDate(out);
        dto.setNumberOfGuests(2);
        dto.setSpecialRequests("Quiet corner room, top floor preference");

        Booking booking = bookingService.createBooking(dto, guestA.getUsername());

        assertNotNull(booking.getId(), "Booking record must have database primary key");
        assertNotNull(booking.getBookingReference(), "Booking reference must be generated");
        assertTrue(booking.getBookingReference().startsWith("BK-"), "Reference format must start with BK-");
        assertEquals(guestA.getId(), booking.getGuest().getId(), "Booking must link to authenticated guest");
        assertEquals(testRoom.getId(), booking.getRoom().getId(), "Booking must link to selected room");
        assertEquals(in, booking.getCheckInDate());
        assertEquals(out, booking.getCheckOutDate());
        assertEquals(2, booking.getNumberOfGuests());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
        assertEquals("Quiet corner room, top floor preference", booking.getSpecialRequests());

        double expectedTariff = (testRoom.getCurrentTariff() > 0 ? testRoom.getCurrentTariff() : testRoom.getBasePricePerNight()) * 3;
        assertEquals(expectedTariff, booking.getTotalAmount(), 0.01, "Historical price must be locked to nights * rate");
    }

    @Test
    @DisplayName("Stage 5.2: Overlapping booking for same room must be rejected (Availability Check)")
    void testDoubleBookingOverlapPrevention() {
        LocalDate in1 = LocalDate.now().plusDays(10);
        LocalDate out1 = LocalDate.now().plusDays(15);

        BookingRequestDto dto1 = new BookingRequestDto();
        dto1.setRoomId(testRoom.getId());
        dto1.setHotelId(testHotel != null ? testHotel.getId() : null);
        dto1.setCheckInDate(in1);
        dto1.setCheckOutDate(out1);
        dto1.setNumberOfGuests(2);

        bookingService.createBooking(dto1, guestA.getUsername());

        // Overlap Case A: Exactly same dates
        BookingRequestDto conflictExact = new BookingRequestDto();
        conflictExact.setRoomId(testRoom.getId());
        conflictExact.setCheckInDate(in1);
        conflictExact.setCheckOutDate(out1);
        assertFalse(bookingService.isRoomAvailable(testRoom.getId(), in1, out1));
        assertThrows(BookingException.class, () -> bookingService.createBooking(conflictExact, guestB.getUsername()));

        // Overlap Case B: Enclosed dates (11 to 14)
        LocalDate in2 = LocalDate.now().plusDays(11);
        LocalDate out2 = LocalDate.now().plusDays(14);
        assertFalse(bookingService.isRoomAvailable(testRoom.getId(), in2, out2));

        // Overlap Case C: Overlapping start (8 to 12)
        LocalDate in3 = LocalDate.now().plusDays(8);
        LocalDate out3 = LocalDate.now().plusDays(12);
        assertFalse(bookingService.isRoomAvailable(testRoom.getId(), in3, out3));

        // Overlap Case D: Overlapping end (13 to 17)
        LocalDate in4 = LocalDate.now().plusDays(13);
        LocalDate out4 = LocalDate.now().plusDays(17);
        assertFalse(bookingService.isRoomAvailable(testRoom.getId(), in4, out4));
    }

    @Test
    @DisplayName("Stage 5.3: Non-overlapping future booking for same room is allowed")
    void testNonOverlappingBookingAllowed() {
        LocalDate in1 = LocalDate.now().plusDays(10);
        LocalDate out1 = LocalDate.now().plusDays(14);

        BookingRequestDto dto1 = new BookingRequestDto();
        dto1.setRoomId(testRoom.getId());
        dto1.setHotelId(testHotel != null ? testHotel.getId() : null);
        dto1.setCheckInDate(in1);
        dto1.setCheckOutDate(out1);
        dto1.setNumberOfGuests(2);
        bookingService.createBooking(dto1, guestA.getUsername());

        // Subsequent booking starting on check-out day (14 to 18)
        LocalDate inFuture = LocalDate.now().plusDays(14);
        LocalDate outFuture = LocalDate.now().plusDays(18);
        assertTrue(bookingService.isRoomAvailable(testRoom.getId(), inFuture, outFuture),
                "Room should be available starting on previous guest's check-out date");

        BookingRequestDto dto2 = new BookingRequestDto();
        dto2.setRoomId(testRoom.getId());
        dto2.setHotelId(testHotel != null ? testHotel.getId() : null);
        dto2.setCheckInDate(inFuture);
        dto2.setCheckOutDate(outFuture);
        dto2.setNumberOfGuests(2);

        Booking booking2 = bookingService.createBooking(dto2, guestB.getUsername());
        assertNotNull(booking2.getId(), "Non-overlapping booking must succeed");
    }

    @Test
    @DisplayName("Stage 5.4: Price storage is historical and immutable when room tariff later changes")
    void testHistoricalPriceLocking() {
        double originalTariff = testRoom.getCurrentTariff();
        LocalDate in = LocalDate.now().plusDays(3);
        LocalDate out = LocalDate.now().plusDays(5); // 2 nights

        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(in);
        dto.setCheckOutDate(out);
        dto.setNumberOfGuests(1);

        Booking booking = bookingService.createBooking(dto, guestA.getUsername());
        double originalBookingAmount = booking.getTotalAmount();
        assertEquals(originalTariff * 2, originalBookingAmount, 0.01);

        // Later room tariff changes significantly
        testRoom.setCurrentTariff(originalTariff * 3.5);
        roomRepository.save(testRoom);

        // The confirmed booking total must remain unchanged
        Booking retrieved = bookingService.getBookingById(booking.getId()).orElseThrow();
        assertEquals(originalBookingAmount, retrieved.getTotalAmount(), 0.01,
                "Confirmed booking price must be historically preserved");
    }

    @Test
    @DisplayName("Stage 5.5: Multi-Tenant Security Isolation: Guest A cannot see Guest B's bookings")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testGuestSecurityIsolation() throws Exception {
        // Create booking for Guest A
        BookingRequestDto dtoA = new BookingRequestDto();
        dtoA.setRoomId(testRoom.getId());
        dtoA.setCheckInDate(LocalDate.now().plusDays(20));
        dtoA.setCheckOutDate(LocalDate.now().plusDays(23));
        dtoA.setNumberOfGuests(2);
        Booking bookingA = bookingService.createBooking(dtoA, guestA.getUsername());

        // Create booking for Guest B on different room or dates
        Room otherRoom = roomRepository.findAll().stream()
                .filter(r -> !r.getId().equals(testRoom.getId()))
                .findFirst()
                .orElse(testRoom);

        BookingRequestDto dtoB = new BookingRequestDto();
        dtoB.setRoomId(otherRoom.getId());
        dtoB.setCheckInDate(LocalDate.now().plusDays(25));
        dtoB.setCheckOutDate(LocalDate.now().plusDays(28));
        dtoB.setNumberOfGuests(1);
        Booking bookingB = bookingService.createBooking(dtoB, guestB.getUsername());

        // Guest A visits /guest/my-bookings
        mockMvc.perform(get("/guest/my-bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/my-bookings"))
                .andExpect(model().attribute("bookings", hasItem(hasProperty("bookingReference", is(bookingA.getBookingReference())))))
                .andExpect(model().attribute("bookings", not(hasItem(hasProperty("bookingReference", is(bookingB.getBookingReference()))))));
    }

    @Test
    @DisplayName("Stage 5.6: Automatic completion when check-out date passes, moves to Booking History")
    void testAutomaticCompletionAndHistoryMove() {
        // Create a booking with past check-out date
        LocalDate pastCheckIn = LocalDate.now().minusDays(5);
        LocalDate pastCheckOut = LocalDate.now().minusDays(1);

        Booking pastBooking = new Booking();
        pastBooking.setBookingReference("BK-PAST-TEST");
        pastBooking.setGuest(guestA);
        pastBooking.setHotel(testHotel);
        pastBooking.setRoom(testRoom);
        pastBooking.setCheckInDate(pastCheckIn);
        pastBooking.setCheckOutDate(pastCheckOut);
        pastBooking.setNumberOfGuests(2);
        pastBooking.setTotalAmount(15000.0);
        pastBooking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(pastBooking);

        // Active bookings query must NOT include the past booking
        List<Booking> activeStays = bookingService.getActiveBookingsByGuest(guestA.getId());
        assertTrue(activeStays.stream().noneMatch(b -> b.getBookingReference().equals("BK-PAST-TEST")),
                "Past booking must not appear in active My Bookings");

        // Booking History query MUST include the completed stay
        List<Booking> historyStays = bookingService.getPastBookingsByGuest(guestA.getId());
        assertTrue(historyStays.stream().anyMatch(b -> b.getBookingReference().equals("BK-PAST-TEST")),
                "Past booking must automatically appear in Booking History");

        Booking updated = bookingRepository.findByBookingReference("BK-PAST-TEST").orElseThrow();
        assertEquals(BookingStatus.COMPLETED, updated.getStatus(),
                "Expired stay must automatically progress to COMPLETED status");
    }

    @Test
    @DisplayName("Stage 5.7: Room is available for future dates after previous booking completes")
    void testRoomAvailabilityAfterCheckout() {
        // Room had a past booking
        Booking pastBooking = new Booking();
        pastBooking.setBookingReference("BK-PAST-AVAIL");
        pastBooking.setGuest(guestA);
        pastBooking.setHotel(testHotel);
        pastBooking.setRoom(testRoom);
        pastBooking.setCheckInDate(LocalDate.now().minusDays(4));
        pastBooking.setCheckOutDate(LocalDate.now().minusDays(1));
        pastBooking.setNumberOfGuests(2);
        pastBooking.setTotalAmount(10000.0);
        pastBooking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(pastBooking);

        // Future dates for same room are available
        LocalDate nextIn = LocalDate.now().plusDays(1);
        LocalDate nextOut = LocalDate.now().plusDays(4);
        assertTrue(bookingService.isRoomAvailable(testRoom.getId(), nextIn, nextOut),
                "Room must be available after previous booking reached check-out");
    }

    @Test
    @DisplayName("Stage 5.8: Form submission with overlapping room redirects to /bookings/new with error")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testWebBookingSubmissionConflict() throws Exception {
        LocalDate in = LocalDate.now().plusDays(30);
        LocalDate out = LocalDate.now().plusDays(34);

        // Initial booking
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(in);
        dto.setCheckOutDate(out);
        dto.setNumberOfGuests(2);
        bookingService.createBooking(dto, guestB.getUsername());

        // Guest A attempts to book the same room on overlapping dates via web form
        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("roomId", testRoom.getId().toString())
                        .param("checkInDate", in.toString())
                        .param("checkOutDate", out.toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new*error=unavailable*"));
    }

    @Test
    @DisplayName("Stage 5.9: Staff and Admin can view all guest bookings")
    @WithMockUser(username = "DARSHAN", roles = {"ADMIN"})
    void testAdminCanViewAllBookings() throws Exception {
        mockMvc.perform(get("/admin/bookings"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/bookings"))
                .andExpect(model().attributeExists("bookings"));
    }

    // ==============================================================
    // DATE VALIDATION REQUIREMENT TESTS (CASES 1 to 6)
    // ==============================================================

    @Test
    @DisplayName("Case 1: Check-in empty and Check-out empty -> reject booking")
    void testCase1_BothDatesEmpty() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(null);
        dto.setCheckOutDate(null);

        BookingException ex = assertThrows(BookingException.class, () ->
                bookingService.createBooking(dto, guestA.getUsername()));
        assertEquals("Please select check-in and check-out dates.", ex.getMessage());
    }

    @Test
    @DisplayName("Case 2: Check-in selected and Check-out empty -> reject booking")
    void testCase2_OnlyCheckInSelected() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(LocalDate.now().plusDays(2));
        dto.setCheckOutDate(null);

        BookingException ex = assertThrows(BookingException.class, () ->
                bookingService.createBooking(dto, guestA.getUsername()));
        assertEquals("Please select check-in and check-out dates.", ex.getMessage());
    }

    @Test
    @DisplayName("Case 3: Check-in empty and Check-out selected -> reject booking")
    void testCase3_OnlyCheckOutSelected() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(null);
        dto.setCheckOutDate(LocalDate.now().plusDays(5));

        BookingException ex = assertThrows(BookingException.class, () ->
                bookingService.createBooking(dto, guestA.getUsername()));
        assertEquals("Please select check-in and check-out dates.", ex.getMessage());
    }

    @Test
    @DisplayName("Case 4: Both valid dates selected -> normal booking confirmation succeeds")
    void testCase4_BothValidDatesSelected() {
        LocalDate in = LocalDate.now().plusDays(40);
        LocalDate out = LocalDate.now().plusDays(43);

        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(in);
        dto.setCheckOutDate(out);
        dto.setNumberOfGuests(1);

        Booking booking = bookingService.createBooking(dto, guestA.getUsername());
        assertNotNull(booking);
        assertNotNull(booking.getId());
        assertEquals(in, booking.getCheckInDate());
        assertEquals(out, booking.getCheckOutDate());
        assertEquals(BookingStatus.CONFIRMED, booking.getStatus());
    }

    @Test
    @DisplayName("Case 5: Check-out date before check-in date -> reject booking")
    void testCase5_CheckOutBeforeCheckIn() {
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(LocalDate.now().plusDays(5));
        dto.setCheckOutDate(LocalDate.now().plusDays(2));

        BookingException ex = assertThrows(BookingException.class, () ->
                bookingService.createBooking(dto, guestA.getUsername()));
        assertEquals("Check-out date must be strictly after the check-in date.", ex.getMessage());
    }

    @Test
    @DisplayName("Case 6: Check-in and check-out are the same date -> reject booking")
    void testCase6_SameDayCheckInAndCheckOut() {
        LocalDate sameDay = LocalDate.now().plusDays(3);
        BookingRequestDto dto = new BookingRequestDto();
        dto.setRoomId(testRoom.getId());
        dto.setCheckInDate(sameDay);
        dto.setCheckOutDate(sameDay);

        BookingException ex = assertThrows(BookingException.class, () ->
                bookingService.createBooking(dto, guestA.getUsername()));
        assertEquals("Check-out date must be strictly after the check-in date.", ex.getMessage());
    }

    @Test
    @DisplayName("Web MVC: Missing check-in and check-out dates on /bookings/submit redirects to error=missing_dates")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testWebSubmitMissingDatesRedirects() throws Exception {
        long bookingCountBefore = bookingRepository.count();

        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("roomId", testRoom.getId().toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new*error=missing_dates*"));

        assertEquals(bookingCountBefore, bookingRepository.count(),
                "No booking record should be created when dates are missing");
    }

    @Test
    @DisplayName("Web MVC: Check-in or Check-out missing alone on /bookings/submit redirects to error=missing_dates")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testWebSubmitPartialDatesRedirects() throws Exception {
        long bookingCountBefore = bookingRepository.count();

        // Only check-in provided
        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("roomId", testRoom.getId().toString())
                        .param("checkInDate", LocalDate.now().plusDays(2).toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new*error=missing_dates*"));

        // Only check-out provided
        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("roomId", testRoom.getId().toString())
                        .param("checkOutDate", LocalDate.now().plusDays(5).toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new*error=missing_dates*"));

        assertEquals(bookingCountBefore, bookingRepository.count(),
                "No booking record should be created when dates are incomplete");
    }

    @Test
    @DisplayName("Web MVC: Invalid date range (out <= in) redirects to error=invalid_dates")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testWebSubmitInvalidDateRangeRedirects() throws Exception {
        mockMvc.perform(post("/bookings/submit")
                        .with(csrf())
                        .param("roomId", testRoom.getId().toString())
                        .param("checkInDate", LocalDate.now().plusDays(5).toString())
                        .param("checkOutDate", LocalDate.now().plusDays(2).toString())
                        .param("numberOfGuests", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/bookings/new*error=invalid_dates*"));
    }

    @Test
    @DisplayName("Web MVC: /bookings/new keeps dates empty by default and shows validation message on error")
    @WithMockUser(username = "stage5_guest_a", roles = {"GUEST"})
    void testBookingFormEmptyDatesByDefaultAndValidationMessage() throws Exception {
        // 1. When opening /bookings/new without date params, dates must be null (not defaulted to today or tomorrow)
        mockMvc.perform(get("/bookings/new").param("roomId", testRoom.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(view().name("guest/booking"))
                .andExpect(model().attributeExists("bookingDto"))
                .andExpect(result -> {
                    BookingRequestDto dto = (BookingRequestDto) result.getModelAndView().getModel().get("bookingDto");
                    assertNull(dto.getCheckInDate(), "Check-in date must be null by default");
                    assertNull(dto.getCheckOutDate(), "Check-out date must be null by default");
                });

        // 2. When redirected with error=missing_dates, shows the exact required validation message
        mockMvc.perform(get("/bookings/new")
                        .param("roomId", testRoom.getId().toString())
                        .param("error", "missing_dates"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("errorMessage", "Please select check-in and check-out dates."));
    }
}
