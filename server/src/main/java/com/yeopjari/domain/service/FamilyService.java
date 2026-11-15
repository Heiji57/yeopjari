package com.yeopjari.domain.service;

import com.yeopjari.domain.family.FamilyFeedBuilder;
import com.yeopjari.domain.model.Driver;
import com.yeopjari.domain.model.FamilyFeed;
import com.yeopjari.domain.model.NotFoundException;
import com.yeopjari.domain.model.SeniorInsight;
import com.yeopjari.domain.model.ShareSetting;
import com.yeopjari.domain.senior.SeniorInsightEngine;
import com.yeopjari.domain.store.InMemoryStore;

import java.time.Clock;
import java.util.List;

public class FamilyService {

    private final InMemoryStore store;
    private final FamilyFeedBuilder feedBuilder;
    private final SeniorInsightEngine seniorEngine;
    private final Clock clock;

    public FamilyService(InMemoryStore store, FamilyFeedBuilder feedBuilder, SeniorInsightEngine seniorEngine,
                         Clock clock) {
        this.store = store;
        this.feedBuilder = feedBuilder;
        this.seniorEngine = seniorEngine;
        this.clock = clock;
    }

    public FamilyFeed feed(String familyId) {
        List<Driver> members = store.driversInFamily(familyId);
        if (members.isEmpty()) {
            throw new NotFoundException("가족 없음: " + familyId);
        }
        return feedBuilder.build(familyId, members, store::finishedTripsOf, store::share, clock.instant());
    }

    public ShareSetting share(String driverId) {
        store.driver(driverId);
        return store.share(driverId);
    }

    public ShareSetting updateShare(String driverId, ShareSetting setting) {
        store.driver(driverId);
        ShareSetting saved = new ShareSetting(driverId, setting.milestones(), setting.weeklySummary(),
                setting.arrivalAlert());
        store.saveShare(saved);
        return saved;
    }

    public List<SeniorInsight> insights(String driverId) {
        store.driver(driverId);
        return seniorEngine.insights(store.finishedTripsOf(driverId), clock.instant());
    }

    public Driver driver(String driverId) {
        return store.driver(driverId);
    }
}
