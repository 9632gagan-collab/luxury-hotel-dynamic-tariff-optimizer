package com.hotel.controller;

import com.hotel.matcher.HotelMatchResult;
import com.hotel.matcher.PreferenceCriteria;
import com.hotel.matcher.PreferenceMatcher;
import com.hotel.model.Hotel;
import com.hotel.service.HotelService;
import com.hotel.service.PlannedHotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.*;

/**
 * Controller handling public and guest discovery for verified luxury hotels across India,
 * hierarchical location browsing (State -> District -> City -> Hotels),
 * advanced multi-select filter criteria, dedicated India Map exploration, property details,
 * and Stage 2 Smart Preference Matching.
 */
@Controller
public class HotelController {

    private final HotelService hotelService;
    private final PlannedHotelService plannedHotelService;
    private final PreferenceMatcher preferenceMatcher;

    @Autowired
    public HotelController(HotelService hotelService,
                           PlannedHotelService plannedHotelService,
                           PreferenceMatcher preferenceMatcher) {
        this.hotelService = hotelService;
        this.plannedHotelService = plannedHotelService;
        this.preferenceMatcher = preferenceMatcher;
    }

    /**
     * Dedicated "Find Hotels" Search & Discovery Page.
     * Supports multi-tier location filtering, experience tags, multi-select facilities,
     * stay dates, guest counts, and occupancy configuration.
     */
    @GetMapping("/hotels")
    public String listHotels(
            @RequestParam(required = false) Long hotelId,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) List<String> facilities,
            @RequestParam(required = false) String facility, // backward compatibility with single-param links
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            Model model,
            Principal principal) {

        // Normalize facilities list from both multi-select checkboxes and single legacy param
        List<String> activeFacilities = new ArrayList<>();
        if (facilities != null) {
            activeFacilities.addAll(facilities);
        }
        if (facility != null && !facility.isBlank() && !activeFacilities.contains(facility.trim())) {
            activeFacilities.add(facility.trim());
        }

        // Support direct selection of a planned hotel by stored hotel ID
        Hotel selectedPlannedHotel = null;
        if (hotelId != null) {
            Optional<Hotel> hOpt = hotelService.getHotelById(hotelId);
            if (hOpt.isPresent()) {
                selectedPlannedHotel = hOpt.get();
                if (state == null || state.isBlank() || "All India".equalsIgnoreCase(state.trim())) {
                    state = selectedPlannedHotel.getState();
                }
                if (district == null || district.isBlank()) {
                    district = selectedPlannedHotel.getDistrict();
                }
            }
        }

        // Auto-resolve state if district is provided without state
        String activeState = state;
        if ((activeState == null || activeState.isBlank() || "All India".equalsIgnoreCase(activeState.trim()))
                && (district != null && !district.isBlank()
                    && !"All Districts".equalsIgnoreCase(district.trim())
                    && !"All Karnataka".equalsIgnoreCase(district.trim()))) {
            String inferred = hotelService.getStateForDistrict(district.trim());
            if (inferred != null) {
                activeState = inferred;
            }
        }

        // Apply advanced multi-criteria filter including budget and dates
        List<Hotel> filteredHotels = hotelService.filterHotelsAdvanced(
                activeState, district, experience, activeFacilities, guests, roomType,
                budget, minPrice, maxPrice, checkIn, checkOut
        );

        if (selectedPlannedHotel != null && !filteredHotels.contains(selectedPlannedHotel)) {
            filteredHotels.add(0, selectedPlannedHotel);
        }

        // Stage 2 Smart Preference Matching & Multi-Field Ranking
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state(activeState)
                .district(district)
                .experience(experience)
                .facilities(activeFacilities)
                .guests(guests)
                .roomType(roomType)
                .budget(budget)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .build();

        List<HotelMatchResult> matchResults = preferenceMatcher.matchAndRankHotels(filteredHotels, criteria);
        Map<Long, HotelMatchResult> hotelMatchMap = new LinkedHashMap<>();
        List<Hotel> rankedHotels = new ArrayList<>();
        for (HotelMatchResult match : matchResults) {
            hotelMatchMap.put(match.getHotel().getId(), match);
            rankedHotels.add(match.getHotel());
        }

        // Ensure selected planned hotel is prioritized at the top of results
        if (selectedPlannedHotel != null) {
            rankedHotels.remove(selectedPlannedHotel);
            rankedHotels.add(0, selectedPlannedHotel);
            model.addAttribute("selectedHotelId", hotelId);
            model.addAttribute("selectedPlannedHotel", selectedPlannedHotel);
        }

        model.addAttribute("hotels", rankedHotels);
        model.addAttribute("hotelMatches", matchResults);
        model.addAttribute("hotelMatchMap", hotelMatchMap);
        model.addAttribute("allStates", hotelService.getAllStates());
        model.addAttribute("stateDistrictMap", hotelService.getStateDistrictMap());
        model.addAttribute("experienceCategories", hotelService.getExperienceCategories());
        model.addAttribute("facilityCategories", hotelService.getFacilityCategories());

        // Location selection attributes
        model.addAttribute("selectedState", (activeState != null && !activeState.isBlank()) ? activeState.trim() : "All India");
        model.addAttribute("selectedDistrict", (district != null && !district.isBlank()) ? district.trim() : "");
        model.addAttribute("selectedExperience", (experience != null) ? experience.trim() : "");
        model.addAttribute("selectedFacilities", activeFacilities);
        model.addAttribute("selectedGuests", (guests != null) ? guests : 2);
        model.addAttribute("selectedRoomType", (roomType != null && !roomType.isBlank()) ? roomType.trim() : "Private / Single Room");
        model.addAttribute("selectedBudget", (budget != null && !budget.isBlank()) ? budget.trim() : "");
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("checkIn", checkIn);
        model.addAttribute("checkOut", checkOut);
        model.addAttribute("totalCount", filteredHotels.size());

        // Available districts strictly for the currently selected state
        List<String> availableDistricts = hotelService.getDistrictsByState(activeState);
        model.addAttribute("availableDistricts", availableDistricts);
        model.addAttribute("allDistricts", hotelService.getAllDistricts());

        String username = principal != null ? principal.getName() : null;
        model.addAttribute("plannedHotelIds", plannedHotelService.getPlannedHotelIdsForGuest(username));

        return "guest/hotels";
    }

    /**
     * Dedicated "Explore India on Map" Dashboard.
     * Features an interactive India map, hover telemetry, state hotel counters,
     * district pickers, and spatial stay discovery.
     */
    @GetMapping("/map")
    public String exploreIndiaOnMap(
            @RequestParam(required = false) String state,
            Model model,
            Principal principal) {
        model.addAttribute("pageTitle", "Explore India on Map - Luxury Hotel Dashboard");
        model.addAttribute("allStates", hotelService.getAllStates());
        model.addAttribute("stateHotelCounts", hotelService.getHotelCountByState());
        model.addAttribute("stateDistrictMap", hotelService.getStateDistrictMap());

        String activeState = (state != null && !state.isBlank()) ? state.trim() : "Karnataka";
        model.addAttribute("activeState", activeState);

        List<Hotel> stateHotels = hotelService.filterHotelsAdvanced(activeState, null, null, null, null, null);
        model.addAttribute("stateHotels", stateHotels);
        model.addAttribute("allHotels", hotelService.getAllHotels());

        String username = principal != null ? principal.getName() : null;
        Set<Long> plannedHotelIds = plannedHotelService.getPlannedHotelIds(username);
        model.addAttribute("plannedHotelIds", plannedHotelIds);

        return "guest/india-map";
    }

    /**
     * Hotel Details Page with verified brand, physical address, direct contacts,
     * official source link, and Google Maps navigation.
     */
    @GetMapping("/hotels/{id}")
    public String viewHotelDetails(@PathVariable Long id,
                                   @RequestParam(required = false) String roomType,
                                   Model model,
                                   Principal principal) {
        Optional<Hotel> hotelOpt = hotelService.getHotelById(id);
        if (hotelOpt.isEmpty()) {
            return "redirect:/hotels";
        }

        Hotel hotel = hotelOpt.get();
        model.addAttribute("hotel", hotel);
        if (roomType != null && !roomType.isBlank()) {
            model.addAttribute("selectedRoomType", roomType.trim());
        }

        String username = principal != null ? principal.getName() : null;
        boolean isPlanned = plannedHotelService.isHotelPlannedByGuest(username, hotel.getId());
        model.addAttribute("isPlanned", isPlanned);

        Set<Long> plannedHotelIds = plannedHotelService.getPlannedHotelIds(username);
        model.addAttribute("plannedHotelIds", plannedHotelIds);

        // Fetch related hotels in the same district or state
        List<Hotel> relatedHotels = hotelService.filterHotelsAdvanced(hotel.getState(), hotel.getDistrict(), null, null, null, null)
                .stream()
                .filter(h -> !h.getId().equals(hotel.getId()))
                .limit(3)
                .toList();

        // If district has few other hotels, fall back to same state
        if (relatedHotels.isEmpty()) {
            relatedHotels = hotelService.filterHotelsAdvanced(hotel.getState(), null, null, null, null, null)
                    .stream()
                    .filter(h -> !h.getId().equals(hotel.getId()))
                    .limit(3)
                    .toList();
        }

        model.addAttribute("relatedHotels", relatedHotels);

        return "guest/hotel-details";
    }

    /**
     * REST endpoint returning verified districts for a selected State.
     * Enables instant dynamic dropdown update without full page reloads.
     */
    @GetMapping("/api/locations/districts")
    @ResponseBody
    public List<String> getDistrictsForState(@RequestParam String state) {
        return hotelService.getDistrictsByState(state);
    }

    /**
     * REST endpoint returning verified hotels for a selected State.
     */
    @GetMapping("/api/hotels/by-state")
    @ResponseBody
    public List<Map<String, Object>> getHotelsByState(@RequestParam String state) {
        List<Hotel> hotels = hotelService.filterHotelsAdvanced(state, null, null, null, null, null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Hotel h : hotels) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", h.getId());
            map.put("name", h.getName());
            map.put("brand", h.getBrand());
            map.put("district", h.getDistrict());
            map.put("city", h.getCity());
            map.put("state", h.getState());
            map.put("hotelType", h.getHotelType());
            map.put("rating", h.getRating());
            map.put("startingPrice", h.getStartingPricePerNight());
            map.put("imageUrl", h.getImageUrl());
            map.put("shortDescription", h.getShortDescription());
            map.put("address", h.getAddress());
            map.put("postalCode", h.getPostalCode());
            map.put("contactNumber", h.getContactNumber());
            map.put("officialWebsite", h.getOfficialWebsite());
            map.put("officialSourceUrl", h.getOfficialSourceUrl());
            map.put("googleMapsUrl", h.getGoogleMapsUrl());
            result.add(map);
        }
        return result;
    }

    /**
     * REST endpoint for real-time responsive filtering across all 8 criteria:
     * State, District, Experience, Multi-Select Facilities, Date, People, Room Type, Budget.
     * Prevents full page reloads and provides instant, seamless updates.
     */
    @GetMapping("/api/hotels/filter")
    @ResponseBody
    public Map<String, Object> filterHotelsApi(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String experience,
            @RequestParam(required = false) List<String> facilities,
            @RequestParam(required = false) String facility,
            @RequestParam(required = false) Integer guests,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) String checkIn,
            @RequestParam(required = false) String checkOut,
            Principal principal) {

        List<String> activeFacilities = new ArrayList<>();
        if (facilities != null) {
            activeFacilities.addAll(facilities);
        }
        if (facility != null && !facility.isBlank() && !activeFacilities.contains(facility.trim())) {
            activeFacilities.add(facility.trim());
        }

        // Auto-resolve state if district is provided without state
        String activeState = state;
        if ((activeState == null || activeState.isBlank() || "All India".equalsIgnoreCase(activeState.trim()))
                && (district != null && !district.isBlank()
                    && !"All Districts".equalsIgnoreCase(district.trim())
                    && !"All Karnataka".equalsIgnoreCase(district.trim()))) {
            String inferred = hotelService.getStateForDistrict(district.trim());
            if (inferred != null) {
                activeState = inferred;
            }
        }

        List<Hotel> filteredHotels = hotelService.filterHotelsAdvanced(
                activeState, district, experience, activeFacilities, guests, roomType,
                budget, minPrice, maxPrice, checkIn, checkOut
        );

        // Stage 2 Smart Preference Matching & Multi-Field Ranking
        PreferenceCriteria criteria = PreferenceCriteria.builder()
                .state(activeState)
                .district(district)
                .experience(experience)
                .facilities(activeFacilities)
                .guests(guests)
                .roomType(roomType)
                .budget(budget)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .checkIn(checkIn)
                .checkOut(checkOut)
                .build();

        List<HotelMatchResult> matchResults = preferenceMatcher.matchAndRankHotels(filteredHotels, criteria);

        String username = principal != null ? principal.getName() : null;
        Set<Long> plannedIds = plannedHotelService.getPlannedHotelIdsForGuest(username);

        List<Map<String, Object>> hotelMaps = new ArrayList<>();
        for (HotelMatchResult match : matchResults) {
            Hotel h = match.getHotel();
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", h.getId());
            map.put("name", h.getName());
            map.put("brand", h.getBrand() != null ? h.getBrand() : "Luxury Stay");
            map.put("state", h.getState());
            map.put("district", h.getDistrict());
            map.put("city", h.getCity() != null ? h.getCity() : h.getCityLocation());
            map.put("address", h.getAddress());
            map.put("postalCode", h.getPostalCode());
            map.put("hotelType", h.getHotelType());
            map.put("rating", h.getRating());
            map.put("startingPrice", h.getStartingPricePerNight());
            map.put("imageUrl", h.getImageUrl());
            map.put("shortDescription", h.getShortDescription());
            map.put("googleMapsUrl", h.getGoogleMapsUrl());
            map.put("experienceCategories", h.getExperienceCategories() != null ? h.getExperienceCategories() : Collections.emptyList());
            map.put("facilityCategories", h.getFacilityCategories() != null ? h.getFacilityCategories() : Collections.emptyList());
            map.put("isPlanned", plannedIds.contains(h.getId()));

            // Stage 2 Smart Matching attributes
            map.put("matchScore", match.getMatchScore());
            map.put("matchScoreFormatted", match.getMatchScoreFormatted());
            map.put("matchTier", match.getMatchTier());
            map.put("applicablePrice", match.getApplicablePrice());
            map.put("applicablePriceFormatted", match.getFormattedPrice());
            map.put("recommendedRoom", match.getRecommendedRoomName());
            map.put("matchReasons", match.getMatchReasons());
            map.put("missedCriteria", match.getMissedCriteria());

            hotelMaps.add(map);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalCount", matchResults.size());
        response.put("hotels", hotelMaps);
        response.put("selectedState", (activeState != null && !activeState.isBlank()) ? activeState.trim() : "All India");
        response.put("selectedDistrict", district != null ? district.trim() : "");
        response.put("selectedExperience", experience != null ? experience.trim() : "");
        response.put("selectedFacilities", activeFacilities);
        response.put("selectedGuests", guests != null ? guests : 2);
        response.put("selectedRoomType", roomType != null ? roomType.trim() : "Private / Single Room");
        response.put("selectedBudget", budget != null ? budget.trim() : "");
        response.put("checkIn", checkIn);
        response.put("checkOut", checkOut);
        response.put("availableDistricts", hotelService.getDistrictsByState(activeState));

        return response;
    }
}
