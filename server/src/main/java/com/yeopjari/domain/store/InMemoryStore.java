package com.yeopjari.domain.store;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.NotFoundException;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Place;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.model.Trip;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 10일 MVP 용 메모리 저장소. 서버를 껐다 켜면 시연 데이터(DemoSeed)로 다시 채워진다.
 * 대회 이후 DB 가 필요하면 이 클래스만 JPA 리포지토리로 바꾸면 된다.
 */
public class InMemoryStore {

    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();
    private final Map<String, Trip> trips = new ConcurrentHashMap<>();
    private final Map<String, Report> reports = new ConcurrentHashMap<>();
    private final Map<String, Place> places = new ConcurrentHashMap<>();
    private final List<ParkingSample> parkingSamples = new CopyOnWriteArrayList<>();
    private final Map<String, ShareSetting> shares = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1000);

    /** 시연 초기화용. */
    public void clear() {
        drivers.clear();
        trips.clear();
        reports.clear();
        places.clear();
        parkingSamples.clear();
        shares.clear();
    }

    public String nextId(String prefix) {
        return prefix + "-" + sequence.incrementAndGet();
    }

    // drivers
    public void saveDriver(Driver driver) {
        drivers.put(driver.id(), driver);
    }

    public Driver driver(String id) {
        return Optional.ofNullable(drivers.get(id)).orElseThrow(() -> new NotFoundException("운전자 없음: " + id));
    }

    public List<Driver> driversInFamily(String familyId) {
        return drivers.values().stream()
                .filter(d -> familyId.equals(d.familyId()))
                .sorted(Comparator.comparing(Driver::id).reversed())
                .toList();
    }

    // trips
    public void saveTrip(Trip trip) {
        trips.put(trip.getId(), trip);
    }

    public Trip trip(String id) {
        return Optional.ofNullable(trips.get(id)).orElseThrow(() -> new NotFoundException("주행 없음: " + id));
    }

    /** 끝난 주행만, 오래된 순. */
    public List<Trip> finishedTripsOf(String driverId) {
        return trips.values().stream()
                .filter(t -> driverId.equals(t.getDriverId()) && t.isFinished())
                .sorted(Comparator.comparing(Trip::getStartedAt))
                .toList();
    }

    // reports
    public void saveReport(Report report) {
        reports.put(report.tripId(), report);
    }

    public Optional<Report> report(String tripId) {
        return Optional.ofNullable(reports.get(tripId));
    }

    // places & parking
    public void savePlace(Place place) {
        places.put(place.id(), place);
    }

    public Place place(String id) {
        return Optional.ofNullable(places.get(id)).orElseThrow(() -> new NotFoundException("주차장 없음: " + id));
    }

    public List<Place> placesNear(String placeId) {
        return places.values().stream().filter(p -> placeId.equals(p.nearPlaceId())).toList();
    }

    public void addParkingSample(ParkingSample sample) {
        parkingSamples.add(sample);
    }

    public List<ParkingSample> samplesOf(String placeId) {
        return parkingSamples.stream().filter(s -> placeId.equals(s.placeId())).toList();
    }

    // share settings
    public ShareSetting share(String driverId) {
        return shares.getOrDefault(driverId, ShareSetting.defaults(driverId));
    }

    public void saveShare(ShareSetting setting) {
        shares.put(setting.driverId(), setting);
    }
}
