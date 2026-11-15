package com.yeopjari.domain.route;

import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.Maneuver;
import com.yeopjari.domain.model.ManeuverType;
import com.yeopjari.domain.model.TensionSpot;
import com.yeopjari.domain.model.TensionType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 경로에서 "오늘 긴장 구간"을 찾는다.
 *
 * <p>1단계는 규칙 기반이다. 규칙으로 후보를 만들고, 지난 주행 기록(DriverHistory)으로 사람마다 무게를 조정한다.
 * 문구를 더 자연스럽게 다듬는 일은 나중에 Gleo AI LLM 에 맡긴다 (README 참고).
 */
public final class TensionSpotDetector {

    public static final int MAX_SPOTS = 3;
    static final int QUICK_LANE_CHANGE_WINDOW_M = 800;
    static final int QUICK_TURN_WINDOW_M = 150;
    static final int SPEED_DROP_KPH = 20;

    public List<TensionSpot> detect(List<Maneuver> route, DriverMode mode, DriverHistory history) {
        List<TensionSpot> candidates = new ArrayList<>();
        for (int i = 0; i < route.size(); i++) {
            Maneuver current = route.get(i);
            Maneuver next = i + 1 < route.size() ? route.get(i + 1) : null;
            evaluate(current, next, mode, history).ifPresent(candidates::add);
        }
        return candidates.stream()
                .sorted(Comparator.comparingInt(TensionSpot::severity).reversed()
                        .thenComparingInt(TensionSpot::maneuverIndex))
                .limit(MAX_SPOTS)
                .sorted(Comparator.comparingInt(TensionSpot::maneuverIndex))
                .toList();
    }

    private Optional<TensionSpot> evaluate(Maneuver m, Maneuver next, DriverMode mode, DriverHistory history) {
        TensionType type;
        int severity;
        String title;
        String tip;

        switch (m.type()) {
            case MERGE -> {
                boolean quickLaneChange = m.lanesToCross() >= 2 && next != null
                        && next.distanceFromPrevM() <= QUICK_LANE_CHANGE_WINDOW_M;
                if (quickLaneChange) {
                    type = TensionType.MERGE_THEN_LANE_CHANGE;
                    severity = 3;
                    title = m.targetLane() > 0
                            ? "합류 후 바로 %d차로".formatted(m.targetLane())
                            : "합류 후 바로 차로 %d번 변경".formatted(m.lanesToCross());
                    tip = "합류 500m 전부터 오른쪽 차로에서 기다렸다가, 합류 후엔 한 차로씩 천천히 옮겨요.";
                } else {
                    type = TensionType.MERGE;
                    severity = 2;
                    title = "%s 합류".formatted(nameOr(m.roadName(), "도로"));
                    tip = "합류 차로 끝까지 가속해서 옆 차 속도에 맞춰요.";
                }
                if (!history.hasDriven(m.roadName())) {
                    severity += 1;
                    tip = "처음 가는 길이에요. " + tip;
                }
            }
            case TURN_LEFT, TURN_RIGHT -> {
                if (next == null || !next.type().isTurn() || next.distanceFromPrevM() > QUICK_TURN_WINDOW_M) {
                    return Optional.empty();
                }
                type = TensionType.QUICK_SUCCESSIVE_TURNS;
                severity = next.distanceFromPrevM() <= 100 ? 3 : 2;
                title = "%s 후 %dm %s".formatted(m.type().korean(), roundTo10(next.distanceFromPrevM()),
                        next.type().korean());
                tip = "첫 회전 전에, 바로 다음 %s 할 차로를 미리 정해 둬요.".formatted(next.type().korean());
            }
            case SCHOOL_ZONE -> {
                type = TensionType.SCHOOL_ZONE;
                severity = 2;
                title = m.speedLimitBefore() > 0 && m.speedLimitAfter() > 0
                        ? "스쿨존 %d→%d".formatted(m.speedLimitBefore(), m.speedLimitAfter())
                        : "스쿨존";
                tip = "들어가기 전에 미리 속도를 줄이고, 갓길의 아이들을 살펴요.";
            }
            case UTURN -> {
                type = TensionType.UTURN;
                severity = mode == DriverMode.SENIOR ? 3 : 2;
                title = "유턴 구간";
                tip = "맞은편 차가 다 지나간 뒤 천천히 돌아요.";
            }
            case ROUNDABOUT -> {
                type = TensionType.ROUNDABOUT;
                severity = mode == DriverMode.SENIOR ? 3 : 2;
                title = "회전교차로";
                tip = "안에 있는 차가 먼저예요. 왼쪽을 보고 들어가요.";
            }
            default -> {
                if (m.speedLimitAfter() <= 0 || m.speedLimitBefore() - m.speedLimitAfter() < SPEED_DROP_KPH) {
                    return Optional.empty();
                }
                type = TensionType.SPEED_DROP;
                severity = 1;
                title = "제한속도 %d→%d".formatted(m.speedLimitBefore(), m.speedLimitAfter());
                tip = "단속 구간 전에 미리 속도를 줄여요.";
            }
        }

        // 개인화: 익숙해진 구간은 가볍게, 어려워했던 구간은 무겁게
        if (history.smoothPasses(type) >= 3) {
            severity -= 1;
        }
        if (history.struggles(type) >= 1) {
            severity += 1;
        }
        severity = Math.max(1, Math.min(3, severity));

        return Optional.of(new TensionSpot("spot-" + m.index(), m.index(), type, severity, title, tip,
                m.roadName(), m.lat(), m.lng()));
    }

    private static int roundTo10(int meters) {
        return Math.max(10, Math.round(meters / 10f) * 10);
    }

    private static String nameOr(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
