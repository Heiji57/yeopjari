package com.yeopjari.api;

import com.yeopjari.api.Dtos.EventsRequest;
import com.yeopjari.api.Dtos.EventsResponse;
import com.yeopjari.api.Dtos.StartTripRequest;
import com.yeopjari.api.Dtos.StartTripResponse;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.service.TripService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trips")
public class TripController {

    private final TripService trips;

    public TripController(TripService trips) {
        this.trips = trips;
    }

    /** 출발(D단 진입) 때 호출. tensionSpotsPlanned 는 길 예습에서 보여준 긴장 구간 수. */
    @PostMapping
    public StartTripResponse start(@RequestBody StartTripRequest req) {
        if (req.driverId() == null || req.driverId().isBlank()) {
            throw new IllegalArgumentException("driverId is required");
        }
        Trip trip = trips.start(req.driverId(), req.destinationName(), req.destinationCategory(), req.placeId(),
                req.tensionSpotsPlanned() == null ? 0 : req.tensionSpotsPlanned());
        return new StartTripResponse(trip.getId(), trip.getStartedAt());
    }

    /** 주행 중 이벤트 묶음 업로드. 차량 앱은 10~30초마다, 그리고 도착 직전에 한 번 보낸다. */
    @PostMapping("/{tripId}/events")
    public EventsResponse events(@PathVariable("tripId") String tripId, @RequestBody EventsRequest req) {
        List<com.yeopjari.domain.model.TripEvent> events = req.events() == null ? List.of() : req.events();
        return new EventsResponse(tripId, trips.addEvents(tripId, events));
    }

    /** 도착 후(P단) 호출. 리포트를 만들어 돌려준다. 두 번 불러도 같은 리포트. */
    @PostMapping("/{tripId}/finish")
    public Report finish(@PathVariable("tripId") String tripId) {
        return trips.finish(tripId);
    }

    @GetMapping("/{tripId}/report")
    public Report report(@PathVariable("tripId") String tripId) {
        return trips.report(tripId);
    }
}
