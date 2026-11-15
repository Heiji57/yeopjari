package com.yeopjari.domain.service;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.RouteCandidate;
import com.yeopjari.domain.model.RoutePreview;
import com.yeopjari.domain.route.DriverHistory;
import com.yeopjari.domain.route.RouteComparator;
import com.yeopjari.domain.store.InMemoryStore;

public class RouteService {

    private final InMemoryStore store;
    private final RouteComparator comparator;

    public RouteService(InMemoryStore store, RouteComparator comparator) {
        this.store = store;
        this.comparator = comparator;
    }

    public RoutePreview preview(String driverId, RouteCandidate fastRoute, RouteCandidate easyRoute) {
        Driver driver = store.driver(driverId);
        DriverHistory history = DriverHistory.from(store.finishedTripsOf(driverId));
        return comparator.preview(fastRoute, easyRoute, driver.mode(), history);
    }
}
