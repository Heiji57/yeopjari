package com.yeopjari.domain.service;

import com.yeopjari.domain.model.ParkingAdvice;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Place;
import com.yeopjari.domain.parking.ParkingDifficultyCalculator;
import com.yeopjari.domain.store.InMemoryStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParkingService {

    private final InMemoryStore store;
    private final ParkingDifficultyCalculator calculator;

    public ParkingService(InMemoryStore store, ParkingDifficultyCalculator calculator) {
        this.store = store;
        this.calculator = calculator;
    }

    public ParkingAdvice advice(String placeId) {
        Place target = store.place(placeId);
        List<Place> nearby = store.placesNear(placeId);
        Map<String, List<ParkingSample>> samples = new HashMap<>();
        samples.put(target.id(), store.samplesOf(target.id()));
        nearby.forEach(p -> samples.put(p.id(), store.samplesOf(p.id())));
        return calculator.advise(target, nearby, samples);
    }
}
