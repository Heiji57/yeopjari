package com.yeopjari.api;

import com.yeopjari.domain.model.RouteCandidate;
import com.yeopjari.domain.model.TripEvent;

import java.time.Instant;
import java.util.List;

/** 요청·응답 본문. 응답은 대부분 도메인 record 를 그대로 JSON 으로 내보낸다. */
public final class Dtos {

    private Dtos() {
    }

    public record StartTripRequest(String driverId, String destinationName, String destinationCategory,
                                   String placeId, Integer tensionSpotsPlanned) {
    }

    public record StartTripResponse(String tripId, Instant startedAt) {
    }

    public record EventsRequest(List<TripEvent> events) {
    }

    public record EventsResponse(String tripId, int eventCount) {
    }

    public record RoutePreviewRequest(String driverId, RouteCandidate fastRoute, RouteCandidate easyRoute) {
    }

    public record ShareRequest(boolean milestones, boolean weeklySummary, boolean arrivalAlert) {
    }

    public record ErrorResponse(String error) {
    }
}
