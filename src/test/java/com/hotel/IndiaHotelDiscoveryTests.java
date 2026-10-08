package com.hotel;

import com.hotel.model.District;
import com.hotel.model.Hotel;
import com.hotel.model.State;
import com.hotel.repository.CityRepository;
import com.hotel.repository.DistrictRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.StateRepository;
import com.hotel.service.HotelService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class IndiaHotelDiscoveryTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private CityRepository cityRepository;

    @Autowired
    private HotelService hotelService;

    @Test
    @DisplayName("India Dataset - Verify states, districts, cities, and hotels are populated across India")
    void testIndiaWideDatasetVerification() {
        List<Hotel> allHotels = hotelRepository.findAll();
        List<State> allStates = stateRepository.findAll();
        List<District> allDistricts = districtRepository.findAll();

        assertTrue(allHotels.size() >= 50, "Expected at least 50 hotels, found: " + allHotels.size());
        assertTrue(allStates.size() >= 12, "Expected at least 12 states/UTs, found: " + allStates.size());
        assertTrue(allDistricts.size() >= 30, "Expected at least 30 districts, found: " + allDistricts.size());

        // Check key states are represented
        Set<String> stateNames = new HashSet<>();
        for (State s : allStates) {
            stateNames.add(s.getName());
        }
        assertTrue(stateNames.contains("Karnataka"));
        assertTrue(stateNames.contains("Maharashtra"));
        assertTrue(stateNames.contains("Tamil Nadu"));
        assertTrue(stateNames.contains("Kerala"));
        assertTrue(stateNames.contains("Rajasthan"));
        assertTrue(stateNames.contains("Goa"));
        assertTrue(stateNames.contains("Delhi"));
        assertTrue(stateNames.contains("West Bengal"));
        assertTrue(stateNames.contains("Himachal Pradesh"));
        assertTrue(stateNames.contains("Uttarakhand"));
        assertTrue(stateNames.contains("Uttar Pradesh"));
    }

    @Test
    @DisplayName("Data Quality - No duplicates, no missing mandatory fields, valid official sources and addresses")
    void testDataQualityIntegrity() {
        List<Hotel> allHotels = hotelRepository.findAll();
        Set<String> seenNames = new HashSet<>();

        for (Hotel hotel : allHotels) {
            // Check uniqueness
            assertFalse(seenNames.contains(hotel.getName()), "Duplicate hotel detected: " + hotel.getName());
            seenNames.add(hotel.getName());

            // Check non-null / non-blank verified fields
            assertNotNull(hotel.getName(), "Hotel name must not be null");
            assertNotNull(hotel.getBrand(), "Hotel brand must not be null: " + hotel.getName());
            assertNotNull(hotel.getState(), "Hotel state must not be null: " + hotel.getName());
            assertNotNull(hotel.getDistrict(), "Hotel district must not be null: " + hotel.getName());
            assertNotNull(hotel.getCity(), "Hotel city must not be null: " + hotel.getName());
            assertNotNull(hotel.getAddress(), "Hotel address must not be null: " + hotel.getName());
            assertNotNull(hotel.getPostalCode(), "Hotel postal code must not be null: " + hotel.getName());
            assertNotNull(hotel.getPhone(), "Hotel phone must not be null: " + hotel.getName());
            assertNotNull(hotel.getOfficialSourceUrl(), "Official source URL must not be null: " + hotel.getName());

            assertFalse(hotel.getAddress().trim().isEmpty(), "Address must not be empty: " + hotel.getName());
            assertFalse(hotel.getPhone().trim().isEmpty(), "Phone must not be empty: " + hotel.getName());
            assertTrue(hotel.getOfficialSourceUrl().startsWith("http"), "Official source must be a valid HTTP(S) URL: " + hotel.getName());

            // Check Google Maps URL generation
            String gmapsUrl = hotel.getGoogleMapsUrl();
            assertNotNull(gmapsUrl);
            assertTrue(gmapsUrl.startsWith("https://www.google.com/maps/search/?api=1&query="));

            // Check phone format
            assertTrue(hotel.getPhone().startsWith("+91") || hotel.getPhone().startsWith("1800"),
                    "Phone number should be official Indian contact (+91 or toll-free 1800): " + hotel.getPhone());
        }
    }

    @Test
    @DisplayName("Home Page - Contains Discover Luxury Across India, Interactive Map, and Featured Experiences")
    void testHomePageRedesign() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Discover Luxury Across India")))
                .andExpect(content().string(containsString("Explore premium hotels, resorts and unique stays across India")))
                .andExpect(content().string(containsString("Find Hotels")))
                .andExpect(content().string(containsString("Explore India by Map")))
                .andExpect(content().string(containsString("Select a state to explore hotels and destinations.")))
                .andExpect(content().string(containsString("Featured Experiences")))
                .andExpect(content().string(containsString("City Luxury")))
                .andExpect(content().string(containsString("Beach Escapes")))
                .andExpect(content().string(containsString("Heritage Hotels")))
                .andExpect(content().string(containsString("Mountain Retreats")));
    }

    @Test
    @DisplayName("Find Hotels Page - Advanced Search Filter UI with Multi-Select Facilities and Occupancy")
    void testFindHotelsPageRendering() throws Exception {
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Find Luxury Hotels &amp; Resorts Across India")))
                .andExpect(content().string(containsString("Search Filters")))
                .andExpect(content().string(containsString("State / Union Territory")))
                .andExpect(content().string(containsString("District")))
                .andExpect(content().string(containsString("Experience")))
                .andExpect(content().string(containsString("Facilities (Multi-Select)")))
                .andExpect(content().string(containsString("Swimming Pool")))
                .andExpect(content().string(containsString("Spa")))
                .andExpect(content().string(containsString("Free Wi-Fi")))
                .andExpect(content().string(containsString("Check-in Date")))
                .andExpect(content().string(containsString("Check-out Date")))
                .andExpect(content().string(containsString("Guests")))
                .andExpect(content().string(containsString("Room Occupancy")))
                .andExpect(content().string(containsString("Apply Filters")))
                .andExpect(content().string(containsString("Clear All Filters")))
                .andExpect(content().string(containsString("hotels found")));
    }

    @Test
    @DisplayName("Filter by State - Filters correctly for Maharashtra")
    void testFilterByState() throws Exception {
        mockMvc.perform(get("/hotels").param("state", "Maharashtra"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Taj Mahal Palace, Mumbai")))
                .andExpect(content().string(not(containsString("The Leela Palace Bengaluru"))));
    }

    @Test
    @DisplayName("Filter by District - Filters correctly for Mumbai City")
    void testFilterByDistrict() throws Exception {
        mockMvc.perform(get("/hotels")
                        .param("state", "Maharashtra")
                        .param("district", "Mumbai City"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Taj Mahal Palace, Mumbai")))
                .andExpect(content().string(containsString("The Oberoi, Mumbai")))
                .andExpect(content().string(not(containsString("The Ritz-Carlton, Pune"))));
    }

    @Test
    @DisplayName("Filter by Experience - Filters correctly for Heritage")
    void testFilterByExperience() throws Exception {
        mockMvc.perform(get("/hotels").param("experience", "Heritage"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Taj Lake Palace")))
                .andExpect(content().string(not(containsString("ITC Grand Chola"))));
    }

    @Test
    @DisplayName("Filter by Multi-Select Facilities - Filters hotels having multiple chosen facilities")
    void testFilterByMultipleFacilities() {
        List<String> requiredFacilities = Arrays.asList("Swimming Pool", "Spa", "Free Wi-Fi");
        List<Hotel> filtered = hotelService.filterHotelsAdvanced("Rajasthan", null, null, requiredFacilities, null, null);

        assertFalse(filtered.isEmpty());
        for (Hotel hotel : filtered) {
            assertEquals("Rajasthan", hotel.getState());
            List<String> facilities = hotel.getFacilityCategories();
            assertTrue(facilities.contains("Swimming Pool"));
            assertTrue(facilities.contains("Spa"));
            assertTrue(facilities.contains("Free Wi-Fi"));
        }
    }

    @Test
    @DisplayName("Hotel Details Page - Displays verified address, official source link, and Google Maps button")
    void testHotelDetailsPageVerifiedInformation() throws Exception {
        Hotel tajMumbai = hotelRepository.findAll().stream()
                .filter(h -> h.getName().contains("Taj Mahal Palace"))
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/hotels/" + tajMumbai.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The Taj Mahal Palace, Mumbai")))
                .andExpect(content().string(containsString("Apollo Bunder, Colaba")))
                .andExpect(content().string(containsString("Mumbai City")))
                .andExpect(content().string(containsString("Maharashtra")))
                .andExpect(content().string(containsString("Official Source")))
                .andExpect(content().string(containsString("View on Google Maps")))
                .andExpect(content().string(containsString("https://www.google.com/maps/search/?api=1&amp;query=")))
                .andExpect(content().string(containsString("https://www.tajhotels.com/en-in/taj/taj-mahal-palace-mumbai/")))
                .andExpect(content().string(containsString("+91 22 6665 3366")));
    }

    @Test
    @DisplayName("Map Dashboard - Explore India on Map page renders interactive map")
    void testMapDashboardRendering() throws Exception {
        mockMvc.perform(get("/map"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Explore India on Map")))
                .andExpect(content().string(containsString("Interactive spatial exploration of verified luxury hotels")))
                .andExpect(content().string(containsString("india-map-wrapper")))
                .andExpect(content().string(containsString("state-detail-panel")));
    }

    @Test
    @DisplayName("REST API - Get districts for state returns JSON list of districts")
    void testGetDistrictsForStateApi() throws Exception {
        mockMvc.perform(get("/api/locations/districts").param("state", "Karnataka"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasItem("Bengaluru Urban")))
                .andExpect(jsonPath("$", hasItem("Kodagu")))
                .andExpect(jsonPath("$", hasItem("Mysuru")))
                .andExpect(jsonPath("$", hasItem("Chikkamagaluru")));
    }

    @Test
    @DisplayName("REST API - Get hotels for state returns verified hotel JSON with maps and source URL")
    void testGetHotelsByStateApi() throws Exception {
        mockMvc.perform(get("/api/hotels/by-state").param("state", "Goa"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name", notNullValue()))
                .andExpect(jsonPath("$[0].state", is("Goa")))
                .andExpect(jsonPath("$[0].district", notNullValue()))
                .andExpect(jsonPath("$[0].officialSourceUrl", notNullValue()))
                .andExpect(jsonPath("$[0].googleMapsUrl", notNullValue()));
    }

    @Test
    @DisplayName("REST API - /api/hotels/filter supports intelligent 8-filter combinations and budget ranges")
    void testApiHotelsFilterIntelligentCombination() throws Exception {
        // 1. Single State filter
        mockMvc.perform(get("/api/hotels/filter").param("state", "Karnataka"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.selectedState", is("Karnataka")))
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(5)))
                .andExpect(jsonPath("$.availableDistricts", hasItem("Mysuru")));

        // 2. State + District
        mockMvc.perform(get("/api/hotels/filter")
                        .param("state", "Karnataka")
                        .param("district", "Mysuru"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedDistrict", is("Mysuru")))
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(2)));

        // 3. Multi-facility selection
        mockMvc.perform(get("/api/hotels/filter")
                        .param("state", "Karnataka")
                        .param("district", "Mysuru")
                        .param("facilities", "Swimming Pool")
                        .param("facilities", "Spa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedFacilities", hasItems("Swimming Pool", "Spa")))
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(1)));

        // 4. Budget filter (Under ₹10,000 matches Mysuru hotels such as Radisson Blu at ₹7,500 and Lalitha Mahal at ₹8,400)
        mockMvc.perform(get("/api/hotels/filter")
                        .param("state", "Karnataka")
                        .param("district", "Mysuru")
                        .param("budget", "0-10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedBudget", is("0-10000")))
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.hotels[*].name", hasItem(containsString("Radisson Blu"))));

        // 5. Combinable 8-Filter requirement test (State + District + Experience + Facilities + Date + People + Room Type + Budget)
        mockMvc.perform(get("/api/hotels/filter")
                        .param("state", "Karnataka")
                        .param("district", "Mysuru")
                        .param("experience", "Heritage")
                        .param("facilities", "Swimming Pool")
                        .param("facilities", "Spa")
                        .param("guests", "3")
                        .param("roomType", "Private")
                        .param("budget", "0-10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.hotels[*].name", hasItem(containsString("Radisson Blu"))));
    }

    @Test
    @DisplayName("HTML View - /hotels page includes 8 category filters including Budget and valid district population")
    void testHotelsPageHasBudgetAndCategoryPills() throws Exception {
        mockMvc.perform(get("/hotels"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"pill_state\"")))
                .andExpect(content().string(containsString("id=\"pill_district\"")))
                .andExpect(content().string(containsString("id=\"pill_experience\"")))
                .andExpect(content().string(containsString("id=\"pill_facilities\"")))
                .andExpect(content().string(containsString("id=\"pill_date\"")))
                .andExpect(content().string(containsString("id=\"pill_people\"")))
                .andExpect(content().string(containsString("id=\"pill_roomType\"")))
                .andExpect(content().string(containsString("id=\"pill_budget\"")))
                .andExpect(content().string(containsString("id=\"panel_budget\"")))
                .andExpect(content().string(containsString("Under ₹10,000")))
                .andExpect(content().string(containsString("₹10,000 – ₹20,000")))
                .andExpect(content().string(containsString("₹20,000 – ₹35,000")))
                .andExpect(content().string(containsString("Above ₹35,000")));
    }
}

