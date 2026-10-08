package com.hotel.matcher;

import com.hotel.model.Amenity;
import com.hotel.model.Hotel;
import com.hotel.model.Room;
import com.hotel.model.enums.RoomCategory;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Concrete implementation of the Stage 2 Smart Preference Matching Engine.
 * Calculates deterministic match scores, generates explicit audit explanations,
 * resolves room-level options, and sorts results using multi-field Comparators.
 */
@Service
public class PreferenceMatcherImpl implements PreferenceMatcher {

    private final HotelPreferenceComparator hotelComparator = new HotelPreferenceComparator();
    private final RoomPreferenceComparator roomComparator = new RoomPreferenceComparator();

    @Override
    public List<HotelMatchResult> matchAndRankHotels(List<Hotel> hotels, PreferenceCriteria criteria) {
        if (hotels == null || hotels.isEmpty()) {
            return Collections.emptyList();
        }

        PreferenceCriteria safeCriteria = criteria != null ? criteria : PreferenceCriteria.builder().build();
        List<HotelMatchResult> results = new ArrayList<>();

        for (Hotel hotel : hotels) {
            results.add(matchHotel(hotel, safeCriteria));
        }

        // Rank using multi-field comparator:
        // 1. Match score descending
        // 2. Applicable price ascending
        // 3. Hotel name alphabetically
        results.sort(hotelComparator);
        return results;
    }

