import type { FamilyFeed, ShareSetting } from './types';
import { demoFeed } from './demoData';

const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080';
export const FAMILY_ID = import.meta.env.VITE_FAMILY_ID ?? 'kim';

export interface FeedResult {
  feed: FamilyFeed;
  offline: boolean;
}

/** 서버가 안 되면 저장해 둔 시연 데이터로 보여준다 (발표 중 네트워크 사고 대비). */
export async function loadFeed(): Promise<FeedResult> {
  try {
    const res = await fetch(`${API_BASE}/api/family/${FAMILY_ID}/feed`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    return { feed: (await res.json()) as FamilyFeed, offline: false };
  } catch {
    return { feed: demoFeed, offline: true };
  }
}

export async function saveShare(setting: ShareSetting): Promise<ShareSetting> {
  const res = await fetch(`${API_BASE}/api/drivers/${setting.driverId}/share`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      milestones: setting.milestones,
      weeklySummary: setting.weeklySummary,
      arrivalAlert: setting.arrivalAlert,
    }),
  });
  if (!res.ok) throw new Error(`공유 설정 저장 실패 (HTTP ${res.status})`);
  return (await res.json()) as ShareSetting;
}
