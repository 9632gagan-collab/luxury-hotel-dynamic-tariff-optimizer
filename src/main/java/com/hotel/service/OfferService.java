package com.hotel.service;

import com.hotel.model.Offer;
import java.util.List;
import java.util.Optional;

public interface OfferService {
    List<Offer> getAllOffers();
    List<Offer> getActiveOffers();
    Optional<Offer> getOfferById(Long id);
    Offer saveOffer(Offer offer);
    Offer toggleOfferStatus(Long id);
    void deleteOffer(Long id);
    boolean existsByPromoCode(String promoCode);
}
