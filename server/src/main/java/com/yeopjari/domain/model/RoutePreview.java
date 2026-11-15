package com.yeopjari.domain.model;

import java.util.List;

/** 출발 전(P단) 길 예습 결과. easyRoute 가 null 이면 쉬운 길 버튼을 숨긴다. */
public record RoutePreview(int minutes, List<TensionSpot> spots, EasyRouteOffer easyRoute) {
}
