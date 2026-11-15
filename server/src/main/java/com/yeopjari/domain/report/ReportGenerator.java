package com.yeopjari.domain.report;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.Report;
import com.yeopjari.domain.model.ReportItem;
import com.yeopjari.domain.model.Trend;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.TripEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * 도착 후(P단) 리포트. 감점·점수 없이 "예습 · 성장 · 조정"만 보여준다.
 *
 * <ul>
 *   <li>예습: 긴장 구간 몇 곳을 지나왔는지</li>
 *   <li>성장: 지난번보다 나아진 것 (깜빡이, 주차 후진, 급브레이크, 첫 합류)</li>
 *   <li>조정: 다음에 해볼 것 한 가지만</li>
 * </ul>
 */
public final class ReportGenerator {

    static final int TREND_THRESHOLD_PCT = 3;

    /**
     * @param previous 같은 운전자의 이전 주행들 (끝난 것, 이번 주행보다 앞선 것). 오래된 순서.
     */
    public Report generate(Trip trip, Driver driver, List<Trip> previous, Instant now) {
        List<ReportItem> items = new ArrayList<>();

        // 1) 예습: 긴장 구간
        int planned = trip.getTensionSpotsPlanned();
        int passed = Math.min(trip.count(EventType.TENSION_SPOT_PASSED), Math.max(planned, 0));
        if (planned > 0) {
            String note = passed == planned ? "모두 지나왔어요" : "%d곳 지나왔어요".formatted(passed);
            items.add(new ReportItem("tension", "긴장 구간", passed + "/" + planned, Trend.NONE, true, note));
        }

        // 2) 성장: 깜빡이
        Optional<Integer> signal = signalRate(trip);
        Optional<Integer> prevSignal = lastPresent(previous, ReportGenerator::signalRate);
        signal.ifPresent(pct -> {
            Trend trend = prevSignal.map(p -> compare(pct, p, TREND_THRESHOLD_PCT)).orElse(Trend.NONE);
            String note = prevSignal.map(p -> "지난번 %d%%".formatted(p)).orElse("회전·합류 때 켠 비율");
            items.add(new ReportItem("signal", "깜빡이", pct + "%", trend, trend != Trend.DOWN, note));
        });

        // 3) 성장: 주차 후진 횟수
        Optional<TripEvent> parking = trip.lastOf(EventType.PARKING_DONE);
        parking.ifPresent(p -> {
            int reverse = p.intAttr("reverseCount", 0);
            Optional<ParkingBaseline> baseline = parkingBaseline(previous, p.attr("placeId"));
            if (baseline.isPresent()) {
                ParkingBaseline b = baseline.get();
                Trend trend = compare(reverse, b.reverseCount(), 0);
                // 같은 주차장이면 "지난번", 다른 주차장들과 비교하면 "평소" (실력이 아니라 주차장 차이일 수 있으니)
                String note = switch (trend) {
                    case DOWN -> b.samePlace() ? "같은 주차장 지난번보다 줄었어요" : "평소보다 줄었어요";
                    case UP -> "오늘은 자리가 좁았나 봐요";
                    default -> b.samePlace() ? "지난번과 비슷해요" : "평소와 비슷해요";
                };
                items.add(new ReportItem("parking", "주차 후진", "%d→%d번".formatted(b.reverseCount(), reverse),
                        trend, trend != Trend.UP, note));
            } else {
                items.add(new ReportItem("parking", "주차 후진", reverse + "번", Trend.NONE, true, "첫 기록이에요"));
            }
        });

        // 4) 성장: 급브레이크 (지난 기록이 있을 때만)
        int hardBrakes = trip.count(EventType.HARD_BRAKE);
        if (!previous.isEmpty()) {
            int prevHardBrakes = previous.get(previous.size() - 1).count(EventType.HARD_BRAKE);
            if (hardBrakes > 0 || prevHardBrakes > 0) {
                Trend trend = compare(hardBrakes, prevHardBrakes, 0);
                items.add(new ReportItem("hardBrake", "급브레이크", "%d→%d번".formatted(prevHardBrakes, hardBrakes),
                        trend, trend != Trend.UP, trend == Trend.DOWN ? "더 부드러워졌어요" : "지난번과 비교"));
            }
        }

        // 5) 성장: 처음 해낸 것
        List<String> milestones = Milestones.of(trip, previous);

        String title = title(driver.mode(), milestones);
        String next = nextSuggestion(trip, signal, parking.map(p -> p.intAttr("reverseCount", 0)));
        return new Report(trip.getId(), driver.id(), driver.mode(), title, List.copyOf(items),
                milestones, next, now);
    }

    public static Optional<Integer> signalRate(Trip trip) {
        int used = trip.count(EventType.TURN_SIGNAL_USED);
        int missed = trip.count(EventType.TURN_SIGNAL_MISSED);
        int total = used + missed;
        return total == 0 ? Optional.empty() : Optional.of(Math.round(used * 100f / total));
    }

    /** @param samePlace true 면 같은 주차장 지난 기록, false 면 지금까지 주차 평균 */
    private record ParkingBaseline(int reverseCount, boolean samePlace) {
    }

    private static Optional<ParkingBaseline> parkingBaseline(List<Trip> previous, String placeId) {
        // 같은 주차장 기록이 있으면 그것과, 없으면 지금까지 평균과 비교
        for (int i = previous.size() - 1; i >= 0; i--) {
            Optional<TripEvent> p = previous.get(i).lastOf(EventType.PARKING_DONE);
            if (p.isPresent() && placeId != null && placeId.equals(p.get().attr("placeId"))) {
                return Optional.of(new ParkingBaseline(p.get().intAttr("reverseCount", 0), true));
            }
        }
        OptionalDouble avg = previous.stream()
                .map(t -> t.lastOf(EventType.PARKING_DONE))
                .flatMap(Optional::stream)
                .mapToInt(e -> e.intAttr("reverseCount", 0))
                .average();
        return avg.isPresent()
                ? Optional.of(new ParkingBaseline((int) Math.round(avg.getAsDouble()), false))
                : Optional.empty();
    }

    private static <T> Optional<T> lastPresent(List<Trip> trips, java.util.function.Function<Trip, Optional<T>> f) {
        for (int i = trips.size() - 1; i >= 0; i--) {
            Optional<T> value = f.apply(trips.get(i));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    private static Trend compare(int current, int previous, int threshold) {
        if (current - previous > threshold) {
            return Trend.UP;
        }
        if (previous - current > threshold) {
            return Trend.DOWN;
        }
        return Trend.SAME;
    }

    private static String title(DriverMode mode, List<String> milestones) {
        if (mode == DriverMode.SENIOR) {
            return "오늘 운전, 차분했어요";
        }
        return milestones.isEmpty() ? "오늘도 한 뼘 늘었어요" : "오늘 %s!".formatted(milestones.get(0));
    }

    private static String nextSuggestion(Trip trip, Optional<Integer> signal, Optional<Integer> reverse) {
        if (signal.isPresent() && signal.get() < 90) {
            return "다음엔 회전 50m 전에 깜빡이를 먼저 켜볼까요?";
        }
        if (trip.count(EventType.HARD_BRAKE) >= 2) {
            return "앞차와 거리를 한 칸 더 두면 브레이크가 부드러워져요.";
        }
        if (reverse.isPresent() && reverse.get() >= 4) {
            return "다음엔 출발 전에 평면 주차장을 먼저 골라볼까요?";
        }
        return "오늘처럼만 가면 돼요. 다음엔 조금 더 먼 길도 괜찮아요.";
    }
}
