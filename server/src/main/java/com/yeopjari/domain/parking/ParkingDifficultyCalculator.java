package com.yeopjari.domain.parking;

import com.yeopjari.domain.model.ParkingAdvice;
import com.yeopjari.domain.model.ParkingDifficulty;
import com.yeopjari.domain.model.ParkingSample;
import com.yeopjari.domain.model.Place;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 주차 난이도 = 사람들이 실제로 넣은 후진(R) 횟수 · 브레이크 횟수 · 걸린 시간으로 자동 계산.
 *
 * <pre>
 * 점수 r = 0.5 × min(평균 후진 / 6, 1)
 *        + 0.3 × min(max(평균 시간 − 60초, 0) / 240초, 1)
 *        + 0.2 × min(평균 브레이크 / 15, 1)
 * 별점  = 1 + round(r × 4)       (1 쉬움 ~ 5 어려움)
 * </pre>
 */
public final class ParkingDifficultyCalculator {

    public static final int MIN_SAMPLES = 3;
    static final int HARD_STARS = 4;
    static final int EASY_STARS = 2;
    static final int MAX_WALK_MINUTES = 5;

    public ParkingDifficulty difficulty(Place place, List<ParkingSample> samples) {
        int n = samples.size();
        if (n < MIN_SAMPLES) {
            return new ParkingDifficulty(place.id(), place.name(), place.kind(), 0, 0, 0, n, "아직 기록이 적어요");
        }
        double avgReverse = samples.stream().mapToInt(ParkingSample::reverseCount).average().orElse(0);
        double avgDuration = samples.stream().mapToInt(ParkingSample::durationSec).average().orElse(0);
        double avgBrake = samples.stream().mapToInt(ParkingSample::brakeCount).average().orElse(0);

        double r = 0.5 * Math.min(avgReverse / 6.0, 1.0)
                + 0.3 * Math.min(Math.max(avgDuration - 60.0, 0.0) / 240.0, 1.0)
                + 0.2 * Math.min(avgBrake / 15.0, 1.0);
        int stars = 1 + (int) Math.round(r * 4);
        return new ParkingDifficulty(place.id(), place.name(), place.kind(), stars, round1(avgReverse),
                Math.round(avgDuration), n, label(stars));
    }

    /**
     * 목적지 주차장이 어렵고(★4 이상) 걸어서 5분 안에 쉬운(★2 이하) 주차장이 있으면 그쪽을 추천한다.
     *
     * @param nearby 목적지 근처 주차장들 (Place.nearPlaceId == target.id)
     */
    public ParkingAdvice advise(Place target, List<Place> nearby, Map<String, List<ParkingSample>> samplesByPlace) {
        ParkingDifficulty targetDifficulty = difficulty(target, samplesByPlace.getOrDefault(target.id(), List.of()));
        if (!targetDifficulty.known() || targetDifficulty.stars() < HARD_STARS) {
            return new ParkingAdvice(targetDifficulty, null, null, describe(targetDifficulty));
        }
        Optional<Candidate> best = nearby.stream()
                .filter(p -> p.walkMinutes() <= MAX_WALK_MINUTES)
                .map(p -> new Candidate(p, difficulty(p, samplesByPlace.getOrDefault(p.id(), List.of()))))
                .filter(c -> c.difficulty().known() && c.difficulty().stars() <= EASY_STARS)
                .min(Comparator.comparingInt((Candidate c) -> c.difficulty().stars())
                        .thenComparingInt(c -> c.place().walkMinutes()));
        if (best.isEmpty()) {
            return new ParkingAdvice(targetDifficulty, null, null, describe(targetDifficulty));
        }
        Candidate c = best.get();
        String message = "%s → 도보 %d분 %s %s 추천".formatted(describe(targetDifficulty), c.place().walkMinutes(),
                c.place().name(), stars(c.difficulty().stars()));
        return new ParkingAdvice(targetDifficulty, c.difficulty(), c.place().walkMinutes(), message);
    }

    public static String stars(int count) {
        return "★".repeat(Math.max(count, 0));
    }

    private static String describe(ParkingDifficulty d) {
        if (!d.known()) {
            return "%s (%s)".formatted(d.placeName(), d.label());
        }
        return "%s %s (후진 평균 %s번)".formatted(d.placeName(), stars(d.stars()), formatCount(d.avgReverse()));
    }

    private static String formatCount(double value) {
        return value == Math.rint(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private static String label(int stars) {
        return switch (stars) {
            case 1 -> "아주 쉬워요";
            case 2 -> "쉬운 편이에요";
            case 3 -> "보통이에요";
            case 4 -> "까다로운 편이에요";
            default -> "많이 까다로워요";
        };
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    private record Candidate(Place place, ParkingDifficulty difficulty) {
    }
}
