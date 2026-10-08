package com.hotel;

import com.hotel.model.District;
import com.hotel.model.Hotel;
import com.hotel.model.State;
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

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class Stage4IndiaHierarchyTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private StateRepository stateRepository;

    @Autowired
    private DistrictRepository districtRepository;

    @Autowired
    private HotelService hotelService;

    @Test
    @DisplayName("Stage 4 - Hierarchy: Every hotel has a valid State and District")
    void testHotelHierarchyIntegrity() {
        List<Hotel> hotels = hotelRepository.findAll();
        assertFalse(hotels.isEmpty(), "Hotels should not be empty");

        for (Hotel hotel : hotels) {
            assertNotNull(hotel.getState(), "Hotel " + hotel.getName() + " must have a State");
            assertFalse(hotel.getState().isBlank(), "Hotel " + hotel.getName() + " State cannot be blank");
            assertNotNull(hotel.getDistrict(), "Hotel " + hotel.getName() + " must have a District");
            assertFalse(hotel.getDistrict().isBlank(), "Hotel " + hotel.getName() + " District cannot be blank");
            assertNotNull(hotel.getGoogleMapsUrl(), "Hotel " + hotel.getName() + " must have a Google Maps URL");
            assertTrue(hotel.getGoogleMapsUrl().startsWith("https://www.google.com/maps"),
                    "Hotel " + hotel.getName() + " Maps URL should start with https://www.google.com/maps");
        }
    }

    @Test
    @DisplayName("Stage 4 - Karnataka -> Bengaluru Urban -> Only Bengaluru hotels")
    void testKarnatakaBengaluruUrbanHierarchy() {
        List<Hotel> hotels = hotelService.getHotelsByDistrict("Karnataka", "Bengaluru Urban");
        assertFalse(hotels.isEmpty(), "Bengaluru Urban should have hotels");

        for (Hotel h : hotels) {
            assertEquals("Karnataka", h.getState(), "Hotel " + h.getName() + " should be in Karnataka");
            assertEquals("Bengaluru Urban", h.getDistrict(), "Hotel " + h.getName() + " should be in Bengaluru Urban");
            assertNotEquals("Mysuru", h.getDistrict(), "Bengaluru hotels should not have district Mysuru");
            assertNotEquals("Maharashtra", h.getState(), "Bengaluru hotels should not have state Maharashtra");
        }

        assertTrue(hotels.stream().anyMatch(h -> h.getName().contains("The Leela Palace Bengaluru")));
        assertTrue(hotels.stream().anyMatch(h -> h.getName().contains("Taj West End")));
    }

    @Test
    @DisplayName("Stage 4 - Karnataka -> Mysuru -> Lalitha Mahal Palace Hotel & Radisson Blu Plaza Hotel Mysore")
    void testKarnatakaMysuruHierarchy() {
        List<Hotel> hotels = hotelService.getHotelsByDistrict("Karnataka", "Mysuru");
        assertFalse(hotels.isEmpty(), "Mysuru should have hotels");

        boolean hasLalithaMahal = hotels.stream().anyMatch(h -> h.getName().equalsIgnoreCase("Lalitha Mahal Palace Hotel"));
        boolean hasRadissonBlu = hotels.stream().anyMatch(h -> h.getName().contains("Radisson Blu Plaza Hotel"));

        assertTrue(hasLalithaMahal, "Mysuru hotels must include Lalitha Mahal Palace Hotel from uploaded PDF directory");
        assertTrue(hasRadissonBlu, "Mysuru hotels must include Radisson Blu Plaza Hotel Mysore");

        for (Hotel h : hotels) {
            assertEquals("Karnataka", h.getState());
            assertEquals("Mysuru", h.getDistrict());
            assertNotEquals("Bengaluru Urban", h.getDistrict());
        }
    }

    @Test
    @DisplayName("Stage 4 - Maharashtra -> Mumbai City -> Corresponding Mumbai hotels")
    void testMaharashtraMumbaiCityHierarchy() {
        List<Hotel> hotels = hotelService.getHotelsByDistrict("Maharashtra", "Mumbai City");
        assertFalse(hotels.isEmpty(), "Mumbai City should have hotels");

        for (Hotel h : hotels) {
            assertEquals("Maharashtra", h.getState());
            assertEquals("Mumbai City", h.getDistrict());
            assertNotEquals("Karnataka", h.getState());
        }

        assertTrue(hotels.stream().anyMatch(h -> h.getName().contains("The Taj Mahal Palace")));
    }

    @Test
    @DisplayName("Stage 4 - Goa -> North Goa -> Corresponding North Goa hotels")
    void testGoaNorthGoaHierarchy() {
        List<Hotel> hotels = hotelService.getHotelsByDistrict("Goa", "North Goa");
        assertFalse(hotels.isEmpty(), "North Goa should have hotels");

        for (Hotel h : hotels) {
            assertEquals("Goa", h.getState());
            assertEquals("North Goa", h.getDistrict());
            assertNotEquals("South Goa", h.getDistrict());
            assertNotEquals("Karnataka", h.getState());
        }

        assertTrue(hotels.stream().anyMatch(h -> h.getName().contains("W Goa") || h.getName().contains("Taj Fort Aguada")));
    }

    @Test
    @DisplayName("Stage 4 - Rajasthan -> Jaipur -> Corresponding Jaipur hotels")
    void testRajasthanJaipurHierarchy() {
        List<Hotel> hotels = hotelService.getHotelsByDistrict("Rajasthan", "Jaipur");
        assertFalse(hotels.isEmpty(), "Jaipur should have hotels");

        for (Hotel h : hotels) {
            assertEquals("Rajasthan", h.getState());
            assertEquals("Jaipur", h.getDistrict());
            assertNotEquals("Udaipur", h.getDistrict());
            assertNotEquals("Karnataka", h.getState());
        }

        assertTrue(hotels.stream().anyMatch(h -> h.getName().contains("Rambagh Palace")));
    }

    @Test
    @DisplayName("Stage 4 - Strict Isolation: State A selected -> State B hotels must NOT appear")
    void testStateIsolation() {
        List<Hotel> karnatakaHotels = hotelService.getHotelsByState("Karnataka");
        assertFalse(karnatakaHotels.isEmpty());

        for (Hotel h : karnatakaHotels) {
            assertEquals("Karnataka", h.getState());
            assertNotEquals("Maharashtra", h.getState());
            assertNotEquals("Goa", h.getState());
            assertNotEquals("Rajasthan", h.getState());
            assertNotEquals("Delhi", h.getState());
        }
    }

    @Test
    @DisplayName("Stage 4 - Strict Isolation: District A selected -> District B hotels must NOT appear")
    void testDistrictIsolation() {
        List<Hotel> mysuruHotels = hotelService.getHotelsByDistrict("Karnataka", "Mysuru");
        assertFalse(mysuruHotels.isEmpty());

        for (Hotel h : mysuruHotels) {
            assertEquals("Mysuru", h.getDistrict());
            assertNotEquals("Bengaluru Urban", h.getDistrict());
            assertNotEquals("Kodagu", h.getDistrict());
            assertNotEquals("Mumbai City", h.getDistrict());
        }
    }

    @Test
    @DisplayName("Stage 4 - State Inference when only District is provided")
    void testStateInferenceFromDistrict() {
        String stateMysuru = hotelService.getStateForDistrict("Mysuru");
        assertEquals("Karnataka", stateMysuru);

        String stateMumbai = hotelService.getStateForDistrict("Mumbai City");
        assertEquals("Maharashtra", stateMumbai);

        String stateJaipur = hotelService.getStateForDistrict("Jaipur");
        assertEquals("Rajasthan", stateJaipur);

        String stateNorthGoa = hotelService.getStateForDistrict("North Goa");
        assertEquals("Goa", stateNorthGoa);
    }

    @Test
    @DisplayName("Stage 4 - API Filter endpoint: Mysuru returns Karnataka and Mysuru hotels")
    void testApiFilterEndpointMysuru() throws Exception {
        mockMvc.perform(get("/api/hotels/filter")
                .param("district", "Mysuru")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedState", is("Karnataka")))
                .andExpect(jsonPath("$.selectedDistrict", is("Mysuru")))
                .andExpect(jsonPath("$.hotels", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.hotels[*].name", hasItem(containsString("Lalitha Mahal Palace Hotel"))))
                .andExpect(jsonPath("$.hotels[*].name", hasItem(containsString("Radisson Blu Plaza Hotel"))))
                .andExpect(jsonPath("$.hotels[*].state", everyItem(is("Karnataka"))))
                .andExpect(jsonPath("$.hotels[*].district", everyItem(is("Mysuru"))));
    }

    @Test
    @DisplayName("Stage 4 - API Filter endpoint: State A cannot return State B hotels")
    void testApiFilterStateIsolation() throws Exception {
        mockMvc.perform(get("/api/hotels/filter")
                .param("state", "Goa")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.selectedState", is("Goa")))
                .andExpect(jsonPath("$.hotels[*].state", everyItem(is("Goa"))));
    }

    @Test
    @DisplayName("Stage 4 - Web Controller /hotels with state and district preserves hierarchy")
    void testWebControllerHotelsHierarchy() throws Exception {
        mockMvc.perform(get("/hotels")
                .param("state", "Karnataka")
                .param("district", "Mysuru"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("selectedState", is("Karnataka")))
                .andExpect(model().attribute("selectedDistrict", is("Mysuru")))
                .andExpect(model().attribute("hotels", hasItem(hasProperty("name", is("Lalitha Mahal Palace Hotel")))));
    }

    @Test
    @DisplayName("Stage 4 - Hotel Details page displays Google Maps URL & breadcrumb hierarchy")
    void testHotelDetailsBreadcrumbsAndMaps() throws Exception {
        Optional<Hotel> lalithaOpt = hotelRepository.findByNameIgnoreCase("Lalitha Mahal Palace Hotel");
        assertTrue(lalithaOpt.isPresent(), "Lalitha Mahal Palace Hotel must be present");
        Hotel lalitha = lalithaOpt.get();

        mockMvc.perform(get("/hotels/" + lalitha.getId()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Lalitha Mahal Palace Hotel")))
                .andExpect(content().string(containsString("Karnataka")))
                .andExpect(content().string(containsString("google.com/maps/search/?api=1")))
                .andExpect(content().string(containsString("Lalitha+Mahal+Palace+Hotel")))
                .andExpect(content().string(containsString("View on Google Maps")));
    }
}
