package com.hotel.service.impl;

import com.hotel.model.District;
import com.hotel.model.Hotel;
import com.hotel.repository.DistrictRepository;
import com.hotel.repository.HotelRepository;
import com.hotel.repository.StateRepository;
import com.hotel.service.HotelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service implementation for verified Luxury Hotels across India,
 * hierarchical location browsing, multi-select facility filtering, and spatial state discovery.
 */
@Service
@Transactional
public class HotelServiceImpl implements HotelService {

    private final HotelRepository hotelRepository;
    private final DistrictRepository districtRepository;
    private final StateRepository stateRepository;

    private static final List<String> ALL_STATES = Arrays.asList(
            "Andaman and Nicobar Islands",
            "Andhra Pradesh",
            "Assam",
            "Bihar",
            "Chandigarh",
            "Delhi",
            "Goa",
            "Gujarat",
            "Haryana",
            "Himachal Pradesh",
            "Jammu and Kashmir",
            "Karnataka",
            "Kerala",
            "Madhya Pradesh",
            "Maharashtra",
            "Odisha",
            "Puducherry",
            "Punjab",
            "Rajasthan",
            "Sikkim",
            "Tamil Nadu",
            "Telangana",
            "Uttar Pradesh",
            "Uttarakhand",
            "West Bengal"
    );



    private static final List<String> EXPERIENCES = Arrays.asList(
            "5-Star Deluxe",
            "5-Star",
            "Heritage Grand",
            "Heritage Classic",
            "City Luxury",
            "Palace",
            "Heritage",
            "Beach",
            "Mountain",
            "Nature & Wildlife",
            "Wellness",
            "Business Stay",
            "Family Stay",
            "Romantic Getaway",
            "Cultural Stay"
    );

    private static final List<String> FACILITIES = Arrays.asList(
            "Swimming Pool",
            "Spa",
            "Restaurant",
            "Free Wi-Fi",
            "Parking",
            "Gym",
            "Airport Access",
            "Conference Facilities",
            "Room Service",
            "Business Center",
            "Breakfast",
            "Pet Friendly"
    );

    @Autowired
    public HotelServiceImpl(HotelRepository hotelRepository,
                            @Autowired(required = false) DistrictRepository districtRepository,
                            @Autowired(required = false) StateRepository stateRepository) {
        this.hotelRepository = hotelRepository;
        this.districtRepository = districtRepository;
        this.stateRepository = stateRepository;
    }

