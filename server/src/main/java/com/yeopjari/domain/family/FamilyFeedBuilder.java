package com.yeopjari.domain.family;

import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.DriverMode;
import com.yeopjari.domain.model.EventType;
import com.yeopjari.domain.model.FamilyFeed;
import com.yeopjari.domain.model.FeedCard;
import com.yeopjari.domain.model.MemberSummary;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.model.Trend;
import com.yeopjari.domain.model.Trip;
import com.yeopjari.domain.model.WeeklySummary;
import com.yeopjari.domain.report.Milestones;
import com.yeopjari.domain.senior.SeniorInsightEngine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * 가족 웹 피드. 운전자가 켠 항목만 만든다. 실시간 위치·좌표는 어떤 카드에도 넣지 않는다.
 */
public final class FamilyFeedBuilder {

    static final Duration MILESTONE_WINDOW = Duration.ofDays(14);
    static final Duration ARRIVAL_WINDOW = Duration.ofHours(24);

    private final SeniorInsightEngine seniorEngine;

    public FamilyFeedBuilder(SeniorInsightEngine seniorEngine) {
        this.seniorEngine = seniorEngine;
    }

    /**
     * @param finishedTripsOf 운전자 id → 끝난 주행 목록 (오래된 순)
     */
    public FamilyFeed build(String familyId, List<Driver> members, Function<String, List<Trip>> finishedTripsOf,
                            Function<String, ShareSetting> shareOf, Instant now) {
        List<MemberSummary> summaries = new ArrayList<>();
        List<FeedCard> cards = new ArrayList<>();
        for (Driver driver : members) {
            ShareSetting share = shareOf.apply(driver.id());
            summaries.add(new MemberSummary(driver.id(), driver.label(), driver.mode(), driver.carModel(), share));
            List<Trip> trips = finishedTripsOf.apply(driver.id());
            if (share.milestones()) {
                cards.addAll(milestoneCards(driver, trips, now));
            }
            if (share.weeklySummary()) {
                if (driver.mode() == DriverMode.SENIOR) {
                    cards.add(seniorWeeklyCard(driver, trips, now));
                } else {
                    growthCard(driver, trips, now).ifPresent(cards::add);
                }
            }
            if (share.arrivalAlert()) {
                arrivalCard(driver, trips, now).ifPresent(cards::add);
            }
        }
        // 사건 카드(처음 해낸 것, 도착)를 먼저, 주간 요약류는 그 아래. 각각 최신 순.
        cards.sort(Comparator.comparing(FamilyFeedBuilder::isSummary)
                .thenComparing(Comparator.comparing(FeedCard::at).reversed()));
        return new FamilyFeed(familyId, List.copyOf(summaries), List.copyOf(cards));
    }

    private List<FeedCard> milestoneCards(Driver driver, List<Trip> trips, Instant now) {
        List<FeedCard> result = new ArrayList<>();
        for (int i = 0; i < trips.size(); i++) {
            Trip trip = trips.get(i);
            if (trip.getEndedAt() == null || trip.getEndedAt().isBefore(now.minus(MILESTONE_WINDOW))) {
                continue;
            }
            for (String milestone : Milestones.of(trip, trips.subList(0, i))) {
                result.add(new FeedCard("ms-" + trip.getId() + "-" + result.size(), driver.id(), "MILESTONE",
                        "🎉", "%s %s".formatted(driver.label(), milestone),
                        // 목적지는 쓰지 않는다 (도착 알림을 꺼 둔 사람도 성장 소식은 기본으로 켜져 있다)
                        "스스로 해낸 첫 경험이에요", trip.getEndedAt()));
            }
        }
        return result;
    }

    private Optional<FeedCard> growthCard(Driver driver, List<Trip> trips, Instant now) {
        Instant weekStart = now.minus(Duration.ofDays(7));
        Instant prevStart = now.minus(Duration.ofDays(14));
        Integer thisWeek = signalRate(trips, weekStart, now);
        Integer lastWeek = signalRate(trips, prevStart, weekStart);
        if (thisWeek == null) {
            return Optional.empty();
        }
        String body = lastWeek == null
                ? "회전·합류 때 깜빡이를 켠 비율이에요"
                : thisWeek > lastWeek ? "지난주 %d%%에서 늘었어요".formatted(lastWeek)
                : "지난주 %d%%".formatted(lastWeek);
        return Optional.of(new FeedCard("growth-" + driver.id(), driver.id(), "GROWTH", "🌱",
                "%s 이번 주 깜빡이 %d%%".formatted(driver.label(), thisWeek), body, now));
    }

    private FeedCard seniorWeeklyCard(Driver driver, List<Trip> trips, Instant now) {
        WeeklySummary s = seniorEngine.weekly(trips, now);
        String night = switch (s.nightTrend()) {
            case UP -> "밤 운전 조금 ↑";
            case DOWN -> "밤 운전 ↓";
            default -> "밤 운전 비슷";
        };
        String title = "%s 이번 주 %s · %s".formatted(driver.label(), s.stable() ? "안정" : "살펴볼 점 있어요", night);
        List<String> body = new ArrayList<>();
        body.add("급브레이크 %d번".formatted(s.hardBrakes()));
        if (s.tpmsWarningDays() >= 3) {
            body.add("타이어 공기압 경고 %d일째 — 차 안에서 정비소 안내를 드렸어요".formatted(s.tpmsWarningDays()));
        }
        if (s.nightTrend() == Trend.UP) {
            body.add("낮 시간 일정을 제안했어요");
        }
        return new FeedCard("weekly-" + driver.id(), driver.id(), "WEEKLY", s.stable() ? "🌿" : "🔎", title,
                String.join(" · ", body), now);
    }

    private Optional<FeedCard> arrivalCard(Driver driver, List<Trip> trips, Instant now) {
        if (trips.isEmpty()) {
            return Optional.empty();
        }
        Trip last = trips.get(trips.size() - 1);
        if (last.getEndedAt() == null || last.getEndedAt().isBefore(now.minus(ARRIVAL_WINDOW))) {
            return Optional.empty();
        }
        return Optional.of(new FeedCard("arrival-" + last.getId(), driver.id(), "ARRIVAL", "📍",
                // 장소 이름도 위치 정보라서 보내지 않는다 ("위치는 보내지 않아요" 약속)
                "%s 목적지에 잘 도착했어요".formatted(driver.label()),
                "본인이 켠 도착 알림이에요 · 장소는 보내지 않아요", last.getEndedAt()));
    }

    private static Integer signalRate(List<Trip> trips, Instant from, Instant to) {
        int used = 0;
        int missed = 0;
        for (Trip t : trips) {
            if (t.getStartedAt().isBefore(from) || !t.getStartedAt().isBefore(to)) {
                continue;
            }
            used += t.count(EventType.TURN_SIGNAL_USED);
            missed += t.count(EventType.TURN_SIGNAL_MISSED);
        }
        return used + missed == 0 ? null : Math.round(used * 100f / (used + missed));
    }

    private static boolean isSummary(FeedCard card) {
        return card.kind().equals("GROWTH") || card.kind().equals("WEEKLY");
    }
}
