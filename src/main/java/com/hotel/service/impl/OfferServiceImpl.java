package com.hotel.service.impl;

import com.hotel.model.Offer;
import com.hotel.repository.OfferRepository;
import com.hotel.service.OfferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;

    @Autowired
    public OfferServiceImpl(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Offer> getAllOffers() {
        return offerRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Offer> getActiveOffers() {
        return offerRepository.findByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Offer> getOfferById(Long id) {
        return offerRepository.findById(id);
    }

    @Override
    public Offer saveOffer(Offer offer) {
        return offerRepository.save(offer);
    }

    @Override
    public Offer toggleOfferStatus(Long id) {
        Optional<Offer> offerOpt = offerRepository.findById(id);
        if (offerOpt.isPresent()) {
            Offer offer = offerOpt.get();
            offer.setActive(!offer.isActive());
            return offerRepository.save(offer);
        }
        return null;
    }

    @Override
    public void deleteOffer(Long id) {
        offerRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPromoCode(String promoCode) {
        return offerRepository.existsByPromoCodeIgnoreCase(promoCode);
    }
}