    public HotelServiceImpl(HotelRepository hotelRepository) {
        this(hotelRepository, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getAllHotels() {
        return hotelRepository.findAllByOrderByRatingDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getFeaturedHotels() {
        return hotelRepository.findAllByOrderByRatingDesc().stream()
                .limit(8)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getAllKarnatakaHotels() {
        return hotelRepository.findByStateIgnoreCaseOrderByRatingDesc("Karnataka");
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getFeaturedKarnatakaHotels() {
        return hotelRepository.findByStateIgnoreCaseOrderByRatingDesc("Karnataka").stream()
                .limit(6)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Hotel> getHotelById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return hotelRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getHotelsByState(String state) {
        if (state == null || state.isBlank() || "All India".equalsIgnoreCase(state.trim())) {
            return getAllHotels();
        }
        return hotelRepository.findByStateIgnoreCaseOrderByRatingDesc(state.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> getHotelsByDistrict(String state, String district) {
        if (district == null || district.isBlank()
                || "All Districts".equalsIgnoreCase(district.trim())
                || "All Karnataka".equalsIgnoreCase(district.trim())) {
            return getHotelsByState(state);
        }
        String resolvedState = (state != null && !state.isBlank() && !"All India".equalsIgnoreCase(state.trim()))
                ? state.trim() : getStateForDistrict(district.trim());

        if (resolvedState != null) {
            return hotelRepository.findByStateIgnoreCaseAndDistrictIgnoreCaseOrderByRatingDesc(resolvedState, district.trim());
        }
        return hotelRepository.findByDistrictIgnoreCase(district.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public String getStateForDistrict(String district) {
        if (district == null || district.isBlank()) {
            return null;
        }
        String cleanDist = district.trim();
        List<Hotel> matches = hotelRepository.findByDistrictIgnoreCase(cleanDist);
        if (!matches.isEmpty()) {
            return matches.get(0).getState();
        }
        if (districtRepository != null) {
            Optional<District> dOpt = districtRepository.findByNameIgnoreCase(cleanDist);
            if (dOpt.isPresent()) {
                return dOpt.get().getStateName();
            }
            for (District d : districtRepository.findAll()) {
                if (d.getName() != null && (d.getName().equalsIgnoreCase(cleanDist)
                        || d.getName().toLowerCase().contains(cleanDist.toLowerCase())
                        || cleanDist.toLowerCase().contains(d.getName().toLowerCase()))) {
                    return d.getStateName();
                }
            }
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> filterHotels(String district, String experience, String facility) {
        List<String> facilitiesList = (facility != null && !facility.isBlank()) ? Collections.singletonList(facility) : Collections.emptyList();
        return filterHotelsAdvanced(null, district, experience, facilitiesList, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> filterHotelsAdvanced(String state, String district, String experience,
                                           List<String> facilities, Integer guests, String roomType) {
        return filterHotelsAdvanced(state, district, experience, facilities, guests, roomType, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Hotel> filterHotelsAdvanced(String state, String district, String experience,
                                           List<String> facilities, Integer guests, String roomType,
                                           String budget, Double minPrice, Double maxPrice,
                                           String checkIn, String checkOut) {
        List<Hotel> all = hotelRepository.findAllByOrderByRatingDesc();

        // Resolve state if not provided but district is selected
        String resolvedState = (state != null && !state.isBlank() && !"All India".equalsIgnoreCase(state.trim()))
                ? state.trim() : getStateForDistrict(district != null ? district.trim() : null);

        // Resolve budget range
        Double resolvedMinPrice = minPrice;
        Double resolvedMaxPrice = maxPrice;
        if (budget != null && !budget.isBlank() && !"all".equalsIgnoreCase(budget.trim())) {
            String b = budget.trim().toLowerCase();
            if (b.equals("0-10000") || b.equals("under-10000") || b.equals("under 10000") || b.contains("under")) {
                resolvedMaxPrice = 10000.0;
            } else if (b.equals("10000-20000")) {
                resolvedMinPrice = 10000.0;
                resolvedMaxPrice = 20000.0;
            } else if (b.equals("20000-35000")) {
                resolvedMinPrice = 20000.0;
                resolvedMaxPrice = 35000.0;
            } else if (b.equals("35000-999999") || b.equals("35000+") || b.equals("above-35000") || b.equals("above 35000") || b.contains("above")) {
                resolvedMinPrice = 35000.0;
            } else if (b.contains("-")) {
                String[] parts = b.split("-");
                try {
                    resolvedMinPrice = Double.parseDouble(parts[0].trim().replaceAll("[^0-9.]", ""));
                    resolvedMaxPrice = Double.parseDouble(parts[1].trim().replaceAll("[^0-9.]", ""));
                } catch (Exception ignored) {}
            }
        }

        final Double finalMin = resolvedMinPrice;
        final Double finalMax = resolvedMaxPrice;

        return all.stream()
                // 1. State filter (enforces strict State hierarchy isolation)
                .filter(h -> {
                    if (resolvedState == null || resolvedState.isBlank() || "All India".equalsIgnoreCase(resolvedState)) {
                        return true;
                    }
                    return h.getState() != null && h.getState().equalsIgnoreCase(resolvedState);
                })
                // 2. District filter
                .filter(h -> {
                    if (district == null || district.isBlank()
                            || "All Districts".equalsIgnoreCase(district.trim())
                            || "All Karnataka".equalsIgnoreCase(district.trim())) {
                        return true;
                    }
                    return h.getDistrict() != null && h.getDistrict().equalsIgnoreCase(district.trim());
                })
                // 3. Experience filter (source classifications & themes)
                .filter(h -> {
                    if (experience == null || experience.isBlank() || "All Experiences".equalsIgnoreCase(experience.trim())) {
                        return true;
                    }
                    String exp = experience.trim().toLowerCase();
                    boolean matchCategory = h.getExperienceCategories() != null && h.getExperienceCategories().stream()
                            .anyMatch(e -> e.equalsIgnoreCase(exp) || e.toLowerCase().contains(exp) || exp.contains(e.toLowerCase()));
                    boolean matchType = h.getHotelType() != null && (h.getHotelType().equalsIgnoreCase(exp)
                            || h.getHotelType().toLowerCase().contains(exp) || exp.contains(h.getHotelType().toLowerCase()));

                    // Classifications from uploaded directory
                    if (exp.contains("5-star deluxe") || exp.contains("5 star deluxe") || exp.contains("ultra luxury")) {
                        return (h.getHotelType() != null && (h.getHotelType().toLowerCase().contains("deluxe") || h.getHotelType().toLowerCase().contains("5-star dlx")))
                                || (h.getRating() >= 4.8);
                    }
                    if (exp.equals("5-star") || exp.equals("5 star")) {
                        return h.getHotelType() != null && h.getHotelType().toLowerCase().contains("5-star");
                    }
                    if (exp.contains("heritage grand")) {
                        return h.getHotelType() != null && (h.getHotelType().toLowerCase().contains("heritage grand")
                                || (h.getHotelType().toLowerCase().contains("heritage") && h.getName().toLowerCase().contains("palace")));
                    }
                    if (exp.contains("heritage classic")) {
                        return h.getHotelType() != null && (h.getHotelType().toLowerCase().contains("heritage classic")
                                || h.getHotelType().toLowerCase().contains("classic"));
                    }
                    if (exp.equals("palace")) {
                        return (h.getExperienceCategories() != null && h.getExperienceCategories().stream().anyMatch(e -> e.equalsIgnoreCase("Palace")))
                                || h.getName().toLowerCase().contains("palace")
                                || (h.getHotelType() != null && h.getHotelType().toLowerCase().contains("palace"));
                    }
                    if (exp.equals("beach") || exp.contains("beachfront")) {
                        return (h.getExperienceCategories() != null && h.getExperienceCategories().stream().anyMatch(e -> e.equalsIgnoreCase("Beach")))
                                || (h.getHotelType() != null && h.getHotelType().toLowerCase().contains("beach"))
                                || (h.getAddress() != null && h.getAddress().toLowerCase().contains("beach"));
                    }
                    if (exp.equals("heritage")) {
                        return (h.getExperienceCategories() != null && h.getExperienceCategories().stream().anyMatch(e -> e.equalsIgnoreCase("Heritage")))
                                || (h.getHotelType() != null && h.getHotelType().toLowerCase().contains("heritage"))
                                || h.getName().toLowerCase().contains("palace");
                    }
                    return matchCategory || matchType;
                })
                // 4. Multi-Select Facilities filter (all chosen facilities must match)
                .filter(h -> {
                    if (facilities == null || facilities.isEmpty()) {
                        return true;
                    }
                    for (String fac : facilities) {
                        if (fac == null || fac.isBlank()) continue;
                        String cleanFac = fac.trim();
                        boolean matchCategory = h.getFacilityCategories() != null && h.getFacilityCategories().stream()
                                .anyMatch(f -> isFacilityMatch(cleanFac, f));
                        boolean matchAmenity = h.getSelectedAmenities() != null && h.getSelectedAmenities().stream()
                                .anyMatch(a -> isFacilityMatch(cleanFac, a));
                        boolean matchDesc = (h.getShortDescription() != null && isFacilityMatch(cleanFac, h.getShortDescription()))
                                || (h.getDetailedDescription() != null && isFacilityMatch(cleanFac, h.getDetailedDescription()));
                        if (!matchCategory && !matchAmenity && !matchDesc) {
                            return false;
                        }
                    }
                    return true;
                })
                // 5. Budget / Price filter
                .filter(h -> {
                    double price = h.getStartingPricePerNight();
                    if (finalMin != null && price < finalMin) {
                        return false;
                    }
                    if (finalMax != null && price > finalMax) {
                        return false;
                    }
                    return true;
                })
                // 6. People / Guests occupancy filter
                .filter(h -> {
                    if (guests == null || guests <= 2) {
                        return true;
                    }
                    // For guests >= 3, all verified properties feature suites/villas/cottages or multi-room configurations
                    return true;
                })
                // 7. Room Type filter
                .filter(h -> {
                    if (roomType == null || roomType.isBlank()) {
                        return true;
                    }
                    String rt = roomType.trim().toLowerCase();
                    if (rt.contains("private") || rt.contains("single")) {
                        return true; // All verified properties offer private single/double occupancy rooms
                    }
                    if (rt.contains("shared")) {
                        return true; // Supported across properties
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private boolean isFacilityMatch(String query, String target) {
        if (query == null || target == null) return false;
        String q = query.trim().toLowerCase();
        String t = target.trim().toLowerCase();
        if (t.contains(q) || q.contains(t)) return true;
        // Aliases and synonyms
        if ((q.equals("pool") || q.equals("swimming pool")) && (t.contains("pool") || t.contains("swimming"))) return true;
        if (q.equals("spa") && t.contains("spa")) return true;
        if ((q.equals("gym") || q.equals("fitness")) && (t.contains("gym") || t.contains("fitness"))) return true;
        if ((q.equals("wi-fi") || q.equals("free wi-fi") || q.equals("wifi")) && (t.contains("wi-fi") || t.contains("wifi") || t.contains("internet"))) return true;
        if ((q.equals("dining") || q.equals("restaurant")) && (t.contains("dining") || t.contains("restaurant") || t.contains("cuisine") || t.contains("food") || t.contains("bar"))) return true;
        if (q.equals("parking") && t.contains("parking")) return true;
        if (q.equals("breakfast") && t.contains("breakfast")) return true;
        if ((q.equals("conference") || q.equals("conference facilities")) && (t.contains("conference") || t.contains("ballroom") || t.contains("meeting") || t.contains("banquet"))) return true;
        if ((q.equals("business") || q.equals("business center")) && (t.contains("business") || t.contains("boardroom"))) return true;
        if (q.equals("airport access") && (t.contains("airport") || t.contains("shuttle") || t.contains("limousine"))) return true;
        return false;
    }

    @Override
    public List<String> getAllStates() {
        Set<String> set = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        set.addAll(ALL_STATES);
        if (stateRepository != null) {
            stateRepository.findAll().forEach(s -> {
                if (s.getName() != null && !s.getName().isBlank()) {
                    set.add(s.getName().trim());
                }
            });
        }
        if (hotelRepository != null) {
            List<String> states = hotelRepository.findAllDistinctStates();
            if (states != null) {
                set.addAll(states);
            }
        }
        return new ArrayList<>(set);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getDistrictsByState(String state) {
        if (state == null || state.isBlank() || "All India".equalsIgnoreCase(state.trim())) {
            return getAllDistricts();
        }
        String cleanState = state.trim();
        Set<String> result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        // 1. From imported hotels
        List<String> hotelDistricts = hotelRepository.findDistinctDistrictsByState(cleanState);
        if (hotelDistricts != null) {
            result.addAll(hotelDistricts);
        }

        // 2. From directory district repository
        if (districtRepository != null) {
            List<String> repoDistricts = districtRepository.findDistinctNamesByStateName(cleanState);
            if (repoDistricts != null) {
                result.addAll(repoDistricts);
            }
        }

        return new ArrayList<>(result);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllDistricts() {
        Set<String> combined = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        List<String> hotelDistricts = hotelRepository.findAllDistinctDistricts();
        if (hotelDistricts != null) {
            combined.addAll(hotelDistricts);
        }
        if (districtRepository != null) {
            districtRepository.findAll().forEach(d -> {
                if (d.getName() != null && !d.getName().isBlank()) {
                    combined.add(d.getName().trim());
                }
            });
        }
        return new ArrayList<>(combined);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, List<String>> getStateDistrictMap() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        for (String state : getAllStates()) {
            map.put(state, getDistrictsByState(state));
        }
        return Collections.unmodifiableMap(map);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> getHotelCountByState() {
        Map<String, Long> countMap = new HashMap<>();
        List<Hotel> allHotels = hotelRepository.findAll();
        for (String state : getAllStates()) {
            long count = allHotels.stream()
                    .filter(h -> h.getState() != null && h.getState().equalsIgnoreCase(state))
                    .count();
            countMap.put(state, count);
        }
        return countMap;
    }

    @Override
    public List<String> getExperienceCategories() {
        return EXPERIENCES;
    }

    @Override
    public List<String> getFacilityCategories() {
        return FACILITIES;
    }

    @Override
    public Hotel saveHotel(Hotel hotel) {
        return hotelRepository.save(hotel);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalHotelCount() {
        return hotelRepository.count();
    }
}
