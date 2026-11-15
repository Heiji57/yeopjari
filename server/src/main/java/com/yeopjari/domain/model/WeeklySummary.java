package com.yeopjari.domain.model;

public record WeeklySummary(boolean stable, int nightMinutes, double nightMinutesPrevAvg, Trend nightTrend,
                            int hardBrakes, double hardBrakesPrevAvg, int tpmsWarningDays) {
}
