// Spring 서버(com.yeopjari.domain.model)의 JSON 모양과 같다.

export type DriverMode = 'NOVICE' | 'SENIOR';

export interface ShareSetting {
  driverId: string;
  milestones: boolean;
  weeklySummary: boolean;
  arrivalAlert: boolean;
}

export interface MemberSummary {
  driverId: string;
  label: string;
  mode: DriverMode;
  carModel: string;
  share: ShareSetting;
}

export type CardKind = 'MILESTONE' | 'GROWTH' | 'WEEKLY' | 'ARRIVAL';

export interface FeedCard {
  id: string;
  driverId: string;
  kind: CardKind;
  emoji: string;
  title: string;
  body: string;
  at: string; // ISO-8601
}

export interface FamilyFeed {
  familyId: string;
  members: MemberSummary[];
  cards: FeedCard[];
}
