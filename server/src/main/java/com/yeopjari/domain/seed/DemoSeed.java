package com.yeopjari.domain.seed;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Place;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.model.TensionType;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;
import com.yeopjari.domain.store.InMemoryStore;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 시연용 데이터. 항상 "지금" 기준으로 만들어져서 언제 서버를 켜도 최근 기록처럼 보인다.
 *
 * <p>김씨 가족(familyId=kim)
 * <ul>
 *   <li>소연이(soyeon): 초보, 아반떼. 지난 기록 3번. 강변북로 합류는 아직 안 해봤다 → 시연 주행에서 "첫 합류 성공"이 뜬다.
 *       스쿨존은 두 번만 지나서 아직 "익숙한 구간"이 아니다 (세 번 무난히 지나면 길 예습에서 빠질 수 있음).</li>
 *   <li>아버지(father): 시니어, 그랜저. 이번 주 밤 운전 증가(병원 저녁 방문), 타이어 공기압 경고 5일째.</li>
 * </ul>
 */
public final class DemoSeed {

    public static final String FAMILY_ID = "kim";
    public static final String SOYEON = "soyeon";
    public static final String FATHER = "father";
    public static final String MALL_B2 = "mall_b2";
    public static final String MALL_FLAT = "mall_flat";

    private final InMemoryStore store;
    private final Clock clock;
    private final ZoneId zone;

    private DemoSeed(InMemoryStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
        this.zone = clock.getZone();
    }

    public static void seed(InMemoryStore store, Clock clock) {
        new DemoSeed(store, clock).run();
    }

    private void run() {
        store.saveDriver(new Driver(SOYEON, "김소연", DriverMode.NOVICE, "디 올 뉴 아반떼", FAMILY_ID, "소연이"));
        store.saveDriver(new Driver(FATHER, "김영수", DriverMode.SENIOR, "더 뉴 그랜저", FAMILY_ID, "아버지"));
        store.saveShare(new ShareSetting(SOYEON, true, true, false));
        store.saveShare(new ShareSetting(FATHER, true, true, true));
        places();
        soyeonTrips();
        fatherTrips();
    }

    private void places() {
        store.savePlace(new Place(MALL_B2, "○○몰 지하주차장", "지하", 37.5260, 126.9295, null, 0));
        store.savePlace(new Place(MALL_FLAT, "공영 평면주차장", "평면", 37.5251, 126.9312, MALL_B2, 3));
        store.savePlace(new Place("academy", "학원 건물 주차장", "기계식", 37.5170, 126.9050, null, 0));
        store.savePlace(new Place("hospital", "한빛병원 주차장", "지상", 37.5400, 126.9500, null, 0));

        int[] b2Reverse = {5, 6, 4, 5, 5, 6, 4, 5, 5, 6, 4, 5};
        for (int i = 0; i < b2Reverse.length; i++) {
            store.addParkingSample(new ParkingSample(MALL_B2, b2Reverse[i], 7 + i % 3, 180 + (i % 5) * 10));
        }
        // 아버지 병원 길 예습에서 "기록이 적어요" 대신 별점이 보이도록
        int[] hospitalReverse = {1, 0, 1, 2, 1};
        for (int i = 0; i < hospitalReverse.length; i++) {
            store.addParkingSample(new ParkingSample("hospital", hospitalReverse[i], 2 + i % 2, 70 + i * 5));
        }
        int[] flatReverse = {1, 0, 1, 2, 1, 1, 0, 1, 1, 2};
        for (int i = 0; i < flatReverse.length; i++) {
            store.addParkingSample(new ParkingSample(MALL_FLAT, flatReverse[i], 1 + i % 3, 60 + (i % 4) * 10));
        }
    }

    private void soyeonTrips() {
        Instant now = clock.instant();

        Trip a = trip(SOYEON, now.minus(Duration.ofDays(12)), "○○몰", "MALL", MALL_B2, 2, 35);
        a.addEvents(signals(15, 5));
        a.addEvents(List.of(
                spot(TensionType.QUICK_SUCCESSIVE_TURNS, "양화로", false),
                spot(TensionType.SCHOOL_ZONE, "망원로", true),
                event(EventType.HARD_BRAKE, "speedKph", "42"),
                event(EventType.HARD_BRAKE, "speedKph", "38"),
                parking(MALL_B2, 5, 9, 230)));
        finish(a);

        Trip b = trip(SOYEON, now.minus(Duration.ofDays(8)), "○○몰", "MALL", MALL_B2, 2, 33);
        b.addEvents(signals(18, 5));
        b.addEvents(List.of(
                spot(TensionType.QUICK_SUCCESSIVE_TURNS, "양화로", true),
                spot(TensionType.SCHOOL_ZONE, "망원로", true),
                event(EventType.HARD_BRAKE, "speedKph", "35"),
                parking(MALL_B2, 5, 8, 210)));
        finish(b);

        Trip c = trip(SOYEON, now.minus(Duration.ofDays(3)), "영어 학원", "ACADEMY", "academy", 1, 18);
        c.addEvents(signals(20, 4));
        c.addEvents(List.of(
                spot(TensionType.QUICK_SUCCESSIVE_TURNS, "성미산로", true),
                parking("academy", 4, 6, 190)));
        finish(c);
    }

