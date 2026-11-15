package com.yeopjari.domain.model;

import java.util.List;

public record FamilyFeed(String familyId, List<MemberSummary> members, List<FeedCard> cards) {
}
