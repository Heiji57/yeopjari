package com.yeopjari.domain;

import com.yeopjari.domain.model.ParkingAdvice;
import com.yeopjari.domain.model.ParkingDifficulty;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Place;
import com.yeopjari.domain.parking.ParkingDifficultyCalculator;
import com.yeopjari.domain.seed.DemoSeed;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ParkingDifficultyTest {

    @Test
    void recommendsTheEasyFlatLotNextToTheHardBasement() {
        ParkingAdvice advice = new DemoFixture().parking.advice(DemoSeed.MALL_B2);

        assertEquals(4, advice.target().stars());
        assertEquals(5.0, advice.target().avgReverse());
        assertNotNull(advice.alternative());
        assertEquals(1, advice.alternative().stars());
        assertEquals(3, advice.walkMinutes());
        assertEquals("○○몰 지하주차장 ★★★★ (후진 평균 5번) → 도보 3분 공영 평면주차장 ★ 추천", advice.message());
    }

    @Test
    void needsAtLeastThreeRecordsBeforeRating() {
        ParkingDifficultyCalculator calc = new ParkingDifficultyCalculator();
        Place place = new Place("p", "새 주차장", "지하", 0, 0, null, 0);
        ParkingDifficulty d = calc.difficulty(place, List.of(new ParkingSample("p", 9, 9, 400),
                new ParkingSample("p", 9, 9, 400)));

        assertEquals(0, d.stars());
        assertFalse(d.known());
    }

    @Test
    void newParkingRecordsFeedTheDifficulty() {
        DemoFixture fx = new DemoFixture();
        int before = fx.store.samplesOf(DemoSeed.MALL_FLAT).size();
        fx.driveFirstGangbyeon();

        assertEquals(before + 1, fx.store.samplesOf(DemoSeed.MALL_FLAT).size());
    }
}
