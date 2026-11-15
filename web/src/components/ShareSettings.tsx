import { useState } from 'react';
import type { MemberSummary, ShareSetting } from '../types';
import { saveShare } from '../api';

const ITEMS: { key: keyof Omit<ShareSetting, 'driverId'>; label: string; hint: string }[] = [
  { key: 'milestones', label: '성장 소식', hint: '"첫 고속도로 합류 성공" 같은 소식' },
  { key: 'weeklySummary', label: '주간 요약', hint: '깜빡이 사용, 밤 운전 추이 같은 한 주 요약' },
  { key: 'arrivalAlert', label: '도착 알림', hint: '목적지에 도착했다는 알림만 (위치는 보내지 않아요)' },
];

/**
 * 시연용 "운전자 화면". 실제 제품에서는 운전자 본인의 차량 앱(P단)에서만 바꾼다.
 * 가족 웹에서는 보기만 할 수 있다.
 */
export function ShareSettings({ member, disabled, onSaved }: {
  member: MemberSummary;
  disabled: boolean;
  onSaved: (s: ShareSetting) => void;
}) {
  const [saving, setSaving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function toggle(key: keyof Omit<ShareSetting, 'driverId'>) {
    setSaving(key);
    setError(null);
    try {
      onSaved(await saveShare({ ...member.share, [key]: !member.share[key] }));
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
    } finally {
      setSaving(null);
    }
  }

  return (
    <section className="share-panel" aria-label={`${member.label} 공유 설정`}>
      <p className="share-note">
        <strong>{member.label}</strong>의 차량 앱에서 보는 화면이에요. 무엇을 나눌지는 운전하는 사람이 정해요.
      </p>
      {ITEMS.map((item) => (
        <label key={item.key} className="toggle-row">
          <span>
            <span className="toggle-label">{item.label}</span>
            <span className="toggle-hint">{item.hint}</span>
          </span>
          <input type="checkbox" role="switch" checked={member.share[item.key]}
            disabled={disabled || saving !== null} onChange={() => toggle(item.key)} />
        </label>
      ))}
      <p className="toggle-fixed">실시간 위치 공유 — 옆자리에는 이 기능이 없어요</p>
      {disabled && <p className="share-error">서버에 연결되면 바꿀 수 있어요.</p>}
      {error && <p className="share-error">{error}</p>}
    </section>
  );
}
