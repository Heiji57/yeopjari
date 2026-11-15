package com.yeopjari.domain.model;

import java.util.List;

public record EasyRouteOffer(String routeOption, int extraMinutes, int spotsBefore, int spotsAfter, String label,
                             List<TensionSpot> spots) {
}
