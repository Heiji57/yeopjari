package com.yeopjari.domain;

import com.yeopjari.domain.model.FamilyFeed;
import com.yeopjari.domain.model.FeedCard;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.seed.DemoSeed;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FamilyFeedBuilderTest {

    @Test
    void showsMilestoneAfterTheDemoDrive() {
        DemoFixture fx = new DemoFixture();
        fx.driveFirstGangbyeon();
        FamilyFeed feed = fx.family.feed(DemoSeed.FAMILY_ID);

        assertTrue(has(feed, "MILESTONE", "소연이 첫 강변북로 합류 성공"), feed::toString);
        assertTrue(has(feed, "WEEKLY", "아버지 이번 주 안정"), feed::toString);
    }

    @Test
    void arrivalAlertOnlyWhenTheDriverTurnedItOn() {
        DemoFixture fx = new DemoFixture();
        fx.driveFirstGangbyeon();
        FamilyFeed feed = fx.family.feed(DemoSeed.FAMILY_ID);

        assertFalse(feed.cards().stream().anyMatch(c ->
                c.kind().equals("ARRIVAL") && c.driverId().equals(DemoSeed.SOYEON)), "소연이는 도착 알림을 안 켰다");
        assertTrue(feed.cards().stream().anyMatch(c ->
                c.kind().equals("ARRIVAL") && c.driverId().equals(DemoSeed.FATHER)), "아버지는 켰다");
    }

    @Test
    void turningOffMilestonesHidesThem() {
        DemoFixture fx = new DemoFixture();
        fx.family.updateShare(DemoSeed.SOYEON, new ShareSetting(DemoSeed.SOYEON, false, true, false));
        fx.driveFirstGangbyeon();
        FamilyFeed feed = fx.family.feed(DemoSeed.FAMILY_ID);

        assertFalse(feed.cards().stream().anyMatch(c -> c.kind().equals("MILESTONE")));
    }

    @Test
    void feedNeverNamesTheDestination() {
        // 장소 이름도 위치 정보다. 성장 소식(기본 켜짐)과 도착 알림 모두 목적지를 쓰지 않는다.
        DemoFixture fx = new DemoFixture();
        fx.driveFirstGangbyeon();
        FamilyFeed feed = fx.family.feed(DemoSeed.FAMILY_ID);

        for (FeedCard card : feed.cards()) {
            String text = card.title() + card.body();
            for (String place : new String[]{"○○몰", "망원시장", "한빛병원", "null"}) {
                assertFalse(text.contains(place), place + " 가 피드에 있어요: " + card);
            }
        }
    }

    @Test
    void feedNeverContainsCoordinates() {
        DemoFixture fx = new DemoFixture();
        fx.driveFirstGangbyeon();
        FamilyFeed feed = fx.family.feed(DemoSeed.FAMILY_ID);

        for (FeedCard card : feed.cards()) {
            assertFalse((card.title() + card.body()).matches(".*\\d{2,3}\\.\\d{3,}.*"), "좌표처럼 보이는 숫자: " + card);
        }
    }

    private static boolean has(FamilyFeed feed, String kind, String titlePrefix) {
        return feed.cards().stream().anyMatch(c -> c.kind().equals(kind) && c.title().startsWith(titlePrefix));
    }
}
