package com.yeopjari.config;

import com.yeopjari.domain.family.FamilyFeedBuilder;
import com.yeopjari.domain.parking.ParkingDifficultyCalculator;
import com.yeopjari.domain.report.ReportGenerator;
import com.yeopjari.domain.route.RouteComparator;
import com.yeopjari.domain.route.TensionSpotDetector;
import com.yeopjari.domain.seed.DemoSeed;
import com.yeopjari.domain.senior.SeniorInsightEngine;
import com.yeopjari.domain.service.FamilyService;
import com.yeopjari.domain.service.ParkingService;
import com.yeopjari.domain.service.RouteService;
import com.yeopjari.domain.service.TripService;
import com.yeopjari.domain.store.InMemoryStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 도메인 로직은 Spring 에 의존하지 않는 순수 Java 다 (테스트가 빠르고, Android 쪽과 규칙을 맞추기 쉽다).
 * 여기서 한 번에 조립한다.
 */
@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Value("${yeopjari.cors-origins}")
    private String[] corsOrigins;

    @Bean
    public Clock clock(@Value("${yeopjari.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public InMemoryStore store(Clock clock, @Value("${yeopjari.seed-demo}") boolean seedDemo) {
        InMemoryStore store = new InMemoryStore();
        if (seedDemo) {
            DemoSeed.seed(store, clock);
        }
        return store;
    }

    @Bean
    public SeniorInsightEngine seniorInsightEngine(Clock clock) {
        return new SeniorInsightEngine(clock.getZone());
    }

    @Bean
    public TripService tripService(InMemoryStore store, Clock clock) {
        return new TripService(store, new ReportGenerator(), clock);
    }

    @Bean
    public RouteService routeService(InMemoryStore store) {
        return new RouteService(store, new RouteComparator(new TensionSpotDetector()));
    }

    @Bean
    public ParkingService parkingService(InMemoryStore store) {
        return new ParkingService(store, new ParkingDifficultyCalculator());
    }

    @Bean
    public FamilyService familyService(InMemoryStore store, SeniorInsightEngine seniorEngine, Clock clock) {
        return new FamilyService(store, new FamilyFeedBuilder(seniorEngine), seniorEngine, clock);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsOrigins)
                .allowedMethods("GET", "POST", "PUT", "OPTIONS");
    }
}
