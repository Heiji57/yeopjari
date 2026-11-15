import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { FamilyFeed, ShareSetting } from './types';
import { loadFeed } from './api';
import { FeedItem } from './components/FeedItem';
import { MemberCard } from './components/MemberCard';
import { ShareSettings } from './components/ShareSettings';

const REFRESH_MS = 10_000;

export default function App() {
  const [feed, setFeed] = useState<FamilyFeed | null>(null);
  const [offline, setOffline] = useState(false);
  // 서버 피드를 한 번이라도 받은 뒤에 연결이 끊기면, 시연 데이터로 바꾸지 않고 마지막 소식을 그대로 둔다
  const [stale, setStale] = useState(false);
  const hadLive = useRef(false);
  const [filter, setFilter] = useState<string | null>(null);
  const [driverView, setDriverView] = useState(false);

  const refresh = useCallback(async () => {
    const result = await loadFeed();
    if (result.offline && hadLive.current) {
      setStale(true);
      return;
    }
    if (!result.offline) hadLive.current = true;
    setFeed(result.feed);
    setOffline(result.offline);
    setStale(false);
  }, []);

  useEffect(() => {
    refresh();
    // 차에서 주행이 끝나면 몇 초 안에 새 카드가 뜨도록
    const timer = setInterval(refresh, REFRESH_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  const members = useMemo(() => new Map(feed?.members.map((m) => [m.driverId, m]) ?? []), [feed]);
  const cards = (feed?.cards ?? []).filter((c) => !filter || c.driverId === filter);
  const selected = filter ? members.get(filter) : undefined;

  function onShareSaved(saved: ShareSetting) {
    setFeed((prev) => prev && {
      ...prev,
      members: prev.members.map((m) => (m.driverId === saved.driverId ? { ...m, share: saved } : m)),
    });
    refresh();
  }

  return (
    <div className="page">
      <header className="top">
        <div>
          <p className="brand">옆자리</p>
          <h1>우리 가족 운전 소식</h1>
        </div>
        <button type="button" className="ghost" onClick={refresh}>새로고침</button>
      </header>

      <p className="privacy">실시간 위치는 공유하지 않아요 · 운전하는 사람이 켠 소식만 보여요</p>
      {offline && <p className="offline">서버에 연결되지 않아 저장된 시연 데이터를 보여주고 있어요.</p>}
      {stale && <p className="offline">연결이 잠시 끊겼어요. 마지막으로 받은 소식을 보여주고 있어요.</p>}

      {!feed ? (
        <p className="empty">불러오는 중…</p>
      ) : (
        <>
          <section className="members" aria-label="가족 구성원">
            {feed.members.map((m) => (
              <MemberCard key={m.driverId} member={m} selected={filter === m.driverId}
                onSelect={() => setFilter(filter === m.driverId ? null : m.driverId)} />
            ))}
          </section>

          {selected && (
            <div className="driver-view-switch">
              <button type="button" className="ghost small" onClick={() => setDriverView(!driverView)}
                aria-expanded={driverView}>
                {driverView ? '운전자 화면 닫기' : `${selected.label}의 공유 설정 보기 (시연)`}
              </button>
              {driverView && <ShareSettings member={selected} disabled={offline || stale} onSaved={onShareSaved} />}
            </div>
          )}

          <section className="feed" aria-label="소식">
            <h2>{selected ? `${selected.label} 소식` : '최근 소식'}</h2>
            {cards.length === 0 ? (
              <p className="empty">아직 나눈 소식이 없어요.</p>
            ) : (
              cards.map((c) => <FeedItem key={c.id} card={c} member={members.get(c.driverId)} />)
            )}
          </section>
        </>
      )}
    </div>
  );
}
