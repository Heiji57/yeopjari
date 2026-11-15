package com.yeopjari.domain.model;

public enum ManeuverType {
    STRAIGHT,
    TURN_LEFT,
    TURN_RIGHT,
    UTURN,
    MERGE,
    EXIT,
    ROUNDABOUT,
    SCHOOL_ZONE;

    public boolean isTurn() {
        return this == TURN_LEFT || this == TURN_RIGHT;
    }

    public String korean() {
        return switch (this) {
            case STRAIGHT -> "직진";
            case TURN_LEFT -> "좌회전";
            case TURN_RIGHT -> "우회전";
            case UTURN -> "유턴";
            case MERGE -> "합류";
            case EXIT -> "진출";
            case ROUNDABOUT -> "회전교차로";
            case SCHOOL_ZONE -> "스쿨존";
        };
    }
}
