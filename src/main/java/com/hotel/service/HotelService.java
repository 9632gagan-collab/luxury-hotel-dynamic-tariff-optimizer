package com.hotel.service;

import com.hotel.model.Hotel;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service interface for managing verified luxury hotels across India,
 * multi-tier location hierarchy (India -> State -> District -> City -> Hotels),
 * advanced multi-select filtering, and interactive spatial discovery.
 */
public interface HotelService {

    List<Hotel> getAllHotels();

    List<Hotel> getFeaturedHotels();

    List<Hotel> getAllKarnatakaHotels();

    List<Hotel> getFeaturedKarnatakaHotels();

    Optional<Hotel> getHotelById(Long id);

    List<Hotel> getHotelsByState(String state);

    List<Hotel> getHotelsByDistrict(String state, String district);

    String getStateForDistrict(String district);

    List<Hotel> filterHotels(String district, String experience, String facility);

    List<Hotel> filterHotelsAdvanced(String state, String district, String experience,
                                    List<String> facilities, Integer guests, String roomType);

    List<Hotel> filterHotelsAdvanced(String state, String district, String experience,
                                    List<String> facilities, Integer guests, String roomType,
                                    String budget, Double minPrice, Double maxPrice,
                                    String checkIn, String checkOut);

    default List<Hotel> filterHotelsAdvanced(String state, String district, String experience,
                                            List<String> facilities, Integer guests, String roomType,
                                            String budget, String checkIn, String checkOut) {
        return filterHotelsAdvanced(state, district, experience, facilities, guests, roomType, budget, null, null, checkIn, checkOut);
    }

    List<String> getAllStates();

    List<String> getDistrictsByState(String state);

    List<String> getAllDistricts();

    Map<String, List<String>> getStateDistrictMap();

    Map<String, Long> getHotelCountByState();

    List<String> getExperienceCategories();

    List<String> getFacilityCategories();

    Hotel saveHotel(Hotel hotel);

    long getTotalHotelCount();
}
