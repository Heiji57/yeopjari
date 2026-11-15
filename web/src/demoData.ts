// 서버 응답(/api/family/kim/feed, 시연 주행 직후)을 저장한 것. 서버가 꺼져 있을 때만 쓴다.
// 시각은 '몇 분 전' 으로 저장해서, 언제 열어도 최근 소식처럼 보인다.
import type { FamilyFeed, FeedCard } from './types';

type StoredCard = Omit<FeedCard, 'at'> & { minutesAgo: number };

const members: FamilyFeed['members'] = [
  {
    "driverId": "soyeon",
    "label": "소연이",
    "mode": "NOVICE",
    "carModel": "디 올 뉴 아반떼",
    "share": {
      "driverId": "soyeon",
      "milestones": true,
      "weeklySummary": true,
      "arrivalAlert": false
    }
  },
  {
    "driverId": "father",
    "label": "아버지",
    "mode": "SENIOR",
    "carModel": "더 뉴 그랜저",
    "share": {
      "driverId": "father",
      "milestones": true,
      "weeklySummary": true,
      "arrivalAlert": true
    }
  }
];

const stored: StoredCard[] = [
  {
    "id": "ms-trip-1070-0",
    "driverId": "soyeon",
    "kind": "MILESTONE",
    "emoji": "🎉",
    "title": "소연이 첫 강변북로 합류 성공",
    "body": "스스로 해낸 첫 경험이에요",
    "minutesAgo": 0
  },
  {
    "id": "arrival-trip-1056",
    "driverId": "father",
    "kind": "ARRIVAL",
    "emoji": "📍",
    "title": "아버지 목적지에 잘 도착했어요",
    "body": "본인이 켠 도착 알림이에요 · 장소는 보내지 않아요",
    "minutesAgo": 165
  },
  {
    "id": "growth-soyeon",
    "driverId": "soyeon",
    "kind": "GROWTH",
    "emoji": "🌱",
    "title": "소연이 이번 주 깜빡이 88%",
    "body": "지난주 77%에서 늘었어요",
    "minutesAgo": 0
  },
  {
    "id": "weekly-father",
    "driverId": "father",
    "kind": "WEEKLY",
    "emoji": "🌿",
    "title": "아버지 이번 주 안정 · 밤 운전 조금 ↑",
    "body": "급브레이크 1번 · 타이어 공기압 경고 5일째 — 차 안에서 정비소 안내를 드렸어요 · 낮 시간 일정을 제안했어요",
    "minutesAgo": 0
  }
];

export const demoFeed: FamilyFeed = {
  familyId: 'kim',
  members,
  cards: stored.map(({ minutesAgo, ...card }) => ({
    ...card,
    at: new Date(Date.now() - minutesAgo * 60_000).toISOString(),
  })),
};
