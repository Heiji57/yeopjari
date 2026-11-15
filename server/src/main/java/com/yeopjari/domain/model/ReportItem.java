package com.yeopjari.domain.model;

/**
 * 도착 리포트 한 칸. 감점·점수는 없다. positive=false 인 항목도 빨간색으로 표시하지 않고
 * "다음엔 이렇게 해볼까요" 제안으로만 이어진다.
 */
public record ReportItem(String key, String label, String value, Trend trend, boolean positive, String note) {
}
