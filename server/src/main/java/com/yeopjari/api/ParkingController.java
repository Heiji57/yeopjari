package com.yeopjari.api;

import com.yeopjari.domain.model.ParkingAdvice;
import com.yeopjari.domain.service.ParkingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parking")
public class ParkingController {

    private final ParkingService parking;

    public ParkingController(ParkingService parking) {
        this.parking = parking;
    }

    /** 목적지 주차장 난이도 + (어려우면) 걸어서 5분 안의 쉬운 주차장 추천. */
    @GetMapping("/{placeId}/difficulty")
    public ParkingAdvice difficulty(@PathVariable("placeId") String placeId) {
        return parking.advice(placeId);
    }
}
