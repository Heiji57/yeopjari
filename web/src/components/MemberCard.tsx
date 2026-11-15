import type { MemberSummary } from '../types';

const MODE_LABEL = { NOVICE: '초보 모드', SENIOR: '시니어 모드' } as const;

function Shared({ on, label }: { on: boolean; label: string }) {
  return (
    <li className={on ? 'shared on' : 'shared off'}>
      <span aria-hidden="true">{on ? '●' : '○'}</span> {label}
      <span className="sr-only">{on ? ' 공유 중' : ' 공유 안 함'}</span>
    </li>
  );
}

export function MemberCard({ member, selected, onSelect }: {
  member: MemberSummary;
  selected: boolean;
  onSelect: () => void;
}) {
  const { share } = member;
  return (
    <button type="button" className={`member-card mode-${member.mode.toLowerCase()} ${selected ? 'selected' : ''}`}
      onClick={onSelect} aria-pressed={selected}>
      <div className="member-head">
        <strong>{member.label}</strong>
        <span className="member-mode">{MODE_LABEL[member.mode]}</span>
      </div>
      <div className="member-car">{member.carModel}</div>
      <ul className="member-share">
        <Shared on={share.milestones} label="성장 소식" />
        <Shared on={share.weeklySummary} label="주간 요약" />
        <Shared on={share.arrivalAlert} label="도착 알림" />
      </ul>
    </button>
  );
}
