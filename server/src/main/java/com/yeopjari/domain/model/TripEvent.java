package com.yeopjari.domain.model;

import java.util.Map;

public record TripEvent(EventType type, long atMs, Map<String, String> attrs) {

    public TripEvent {
        if (type == null) {
            throw new IllegalArgumentException("event type is required");
        }
        attrs = attrs == null ? Map.of() : Map.copyOf(attrs);
    }

    public static TripEvent of(EventType type, long atMs, String... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("keyValues must be pairs");
        }
        java.util.HashMap<String, String> map = new java.util.HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return new TripEvent(type, atMs, map);
    }

    public String attr(String key) {
        return attrs.get(key);
    }

    public int intAttr(String key, int defaultValue) {
        String value = attrs.get(key);
        if (value == null) {
            return defaultValue;
        }
        try {
            return (int) Math.round(Double.parseDouble(value));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