    private void fatherTrips() {
        Instant now = clock.instant();
        LocalDate today = now.atZone(zone).toLocalDate();

        // 이번 주: 최근 5일 타이어 공기압 경고, 저녁 병원 방문
        Trip t0 = trip(FATHER, now.minus(Duration.ofHours(3)), "망원시장", "MARKET", null, 0, 15);
        t0.addEvents(List.of(tpms()));
        finish(t0);
        Trip t1 = trip(FATHER, at(today.minusDays(1), 19, 30), "한빛병원", "HOSPITAL", "hospital", 1, 30);
        t1.addEvents(List.of(tpms(), night(25)));
        finish(t1);
        Trip t2 = trip(FATHER, at(today.minusDays(2), 10, 0), "마트", "MART", null, 0, 25);
        t2.addEvents(List.of(tpms(), event(EventType.HARD_BRAKE, "speedKph", "33")));
        finish(t2);
        Trip t4 = trip(FATHER, at(today.minusDays(4), 18, 40), "교회", "CHURCH", null, 0, 40);
        t4.addEvents(List.of(tpms(), night(20)));
        finish(t4);
        Trip t6 = trip(FATHER, at(today.minusDays(6), 11, 0), "공원", "PARK", null, 0, 20);
        finish(t6);

        // 지난 3주: 주 1번 저녁 운전 (평균 20분)
        int[][] past = {
                // {며칠 전, 시, 밤운전분, 급브레이크}
                {8, 10, 0, 0}, {10, 19, 20, 1}, {12, 11, 0, 0},
                {15, 9, 0, 1}, {17, 19, 15, 1}, {19, 14, 0, 0},
                {22, 10, 0, 0}, {24, 19, 25, 1}, {26, 15, 0, 0},
        };
        String[] destinations = {"마트", "친구 모임", "공원", "은행", "교회", "마트", "공원", "친구 모임", "은행"};
        for (int i = 0; i < past.length; i++) {
            Trip t = trip(FATHER, at(today.minusDays(past[i][0]), past[i][1], 0), destinations[i], "ETC", null, 0, 30);
            List<TripEvent> events = new ArrayList<>();
            if (past[i][2] > 0) {
                events.add(night(past[i][2]));
            }
            for (int k = 0; k < past[i][3]; k++) {
                events.add(event(EventType.HARD_BRAKE, "speedKph", "40"));
            }
            t.addEvents(events);
            finish(t);
        }
    }

    // ---- helpers ----

    private final java.util.Map<String, Instant> endTimes = new java.util.HashMap<>();

    private Trip trip(String driverId, Instant startedAt, String destination, String category, String placeId,
                      int planned, int minutes) {
        Trip trip = new Trip(store.nextId("trip"), driverId, startedAt, destination, category, placeId, planned);
        endTimes.put(trip.getId(), startedAt.plus(Duration.ofMinutes(minutes)));
        store.saveTrip(trip);
        return trip;
    }

    private void finish(Trip trip) {
        trip.finish(endTimes.get(trip.getId()));
    }

    private Instant at(LocalDate date, int hour, int minute) {
        return date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant();
    }

    private static List<TripEvent> signals(int used, int missed) {
        List<TripEvent> events = new ArrayList<>();
        for (int i = 0; i < used; i++) {
            events.add(event(EventType.TURN_SIGNAL_USED, "maneuver", i % 2 == 0 ? "TURN_LEFT" : "TURN_RIGHT"));
        }
        for (int i = 0; i < missed; i++) {
            events.add(event(EventType.TURN_SIGNAL_MISSED, "maneuver", "TURN_RIGHT"));
        }
        return events;
    }

    private static TripEvent spot(TensionType type, String road, boolean smooth) {
        return event(EventType.TENSION_SPOT_PASSED, "spotType", type.name(), "road", road,
                "smooth", String.valueOf(smooth));
    }

    private static TripEvent parking(String placeId, int reverse, int brakes, int seconds) {
        return event(EventType.PARKING_DONE, "placeId", placeId, "reverseCount", String.valueOf(reverse),
                "brakeCount", String.valueOf(brakes), "durationSec", String.valueOf(seconds));
    }

    private static TripEvent tpms() {
        return event(EventType.TELLTALE_ON, "name", "TPMS");
    }

    private static TripEvent night(int minutes) {
        return event(EventType.NIGHT_DRIVING, "minutes", String.valueOf(minutes));
    }

    private static TripEvent event(EventType type, String... kv) {
        return TripEvent.of(type, 0L, kv);
    }
}
