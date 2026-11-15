package com.yeopjari.domain.model;

import java.time.Instant;
import java.util.List;

public record Report(String tripId, String driverId, DriverMode mode, String title, List<ReportItem> items,
                     List<String> milestones, String nextSuggestion, Instant createdAt) {
}
