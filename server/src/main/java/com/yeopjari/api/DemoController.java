package com.yeopjari.api;

import com.yeopjari.domain.seed.DemoSeed;
import com.yeopjari.domain.store.InMemoryStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;

/** 시연 리허설용. 발표 직전에 한 번 눌러서 김씨 가족 데이터로 되돌린다. */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final InMemoryStore store;
    private final Clock clock;

    public DemoController(InMemoryStore store, Clock clock) {
        this.store = store;
        this.clock = clock;
    }

    @PostMapping("/reset")
    public Map<String, String> reset() {
        store.clear();
        DemoSeed.seed(store, clock);
        return Map.of("status", "ok", "familyId", DemoSeed.FAMILY_ID);
    }
}
