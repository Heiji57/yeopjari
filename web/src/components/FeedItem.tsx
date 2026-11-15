import type { FeedCard, MemberSummary } from '../types';
import { relativeTime } from '../time';

const KIND_LABEL: Record<FeedCard['kind'], string> = {
  MILESTONE: '처음 해냈어요',
  GROWTH: '성장',
  WEEKLY: '주간 요약',
  ARRIVAL: '도착 알림',
};

export function FeedItem({ card, member }: { card: FeedCard; member?: MemberSummary }) {
  return (
    <article className={`feed-item kind-${card.kind.toLowerCase()}`}>
      <div className="feed-emoji" aria-hidden="true">{card.emoji}</div>
      <div className="feed-main">
        <div className="feed-meta">
          <span className="feed-kind">{KIND_LABEL[card.kind]}</span>
          {member && <span className={`mode-dot mode-${member.mode.toLowerCase()}`}>{member.label}</span>}
          {/* 주간 요약은 매번 새로 계산돼서 시각이 늘 '방금' 이 되므로 기간으로 보여준다 */}
          {card.kind === 'GROWTH' || card.kind === 'WEEKLY'
            ? <span>이번 주</span>
            : <time dateTime={card.at}>{relativeTime(card.at)}</time>}
        </div>
        <h3 className="feed-title">{card.title}</h3>
        <p className="feed-body">{card.body}</p>
      </div>
    </article>
  );
}
