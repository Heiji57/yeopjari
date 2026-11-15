package com.yeopjari.domain.model;

/**
 * 주차장.
 *
 * @param nearPlaceId  이 주차장이 대안이 될 수 있는 목적지 주차장 id (없으면 null)
 * @param walkMinutes  nearPlaceId 목적지까지 걸어서 걸리는 시간
 */
public record Place(String id, String name, String kind, double lat, double lng, String nearPlaceId,
                    int walkMinutes) {
}