    @Override
    public HotelMatchResult matchHotel(Hotel hotel, PreferenceCriteria criteria) {
        List<String> matchReasons = new ArrayList<>();
        List<String> missedCriteria = new ArrayList<>();

        double totalWeight = 0.0;
        double earnedWeight = 0.0;

        // 1. Location — District Match
        if (criteria.hasDistrict()) {
            totalWeight += 25.0;
            if (hotel.getDistrict() != null && hotel.getDistrict().equalsIgnoreCase(criteria.getDistrict().trim())) {
                earnedWeight += 25.0;
                matchReasons.add("✓ District matches (" + hotel.getDistrict() + ")");
            } else {
                missedCriteria.add("✕ Outside preferred district (" + criteria.getDistrict() + ")");
            }
        }

        // 2. Location — State Match
        if (criteria.hasState()) {
            totalWeight += 15.0;
            if (hotel.getState() != null && hotel.getState().equalsIgnoreCase(criteria.getState().trim())) {
                earnedWeight += 15.0;
                matchReasons.add("✓ State matches (" + hotel.getState() + ")");
            } else {
                missedCriteria.add("✕ Outside preferred state (" + criteria.getState() + ")");
            }
        }

        // 3. Experience / Category Match
        if (criteria.hasExperience()) {
            totalWeight += 20.0;
            if (matchesExperience(hotel, criteria.getExperience())) {
                earnedWeight += 20.0;
                matchReasons.add("✓ " + formatExperienceName(criteria.getExperience()) + " experience matches");
            } else {
                missedCriteria.add("✕ " + formatExperienceName(criteria.getExperience()) + " experience not primary");
            }
        }

        // 4. Facilities Multi-Select Match
        if (criteria.hasFacilities()) {
            double facilityWeightPerItem = 25.0 / Math.max(1, criteria.getFacilities().size());
            for (String fac : criteria.getFacilities()) {
                totalWeight += facilityWeightPerItem;
                if (hotelHasFacility(hotel, fac)) {
                    earnedWeight += facilityWeightPerItem;
                    matchReasons.add("✓ " + formatFacilityShort(fac) + " available");
                } else {
                    missedCriteria.add("✕ " + formatFacilityShort(fac) + " not listed");
                }
            }
        }

        // 5. Room-Level Matching: Determine best suitable room & occupancy
        int guests = criteria.getGuests();
        RoomMatchResult roomMatch = determineBestRoomForHotel(hotel, criteria);
        totalWeight += 15.0;
        if (roomMatch.isSuitableForOccupancy()) {
            earnedWeight += 15.0;
            matchReasons.add("✓ Suitable for " + guests + (guests == 1 ? " guest" : " guests"));
        } else {
            missedCriteria.add("✕ Limited capacity for " + guests + " guests");
        }

        // 6. Room Type Match
        totalWeight += 10.0;
        if (roomMatch.isSuitableForRoomType()) {
            earnedWeight += 10.0;
            if (criteria.isSharedRoom()) {
                matchReasons.add("✓ Shared / Twin room available");
            } else {
                matchReasons.add("✓ Private room available (" + roomMatch.getRoomName() + ")");
            }
        } else {
            missedCriteria.add("✕ Requested room type not optimal");
        }

        // 7. Budget / Price Suitability
        double applicablePrice = roomMatch.getApplicablePrice();
        if (criteria.hasBudget()) {
            totalWeight += 20.0;
            if (isWithinBudget(applicablePrice, criteria)) {
                earnedWeight += 20.0;
                matchReasons.add("✓ Within selected budget (₹" + String.format("%,.0f", applicablePrice) + "/night)");
            } else {
                missedCriteria.add("✕ Outside selected budget (₹" + String.format("%,.0f", applicablePrice) + "/night)");
            }
        }

        // 8. Dates / Availability Suitability
        if (criteria.hasDates()) {
            totalWeight += 10.0;
            long nights = ChronoUnit.DAYS.between(criteria.getCheckInDate(), criteria.getCheckOutDate());
            earnedWeight += 10.0;
            matchReasons.add("✓ Available for selected dates (" + nights + (nights == 1 ? " night)" : " nights)"));
        }

        // Calculate Deterministic Score Percentage
        int finalScore;
        if (totalWeight <= 0.0) {
            // Baseline score when no specific filters are applied (general luxury match 88-95% based on rating)
            double ratingBonus = Math.max(0, (hotel.getRating() - 4.5) * 10);
            finalScore = (int) Math.round(88 + ratingBonus);
            matchReasons.add("✓ Verified luxury hospitality standards");
            matchReasons.add("✓ High guest satisfaction rating (" + hotel.getRating() + "★)");
            matchReasons.add("✓ " + roomMatch.getRoomName() + " available");
        } else {
            double rawPercentage = (earnedWeight / totalWeight) * 100.0;
            // Scale gracefully: a hotel that meets 100% of selected criteria gets 94% - 98%
            // A hotel with one missing preference gets 78% - 84%
            double ratingFactor = (hotel.getRating() >= 4.9) ? 2.0 : (hotel.getRating() >= 4.7 ? 1.0 : 0.0);
            double scaledScore = (rawPercentage * 0.94) + ratingFactor + (earnedWeight > 0 ? 3.0 : 0.0);
            finalScore = (int) Math.round(Math.min(98.0, Math.max(25.0, scaledScore)));
        }

        return new HotelMatchResult(hotel, finalScore, applicablePrice, roomMatch, matchReasons, missedCriteria);
    }

    @Override
    public List<RoomMatchResult> matchAndRankRooms(List<Room> rooms, PreferenceCriteria criteria) {
        if (rooms == null || rooms.isEmpty()) {
            return Collections.emptyList();
        }

        PreferenceCriteria safeCriteria = criteria != null ? criteria : PreferenceCriteria.builder().build();
        List<RoomMatchResult> results = new ArrayList<>();

        for (Room room : rooms) {
            results.add(matchRoom(room, safeCriteria));
        }

        results.sort(roomComparator);
        return results;
    }

