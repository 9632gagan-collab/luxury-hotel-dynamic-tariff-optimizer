package com.hotel;

import com.hotel.matcher.*;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.RoomRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class Stage2SmartPreferenceMatcherTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PreferenceMatcher preferenceMatcher;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    @DisplayName("Case 1: State + District only - Deterministic score and ranking")
    void testCase1_StateAndDistrictOnly() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state("Karnataka")
                .district("Mysuru")
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        // Top results must be Mysuru hotels with high scores
        HotelMatchResult top = results.get(0);
        assertTrue(top.getMatchScore() >= 90, "Expected high match score for district match, got: " + top.getMatchScore());
        assertEquals("Karnataka", top.getHotel().getState());
        assertEquals("Mysuru", top.getHotel().getDistrict());
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("District matches")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("State matches")));

        // Determinism test: evaluate same hotel twice, score must be identical
        HotelMatchResult topSecondRun = preferenceMatcher.matchHotel(top.getHotel(), criteria);
        assertEquals(top.getMatchScore(), topSecondRun.getMatchScore(), "Scores must be strictly deterministic");
    }

    @Test
    @DisplayName("Case 2: State + District + Experience - Validates experience match")
    void testCase2_StateDistrictExperience() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state("Karnataka")
                .district("Mysuru")
                .experience("Heritage")
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        HotelMatchResult top = results.get(0);
        assertTrue(top.getMatchScore() >= 90);
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Heritage experience matches")));
    }

    @Test
    @DisplayName("Case 3: State + District + Facilities - Validates multi-select facilities match")
    void testCase3_StateDistrictFacilities() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state("Karnataka")
                .district("Mysuru")
                .facilities(Arrays.asList("Swimming Pool", "Spa"))
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        HotelMatchResult top = results.get(0);
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Pool available")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Spa available")));
    }

    @Test
    @DisplayName("Case 4: State + District + People + Room Type - Room-level matching for 3 guests")
    void testCase4_StateDistrictPeopleRoomType() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state("Karnataka")
                .district("Mysuru")
                .guests(3)
                .roomType("Private / Single Room")
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        HotelMatchResult top = results.get(0);
        // For 3 guests, best room should be Suite
        assertEquals("Executive Suite", top.getRecommendedRoomName());
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Suitable for 3 guests")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Private room available")));
    }

    @Test
    @DisplayName("Case 5: Budget + Room Type - Filters within budget and reflects in score")
    void testCase5_BudgetAndRoomType() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .budget("0-10000")
                .roomType("Private / Single Room")
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        // Top results must be <= 10000
        for (int i = 0; i < Math.min(3, results.size()); i++) {
            HotelMatchResult r = results.get(i);
            assertTrue(r.getApplicablePrice() <= 10000.0, "Applicable price should be within budget: " + r.getApplicablePrice());
            assertTrue(r.getMatchReasons().stream().anyMatch(re -> re.contains("Within selected budget")));
        }
    }

    @Test
    @DisplayName("Case 6: All filters together - Evaluates complete combined pipeline")
    void testCase6_AllFiltersTogether() {
        List<Hotel> allHotels = hotelRepository.findAll();
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state("Karnataka")
                .district("Mysuru")
                .experience("Heritage")
                .facilities(Arrays.asList("Swimming Pool", "Spa"))
                .guests(3)
                .roomType("Private / Single Room")
                .budget("0-15000")
                .checkIn("2026-10-12")
                .checkOut("2026-10-14")
                .build();

        List<HotelMatchResult> results = preferenceMatcher.matchAndRankHotels(allHotels, criteria);
        assertFalse(results.isEmpty());

        HotelMatchResult top = results.get(0);
        assertEquals("Karnataka", top.getHotel().getState());
        assertEquals("Mysuru", top.getHotel().getDistrict());
        assertTrue(top.getMatchScore() >= 85);
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("District matches")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Suitable for 3 guests")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Pool available")));
        assertTrue(top.getMatchReasons().stream().anyMatch(r -> r.contains("Spa available")));
    }

    @Test
    @DisplayName("Multi-field Comparator - Correct sorting: Score desc, Price asc, Name asc")
    void testMultiFieldComparator() {
        Hotel h1 = new Hotel();
        h1.setId(1L);
        h1.setName("Hotel Alpha");
        h1.setStartingPricePerNight(10000);

        Hotel h2 = new Hotel();
        h2.setId(2L);
        h2.setName("Hotel Beta");
        h2.setStartingPricePerNight(8000);

        Hotel h3 = new Hotel();
        h3.setId(3L);
        h3.setName("Hotel Gamma");
        h3.setStartingPricePerNight(8000);

        // h1 has 95%, h2 has 90%, h3 has 90%
        HotelMatchResult res1 = new HotelMatchResult(h1, 95, 10000, null, List.of("✓ Reason"), List.of());
        HotelMatchResult res2 = new HotelMatchResult(h2, 90, 8000, null, List.of("✓ Reason"), List.of());
        HotelMatchResult res3 = new HotelMatchResult(h3, 90, 8000, null, List.of("✓ Reason"), List.of());

        List<HotelMatchResult> list = Arrays.asList(res2, res1, res3);
        list.sort(new HotelPreferenceComparator());

        // 1st: res1 (highest score 95)
        assertEquals(1L, list.get(0).getHotel().getId());
        // 2nd: res2 (score 90, price 8000, name Hotel Beta comes before Hotel Gamma)
        assertEquals(2L, list.get(1).getHotel().getId());
        // 3rd: res3 (score 90, price 8000, Hotel Gamma)
        assertEquals(3L, list.get(2).getHotel().getId());
    }

    @Test
    @DisplayName("Room-Level Matching - Room entity matching supports DELUXE, SUITE, PRESIDENTIAL")
    void testRoomLevelMatchingForRoomEntities() {
        List<Room> allRooms = roomRepository.findAll();
        assertFalse(allRooms.isEmpty(), "Baseline rooms should be present in database");

        // Criteria for 4 guests
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .guests(4)
                .budget("0-10000")
                .build();

        List<RoomMatchResult> roomMatches = preferenceMatcher.matchAndRankRooms(allRooms, criteria);
        assertFalse(roomMatches.isEmpty());

        // Top matched room should accommodate 4 guests and be within budget
        RoomMatchResult topRoom = roomMatches.get(0);
        assertTrue(topRoom.isSuitableForOccupancy());
        assertTrue(topRoom.getMaxOccupancy() >= 4);
    }

    @Test
    @DisplayName("REST API - /api/hotels/filter returns matchScore, reasons, and recommended room")
    void testApiHotelsFilterReturnsMatchDetails() throws Exception {
        mockMvc.perform(get("/api/hotels/filter")
                        .param("state", "Karnataka")
                        .param("district", "Mysuru")
                        .param("facilities", "Swimming Pool")
                        .param("facilities", "Spa")
                        .param("guests", "2"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.hotels[0].matchScore", greaterThanOrEqualTo(85)))
                .andExpect(jsonPath("$.hotels[0].matchScoreFormatted", containsString("% Match")))
                .andExpect(jsonPath("$.hotels[0].matchTier", notNullValue()))
                .andExpect(jsonPath("$.hotels[0].recommendedRoom", notNullValue()))
                .andExpect(jsonPath("$.hotels[0].matchReasons", hasItem(containsString("District matches"))))
                .andExpect(jsonPath("$.hotels[0].matchReasons", hasItem(containsString("Pool available"))));
    }
}
