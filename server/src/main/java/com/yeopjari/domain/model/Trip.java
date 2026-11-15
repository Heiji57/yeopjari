package com.yeopjari.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/** 한 번의 주행. 이벤트는 주행 중에 묶음으로 계속 추가된다. */
public final class Trip {

    private final String id;
    private final String driverId;
    private final Instant startedAt;
    private final String destinationName;
    private final String destinationCategory;
    private final String placeId;
    private final int tensionSpotsPlanned;
    private final List<TripEvent> events = new ArrayList<>();
    private Instant endedAt;

    public Trip(String id, String driverId, Instant startedAt, String destinationName,
                String destinationCategory, String placeId, int tensionSpotsPlanned) {
        this.id = id;
        this.driverId = driverId;
        this.startedAt = startedAt;
        this.destinationName = destinationName;
        this.destinationCategory = destinationCategory;
        this.placeId = placeId;
        this.tensionSpotsPlanned = tensionSpotsPlanned;
    }

    public synchronized void addEvents(Collection<TripEvent> newEvents) {
        if (endedAt != null) {
            throw new IllegalStateException("이미 끝난 주행에는 이벤트를 추가할 수 없어요: " + id);
        }
        events.addAll(newEvents);
    }

    public synchronized void finish(Instant at) {
        if (endedAt == null) {
            endedAt = at;
        }
    }

    public boolean isFinished() {
        return endedAt != null;
    }

    public synchronized List<TripEvent> getEvents() {
        return Collections.unmodifiableList(new ArrayList<>(events));
    }

    public List<TripEvent> eventsOf(EventType type) {
        return getEvents().stream().filter(e -> e.type() == type).toList();
    }

    public int count(EventType type) {
        return eventsOf(type).size();
    }

    public Optional<TripEvent> lastOf(EventType type) {
        List<TripEvent> list = eventsOf(type);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(list.size() - 1));
    }

    public boolean hasTelltale(String name) {
        return eventsOf(EventType.TELLTALE_ON).stream().anyMatch(e -> name.equals(e.attr("name")));
    }

    public String getId() { return id; }
    public String getDriverId() { return driverId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getEndedAt() { return endedAt; }
    public String getDestinationName() { return destinationName; }
    public String getDestinationCategory() { return destinationCategory; }
    public String getPlaceId() { return placeId; }
    public int getTensionSpotsPlanned() { return tensionSpotsPlanned; }
}
