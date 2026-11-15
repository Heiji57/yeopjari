package com.yeopjari.domain.service;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.NotFoundException;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;
import com.yeopjari.domain.report.ReportGenerator;
import com.yeopjari.domain.store.InMemoryStore;

import java.time.Clock;
import java.util.List;

public class TripService {

    private final InMemoryStore store;
    private final ReportGenerator reportGenerator;
    private final Clock clock;

    public TripService(InMemoryStore store, ReportGenerator reportGenerator, Clock clock) {
        this.store = store;
        this.reportGenerator = reportGenerator;
        this.clock = clock;
    }

    public Trip start(String driverId, String destinationName, String destinationCategory, String placeId,
                      int tensionSpotsPlanned) {
        store.driver(driverId); // 존재 확인
        Trip trip = new Trip(store.nextId("trip"), driverId, clock.instant(), destinationName,
                destinationCategory, placeId, tensionSpotsPlanned);
        store.saveTrip(trip);
        return trip;
    }

    public int addEvents(String tripId, List<TripEvent> events) {
        Trip trip = store.trip(tripId);
        trip.addEvents(events);
        // 주차 기록은 모두의 주차 난이도 데이터가 된다
        for (TripEvent e : events) {
            if (e.type() == EventType.PARKING_DONE && e.attr("placeId") != null) {
                store.addParkingSample(new ParkingSample(e.attr("placeId"), e.intAttr("reverseCount", 0),
                        e.intAttr("brakeCount", 0), e.intAttr("durationSec", 0)));
            }
        }
        return trip.getEvents().size();
    }

    public Report finish(String tripId) {
        Trip trip = store.trip(tripId);
        return store.report(tripId).orElseGet(() -> {
            trip.finish(clock.instant());
            Driver driver = store.driver(trip.getDriverId());
            List<Trip> previous = store.finishedTripsOf(driver.id()).stream()
                    .filter(t -> !t.getId().equals(tripId) && t.getStartedAt().isBefore(trip.getStartedAt()))
                    .toList();
            Report report = reportGenerator.generate(trip, driver, previous, clock.instant());
            store.saveReport(report);
            return report;
        });
    }

    public Report report(String tripId) {
        return store.report(tripId).orElseThrow(() -> new NotFoundException("리포트가 아직 없어요: " + tripId));
    }
}
