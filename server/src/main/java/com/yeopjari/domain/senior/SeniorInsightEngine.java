package com.yeopjari.domain.senior;

import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.SeniorInsight;
import com.yeopjari.domain.model.Trend;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.WeeklySummary;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 시니어 모드: 지난 주행을 모아 차 안 제안과 가족용 주간 요약을 만든다.
 * 평가하지 않고 "조정"만 제안한다 (밤 운전 → 낮 일정, 경고등 → 정비소 경유).
 */
public final class SeniorInsightEngine {

    public static final String TPMS = "TPMS";
    static final int TPMS_NOTICE_DAYS = 3;
    static final int NIGHT_HOUR = 18;

    private final ZoneId zone;

    public SeniorInsightEngine(ZoneId zone) {
        this.zone = zone;
    }

    public WeeklySummary weekly(List<Trip> trips, Instant now) {
        Instant weekStart = now.minus(Duration.ofDays(7));
        Instant prevStart = now.minus(Duration.ofDays(28));
        List<Trip> thisWeek = between(trips, weekStart, now);
        List<Trip> prevWeeks = between(trips, prevStart, weekStart);

        int night = nightMinutes(thisWeek);
        double nightPrevAvg = nightMinutes(prevWeeks) / 3.0;
        int hardBrakes = count(thisWeek, EventType.HARD_BRAKE);
        double hardBrakesPrevAvg = count(prevWeeks, EventType.HARD_BRAKE) / 3.0;

        Trend nightTrend;
        if (night > nightPrevAvg * 1.3 && night - nightPrevAvg >= 15) {
            nightTrend = Trend.UP;
        } else if (nightPrevAvg > 0 && night < nightPrevAvg * 0.7) {
            nightTrend = Trend.DOWN;
        } else {
            nightTrend = Trend.SAME;
        }
        int tpmsDays = tpmsWarningDays(trips, now);
        boolean stable = hardBrakes <= Math.max(1, Math.ceil(hardBrakesPrevAvg));
        return new WeeklySummary(stable, night, round1(nightPrevAvg), nightTrend, hardBrakes,
                round1(hardBrakesPrevAvg), tpmsDays);
    }

    public List<SeniorInsight> insights(List<Trip> trips, Instant now) {
        WeeklySummary summary = weekly(trips, now);
        List<SeniorInsight> result = new ArrayList<>();

        if (summary.tpmsWarningDays() >= TPMS_NOTICE_DAYS) {
            result.add(new SeniorInsight("TPMS",
                    "타이어 공기압 경고 %d일째예요. 가는 길에 정비소 들를까요?".formatted(summary.tpmsWarningDays()),
                    "ADD_WAYPOINT_REPAIR_SHOP", false));
        }
        if (summary.nightTrend() == Trend.UP) {
            List<Trip> thisWeek = between(trips, now.minus(Duration.ofDays(7)), now);
            boolean hospitalAtNight = thisWeek.stream().anyMatch(t ->
                    "HOSPITAL".equals(t.getDestinationCategory()) && isNight(t.getStartedAt()));
            String message = hospitalAtNight
                    ? "밤 운전이 늘었어요. 병원 예약은 오전으로 해볼까요?"
                    : "밤 운전이 늘었어요. 자주 가는 곳 일정을 낮으로 옮겨볼까요?";
            result.add(new SeniorInsight("NIGHT_DRIVING", message, "PLAN_DAYTIME", false));
        } else if (summary.nightTrend() == Trend.DOWN) {
            result.add(new SeniorInsight("NIGHT_DRIVING", "이번 주는 밤 운전이 줄었어요.", "NONE", true));
        }
        if (summary.stable()) {
            result.add(new SeniorInsight("STABLE", "이번 주 운전, 안정적이었어요.", "NONE", true));
        }
        return List.copyOf(result);
    }

    /** 가장 최근 주행부터 거슬러 올라가며 TPMS 경고가 계속 켜져 있던 첫 날부터 오늘까지 일수. */
    int tpmsWarningDays(List<Trip> trips, Instant now) {
        List<Trip> sorted = trips.stream()
                .sorted(Comparator.comparing(Trip::getStartedAt).reversed())
                .filter(t -> !t.getStartedAt().isAfter(now))
                .toList();
        LocalDate firstDay = null;
        for (Trip trip : sorted) {
            if (!trip.hasTelltale(TPMS)) {
                break;
            }
            firstDay = trip.getStartedAt().atZone(zone).toLocalDate();
        }
        if (firstDay == null) {
            return 0;
        }
        return (int) ChronoUnit.DAYS.between(firstDay, now.atZone(zone).toLocalDate()) + 1;
    }

    private boolean isNight(Instant at) {
        int hour = at.atZone(zone).getHour();
        return hour >= NIGHT_HOUR || hour < 6;
    }

    private static List<Trip> between(List<Trip> trips, Instant fromInclusive, Instant toExclusive) {
        return trips.stream()
                .filter(t -> !t.getStartedAt().isBefore(fromInclusive) && t.getStartedAt().isBefore(toExclusive))
                .toList();
    }

    private static int nightMinutes(List<Trip> trips) {
        return trips.stream()
                .flatMap(t -> t.eventsOf(EventType.NIGHT_DRIVING).stream())
                .mapToInt(e -> e.intAttr("minutes", 0))
                .sum();
    }

    private static int count(List<Trip> trips, EventType type) {
        return trips.stream().mapToInt(t -> t.count(type)).sum();
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }
}