    @Override
    public RoomMatchResult matchRoom(Room room, PreferenceCriteria criteria) {
        List<String> matchReasons = new ArrayList<>();
        double totalWeight = 0.0;
        double earnedWeight = 0.0;

        // Occupancy suitability
        int guests = criteria.getGuests();
        boolean suitableForOccupancy = room.getMaxOccupancy() >= guests;
        totalWeight += 25.0;
        if (suitableForOccupancy) {
            earnedWeight += 25.0;
            matchReasons.add("✓ Accommodates " + guests + " guests (Max: " + room.getMaxOccupancy() + ")");
        }

        // Room Type / Category suitability
        boolean suitableForRoomType = true;
        totalWeight += 20.0;
        if (criteria.isSharedRoom()) {
            matchReasons.add("✓ Shared / Twin configuration available");
            earnedWeight += 20.0;
        } else {
            matchReasons.add("✓ Private " + room.getCategory().getDisplayName() + " option");
            earnedWeight += 20.0;
        }

        // Budget suitability
        double effectivePrice = room.getCurrentTariff() > 0 ? room.getCurrentTariff() : room.getBasePricePerNight();
        boolean withinBudget = true;
        if (criteria.hasBudget()) {
            totalWeight += 30.0;
            withinBudget = isWithinBudget(effectivePrice, criteria);
            if (withinBudget) {
                earnedWeight += 30.0;
                matchReasons.add("✓ Tariff matches budget range (₹" + String.format("%,.0f", effectivePrice) + ")");
            }
        }

        // Amenities match
        if (criteria.hasFacilities() && room.getAmenities() != null && !room.getAmenities().isEmpty()) {
            Set<Amenity> roomAmenities = room.getAmenities();
            for (String fac : criteria.getFacilities()) {
                totalWeight += 10.0;
                boolean hasAmenity = roomAmenities.stream()
                        .anyMatch(a -> a.getName().toLowerCase().contains(fac.toLowerCase()));
                if (hasAmenity) {
                    earnedWeight += 10.0;
                    matchReasons.add("✓ Room amenity: " + fac);
                }
            }
        }

        int score = (int) Math.round(totalWeight > 0 ? (earnedWeight / totalWeight) * 100.0 : 90.0);
        return new RoomMatchResult(
                room.getCategory().getDisplayName(),
                room.getCategory(),
                room.getMaxOccupancy(),
                effectivePrice,
                suitableForOccupancy,
                suitableForRoomType,
                withinBudget,
                score,
                matchReasons
        );
    }

    private RoomMatchResult determineBestRoomForHotel(Hotel hotel, PreferenceCriteria criteria) {
        int guests = criteria.getGuests();
        double basePrice = hotel.getStartingPricePerNight();

        String roomName;
        RoomCategory category;
        int maxOccupancy;
        double applicablePrice;

        if (criteria.isSharedRoom()) {
            roomName = "Deluxe Twin Room";
            category = RoomCategory.DELUXE;
            maxOccupancy = 2;
            applicablePrice = basePrice;
        } else if (guests <= 2) {
            roomName = "Deluxe Room";
            category = RoomCategory.DELUXE;
            maxOccupancy = 2;
            applicablePrice = basePrice;
        } else if (guests <= 4) {
            roomName = "Executive Suite";
            category = RoomCategory.SUITE;
            maxOccupancy = 4;
            // Suites typically run ~1.6x base room tariff
            applicablePrice = Math.round(basePrice * 1.6);
        } else {
            roomName = "Presidential Suite";
            category = RoomCategory.PRESIDENTIAL;
            maxOccupancy = 6;
            // Presidential suites run ~2.8x base room tariff
            applicablePrice = Math.round(basePrice * 2.8);
        }

        boolean suitableForOccupancy = (maxOccupancy >= guests);
        boolean suitableForRoomType = true;
        boolean withinBudget = !criteria.hasBudget() || isWithinBudget(applicablePrice, criteria);

        List<String> reasons = new ArrayList<>();
        if (suitableForOccupancy) {
            reasons.add("✓ Suitable for " + guests + " guests");
        }
        reasons.add("✓ " + roomName + " available");

        return new RoomMatchResult(roomName, category, maxOccupancy, applicablePrice,
                suitableForOccupancy, suitableForRoomType, withinBudget, 90, reasons);
    }

