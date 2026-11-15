package com.yeopjari.domain.model;

import java.time.Instant;

/** 가족 웹 카드. kind: MILESTONE, GROWTH, WEEKLY, ARRIVAL. 좌표 같은 위치 정보는 담지 않는다. */
public record FeedCard(String id, String driverId, String kind, String emoji, String title, String body,
                       Instant at) {
}