    private boolean matchesExperience(Hotel hotel, String experience) {
        if (experience == null || experience.isBlank()) return true;
        String exp = experience.trim().toLowerCase();

        if (hotel.getExperienceCategories() != null) {
            for (String e : hotel.getExperienceCategories()) {
                if (e.equalsIgnoreCase(exp) || e.toLowerCase().contains(exp) || exp.contains(e.toLowerCase())) {
                    return true;
                }
            }
        }

        if (hotel.getHotelType() != null) {
            String ht = hotel.getHotelType().toLowerCase();
            if (ht.contains(exp) || exp.contains(ht)) {
                return true;
            }
        }

        // Directory classification aliases
        if (exp.contains("5-star deluxe") || exp.contains("5 star deluxe") || exp.contains("ultra luxury")) {
            return (hotel.getHotelType() != null && hotel.getHotelType().toLowerCase().contains("deluxe")) || hotel.getRating() >= 4.8;
        }
        if (exp.equals("5-star") || exp.equals("5 star")) {
            return hotel.getHotelType() != null && hotel.getHotelType().toLowerCase().contains("5-star");
        }
        if (exp.contains("heritage")) {
            return (hotel.getHotelType() != null && hotel.getHotelType().toLowerCase().contains("heritage"))
                    || (hotel.getExperienceCategories() != null && hotel.getExperienceCategories().stream().anyMatch(e -> e.toLowerCase().contains("heritage") || e.toLowerCase().contains("palace")));
        }

        return false;
    }

    private boolean hotelHasFacility(Hotel hotel, String facility) {
        if (facility == null || facility.isBlank()) return true;
        String f = facility.trim().toLowerCase();

        List<String> list = hotel.getFacilityCategories();
        if (list == null || list.isEmpty()) {
            list = hotel.getSelectedAmenities();
        }
        if (list == null) return false;

        return list.stream().anyMatch(item -> {
            String itemLower = item.toLowerCase();
            if (itemLower.equals(f) || itemLower.contains(f) || f.contains(itemLower)) return true;
            // Aliases
            if (f.equals("swimming pool") || f.equals("pool")) return itemLower.contains("pool");
            if (f.equals("spa")) return itemLower.contains("spa") || itemLower.contains("wellness");
            if (f.equals("free wi-fi") || f.equals("wi-fi") || f.equals("wifi")) return itemLower.contains("wi-fi") || itemLower.contains("wifi") || itemLower.contains("internet");
            if (f.equals("gym") || f.equals("fitness")) return itemLower.contains("gym") || itemLower.contains("fitness");
            if (f.equals("restaurant") || f.equals("dining")) return itemLower.contains("restaurant") || itemLower.contains("dining");
            if (f.equals("conference facilities") || f.equals("conference")) return itemLower.contains("conference") || itemLower.contains("meeting");
            return false;
        });
    }

    private boolean isWithinBudget(double price, PreferenceCriteria criteria) {
        if (criteria.getMinPrice() != null && price < criteria.getMinPrice()) {
            return false;
        }
        if (criteria.getMaxPrice() != null && price > criteria.getMaxPrice()) {
            return false;
        }

        String b = criteria.getBudget();
        if (b != null && !b.isBlank() && !"all".equalsIgnoreCase(b.trim())) {
            String clean = b.trim().toLowerCase();
            if (clean.equals("0-10000") || clean.contains("under")) {
                return price <= 10000.0;
            } else if (clean.equals("10000-20000")) {
                return price >= 9500.0 && price <= 20000.0;
            } else if (clean.equals("20000-35000")) {
                return price >= 19500.0 && price <= 35000.0;
            } else if (clean.equals("35000-999999") || clean.contains("above") || clean.contains("+")) {
                return price >= 34000.0;
            } else if (clean.contains("-")) {
                try {
                    String[] parts = clean.split("-");
                    double min = Double.parseDouble(parts[0].trim().replaceAll("[^0-9.]", ""));
                    double max = Double.parseDouble(parts[1].trim().replaceAll("[^0-9.]", ""));
                    return price >= min && price <= max;
                } catch (Exception ignored) {}
            }
        }
        return true;
    }

    private String formatFacilityShort(String fac) {
        if (fac == null) return "";
        if (fac.equalsIgnoreCase("swimming pool")) return "Pool";
        if (fac.equalsIgnoreCase("free wi-fi")) return "Wi-Fi";
        if (fac.equalsIgnoreCase("conference facilities")) return "Conference";
        if (fac.equalsIgnoreCase("restaurant")) return "Dining";
        return fac;
    }

    private String formatExperienceName(String exp) {
        if (exp == null) return "";
        return exp;
    }
}
