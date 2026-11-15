# 옆자리

> 운전이 불안한 사람 옆자리에, 누군가 앉아 있는 것처럼.
> 감점 없이 **예습 · 성장 · 조정**만 하는 현대차 플레오스 커넥트 차량 앱.

같은 엔진, 모드만 다름:

| 모드 | 운전하는 사람 | 걱정하는 사람 | 차 |
|---|---|---|---|
| 초보 | 첫 차 탄 자녀 (소연이) | 부모 | 디 올 뉴 아반떼 |
| 시니어 | 운전하는 부모님 (아버지) | 자녀 | 더 뉴 그랜저 |

---

## 폴더 구조

```
yeopjari/
├─ server/            Spring Boot 3.4 · Java 21   ← 리포트·주차장 집계·가족 공유 API + "AI" 규칙
│  └─ src/main/java/com/yeopjari/
│     ├─ domain/      ★ 순수 Java 로직 (Spring 의존 없음, 테스트 17개)
│     │  ├─ route/    긴장 구간 찾기 + 쉬운 길 비교      (AI 담당)
│     │  ├─ parking/  주차 난이도 점수                   (AI 담당)
│     │  ├─ report/   도착 리포트 (감점 없음)
│     │  ├─ senior/   시니어 제안·주간 요약
│     │  ├─ family/   가족 피드 (공유 켠 것만, 위치 없음)
│     │  └─ seed/     시연 데이터 (김씨 가족)
│     ├─ api/         REST 컨트롤러
│     └─ config/      조립·CORS
├─ web/               React 18 + Vite + TS        ← 가족 웹
└─ android/           Kotlin · Gradle
   ├─ core/           ★ 순수 Kotlin (Android 의존 없음, 테스트 10개)
   │                  이벤트 감지 · 음성 코치 · 시나리오 재생 · 서버 클라이언트
   └─ app/            Compose 화면 · TTS · Pleos SDK 어댑터(파일 1개)
```

---

## 빠르게 실행하기

### 1. 서버 (Spring)

```bash
cd server
mvn spring-boot:run          # http://localhost:8080
mvn test                     # 도메인 테스트 + API 흐름 테스트
```

켜지면 시연 데이터(김씨 가족)가 자동으로 채워져요.

> ⚠️ **리허설할 때마다, 그리고 발표 직전에 `POST /api/demo/reset`** (`curl -X POST http://localhost:8080/api/demo/reset`).
> 초기화 없이 한 번 더 달리면 "첫 강변북로 합류 성공"이 안 뜨고, 예습 구간(스쿨존 → 제한속도 80→60)·주차 별점·리포트 문구가 대본과 달라져요.

### 2. 가족 웹 (React)

```bash
cd web
cp .env.example .env         # VITE_API_BASE=http://localhost:8080
npm install
npm run dev                  # http://localhost:5173
```

서버가 꺼져 있으면 저장된 시연 데이터로 화면이 나와요 (발표 중 네트워크 사고 대비). 10초마다 자동 새로고침.

### 3. 차량 앱 (Android)

1. Android Studio 에서 `android/` 폴더 열기 (Gradle 동기화)
2. Pleos Connect Emulator 실행 ([설치 가이드](https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator))
3. 서버를 PC 에서 켠 상태로 `app` 실행 → 에뮬레이터는 `http://10.0.2.2:8080` 으로 PC 서버에 붙어요
4. 기본값은 **시나리오 모드** (`gradle.properties` → `yeopjari.usePleosSdk=false`)

```bash
cd android
./gradlew :core:test         # 이벤트 감지·음성 코치·시나리오 테스트
./gradlew :app:installDebug
```

---

## 발표 시연 대본 (약 3분)

> 2번에서 **쉬운 길**을 눌러도 돼요: 예습이 "쉬운 길 · 긴장 구간 1곳(스쿨존)"으로 바뀌고, 강변북로 없이 시내로 달려요 (`DemoScenarios.soyeonEasyRoute`). 이때 리포트에는 "첫 합류 성공"이 없어요 — 대본대로 가려면 "이대로 출발".

| # | 화면 | 하는 일 | 보여줄 것 |
|---|---|---|---|
| 1 | 차량 앱 · P단 | 소연이로 시작 | **오늘 긴장 구간 3곳**: 합류 후 바로 1차로 / 우회전 후 100m 좌회전 / 스쿨존 60→30 |
| 2 | 〃 | 오른쪽 위 | **쉬운 길 +4분 · 긴장 구간 3→1** 버튼, 주차: ○○몰 지하 ★★★★ (후진 평균 5번) → 도보 3분 평면 ★ 추천 |
| 3 | 〃 | "이대로 출발" (오늘은 도전!) | 시나리오 6배속 재생, 약 40초 (음성이 나오는 구간은 실제 속도에 가깝게 늘려 둬서 말이 화면보다 늦지 않아요) |
| 4 | 차량 앱 · D단 | 음성 | "500m 뒤 합류예요. 지금 오른쪽 차로로 가 있을게요" → "왼쪽 깜빡이 켜볼까요?" → "곧 스쿨존이에요. 30으로 천천히 갈게요" |
| 5 | 차량 앱 · P단 | 주차 끝 → 자동 리포트 | **긴장 구간 3/3 · 깜빡이 92% ↑ · 주차 후진 5→2번 ↓ · 첫 강변북로 합류 성공** (점수·감점 없음) |
| 6 | 가족 웹 (폰) | 새로고침 | 🎉 **소연이 첫 강변북로 합류 성공** 카드가 맨 위에 |
| 7 | 차량 앱 | 아버지로 전환 | 큰 글씨 + 음성: "타이어 공기압 경고 5일째예요. 가는 길에 정비소 들를까요?" [들를게요], "밤 운전이 늘었어요. 병원 예약은 오전으로 해볼까요?" |
| 8 | 가족 웹 | 아버지 카드 | 🌿 **아버지 이번 주 안정 · 밤 운전 조금 ↑** / 📍 "아버지 목적지에 잘 도착했어요" (본인이 켠 것만, 장소 이름은 안 보냄) |
| 9 | 가족 웹 | 소연이 → 공유 설정 보기 | "무엇을 나눌지는 운전하는 사람이 정해요", **실시간 위치 공유 — 옆자리에는 이 기능이 없어요** |

> 1~6번 숫자는 `server/.../DemoRoutePreviewTest`, `ReportGeneratorTest`, `android/core/.../VoiceCoachTest`, `DriveEventDetectorTest` 가 고정하고 있어요.
> 시연 데이터나 규칙을 바꾸다 테스트가 깨지면 **대본도 같이 확인**하세요.

---

## 핵심 규칙 (심사 질문 대비)

### 긴장 구간 찾기 (`server/.../route/TensionSpotDetector.java`)
규칙 기반 + 개인화. 최대 3곳, 경로 순서대로.

| 규칙 | 긴장도 |
|---|---|
| 합류 후 800m 안에 차로 2개 이상 변경 | 3 |
| 회전 후 150m 안에 또 회전 (100m 이하면 3) | 2~3 |
| 처음 가는 도로의 합류 | +1 |
| 스쿨존 / 유턴 / 회전교차로 (시니어는 유턴·회전교차로 3) | 2 |
| 제한속도 20km/h 이상 감소 | 1 |
| **개인화**: 같은 유형을 3번 무난히 지남 | −1 |
| **개인화**: 같은 유형에서 급제동·깜빡이 놓침 | +1 |

쉬운 길: 편한 길(`COMFORTABLE`)이 10분 이내로 더 걸리고 긴장도 합이 낮을 때만 버튼을 보여요.

### 주차 난이도 (`server/.../parking/ParkingDifficultyCalculator.java`)
사람들이 실제로 주차하며 넣은 **후진(R) 횟수 · 브레이크 횟수 · 걸린 시간**으로 자동 계산. 3건 이상 쌓여야 별점.

```
r = 0.5 × min(평균 후진/6, 1) + 0.3 × min((평균 시간−60초)/240초, 1) + 0.2 × min(평균 브레이크/15, 1)
별점 = 1 + round(r × 4)   (★ 쉬움 ~ ★★★★★ 어려움)
```
목적지가 ★4 이상이고 걸어서 5분 안에 ★2 이하가 있으면 추천.

### 도착 리포트 (`server/.../report/ReportGenerator.java`)
- **예습**: 긴장 구간 n/n
- **성장**: 깜빡이 % (지난번 대비 ±3%p 넘으면 ↑↓), 주차 후진 (같은 주차장 → 없으면 평균과 비교), 급브레이크, **처음 해낸 것** (처음 합류한 도로)
- **조정**: 다음에 해볼 것 딱 한 가지
- 점수·감점·위반이라는 말은 쓰지 않아요 (테스트로 검사)

### 시니어 (`server/.../senior/SeniorInsightEngine.java`)
- 타이어 공기압(TPMS) 경고가 3일 이상 이어지면 → 정비소 경유 제안
- 이번 주 밤 운전이 지난 3주 평균보다 30% 이상 + 15분 이상 늘면 → 낮 일정 제안 (저녁 병원 방문이 있었으면 "병원 예약은 오전으로")

### 가족 공유 원칙
- 운전하는 사람이 켠 것만: 성장 소식 / 주간 요약 / 도착 알림 (도착 알림은 기본 꺼짐)
- **실시간 위치는 기능 자체가 없음.** 피드 카드에 좌표가 들어가지 않는 것도 테스트로 검사

---

## API

| 메서드 | 경로 | 언제 | 설명 |
|---|---|---|---|
| POST | `/api/route/preview` | 출발 전 P | 경로 안내 지점 → 긴장 구간 ≤3 + 쉬운 길 제안 |
| GET | `/api/parking/{placeId}/difficulty` | 출발 전 P | 주차 난이도 + 대안 |
| POST | `/api/trips` | 출발 | 주행 시작 → `tripId` |
| POST | `/api/trips/{tripId}/events` | 주행 중 | 이벤트 묶음 업로드 (20개마다 + 도착 시) |
| POST | `/api/trips/{tripId}/finish` | 도착 후 P | 리포트 생성 (두 번 불러도 같음) |
| GET | `/api/trips/{tripId}/report` | 아무 때나 | 리포트 다시 보기 |
| GET | `/api/drivers/{driverId}/insights` | 시니어 P | 차 안 제안 |
| GET | `/api/family/{familyId}/feed` | 가족 웹 | 피드 |
| GET / PUT | `/api/drivers/{driverId}/share` | 운전자 | 공유 설정 |
| POST | `/api/demo/reset` | 리허설 | 시연 데이터로 초기화 |

에러는 `{"error": "..."}` + 404(없음) / 400(잘못된 요청).

<details>
<summary>요청·응답 예시</summary>

```jsonc
// POST /api/route/preview
{
  "driverId": "soyeon",
  "fastRoute": { "routeOption": "RECOMMENDED", "minutes": 32, "maneuvers": [
    { "index": 2, "type": "MERGE", "distanceFromPrevM": 1200, "lanesToCross": 3, "targetLane": 1,
      "speedLimitBefore": 0, "speedLimitAfter": 0, "roadName": "강변북로", "lat": 37.553, "lng": 126.895 }
  ]},
  "easyRoute": { "routeOption": "COMFORTABLE", "minutes": 36, "maneuvers": [ ... ] }
}
// → { "minutes": 32, "spots": [{ "maneuverIndex": 2, "type": "MERGE_THEN_LANE_CHANGE", "severity": 3,
//      "title": "합류 후 바로 1차로", "tip": "처음 가는 길이에요. 합류 500m 전부터 ..." }, ...],
//     "easyRoute": { "routeOption": "COMFORTABLE", "extraMinutes": 4, "label": "쉬운 길 +4분 · 긴장 구간 3→1", ... } }

// POST /api/trips/{tripId}/events
{ "events": [
  { "type": "TURN_SIGNAL_USED", "atMs": 12000, "attrs": { "maneuver": "TURN_RIGHT" } },
  { "type": "MERGE_SUCCESS", "atMs": 61000, "attrs": { "road": "강변북로" } },
  { "type": "PARKING_DONE", "atMs": 98000, "attrs": { "placeId": "mall_flat", "reverseCount": "2", "brakeCount": "3", "durationSec": "85" } }
]}
```

이벤트 종류와 `attrs` 규칙: `server/.../domain/model/EventType.java` 주석. Android `core/model/Models.kt` 의 `EventType` 과 이름이 같아야 해요.
</details>

---

## 지금 상태: 검증된 것 / 직접 해야 할 것

개발 환경 제약(외부 저장소 차단)으로 Spring Boot·Android 전체 빌드는 아직 못 돌렸어요. 대신 이렇게 확인했어요.

| 부분 | 확인 방법 | 결과 |
|---|---|---|
| 서버 도메인 로직 | JUnit 테스트 17개 | ✅ 통과 |
| 서버 컨트롤러 12개 | 실제 컨트롤러 코드를 HTTP 로 띄워 전 엔드포인트 호출 | ✅ 정상, 404/400 포함 |
| Android core | Kotlin 2.0 컴파일 + JUnit 4 테스트 10개 | ✅ 통과 |
| **처음부터 끝까지** | core 의 시나리오·감지기·음성 코치·HTTP 클라이언트 → 실제 서버 | ✅ 대본 1~8번 숫자·문구 그대로 |
| 가족 웹 | `tsc` + `vite build`, 폰 크기 브라우저에서 피드·공유 토글·오프라인 | ✅ |
| **Android app 모듈 (Compose 화면·TTS)** | 아직 빌드 안 함 | ⚠️ **1일차에 Android Studio 로 빌드해서 확인** |
| `mvn test` 의 `ApiFlowTest` (Spring 컨텍스트) | 아직 안 돌림 | ⚠️ 1일차에 `mvn test` |

---

## 10일 계획

### 1~2일차: 막힐 수 있는 것부터 (가장 중요)
- [ ] **(Android)** 플레이그라운드에서 프로젝트 만들고 **SDK 접근 신청** — 승인 대기가 있으니 오늘
- [ ] **(Android)** `android/` 를 Android Studio 로 빌드·실행 (app 모듈 첫 빌드)
- [ ] **(Android)** 버추얼 콘솔 확인: 기어·브레이크·깜빡이·TPMS 는 문서상 ✅ / **차속 카드는 목록에 없음** → 직접 확인
- [ ] **(Android)** NaviHelper `getTBTInfo` 가 **다음 안내 1개만** 주는지, **경로 전체 안내 목록**을 받을 방법이 있는지 확인 → 아래 리스크 1
- [ ] **(Spring)** `mvn test`, `mvn spring-boot:run`
- [ ] **(React)** `npm run dev` 로 서버 붙여 보기

### 3~6일차: MVP ① 길 예습 ② 도착 리포트 ③ 가족 웹
| 담당 | 할 일 |
|---|---|
| Android | `PleosAdapters.kt` 의 TODO 채우기 (SDK 승인 후) · 화면 다듬기 · 시나리오 하나 더 (시니어 저녁 병원 길 대본) |
| AI | 규칙 숫자 튜닝 · 긴장 구간 문구를 Gleo AI(LLM)로 자연스럽게 (규칙이 고르고, LLM 은 말투만) · 주차 난이도 가중치 근거 정리 |
| Spring | 인메모리 → DB 가 필요하면 `InMemoryStore` 만 교체 · 공유 설정 변경 권한 체크 |
| React | 가족 초대 링크 · 카드 디자인 · 시니어 자녀용 주간 요약 화면 |
| UI/UX | 차량 화면(가로) 초보·시니어 두 벌 · 주행 중 화면은 글자 3줄 이하 · 발표 슬라이드 |

### 7~9일차: 여유 되면 음성 안내 고도화 · 주차 난이도 데이터 늘리기 · 리허설 2회
### 10일차: 통합 리허설 → 시연 영상 녹화 (네트워크 사고 대비 백업 영상 필수)

---

## 리스크와 대응

1. **경로 전체 안내 목록** — 길 예습은 출발 전에 경로 전체(합류·회전·스쿨존 위치)를 알아야 해요. NaviHelper 문서상 API 는 `getTBTInfo`(다음 안내), `getRouteStateInfo`(현재 경로 정보) 정도라 **전체 목록을 주는지 불확실**해요.
   → 안 되면: 외부 길찾기 API(예: TMAP·카카오모빌리티 경로 안내)로 같은 출발·도착을 조회해 `maneuvers` 를 만들고, 실제 안내는 Pleos 내비가. 서버는 이미 `maneuvers` 만 받게 되어 있어 Android 쪽만 바꾸면 돼요.
2. **에뮬레이터 차속·경로 주행** — 버추얼 콘솔 카드 목록에 일반 차속이 없어요. → 시연은 **시나리오 모드**가 기본. 깜빡이·기어·TPMS 는 버추얼 콘솔로 라이브 조작해 "진짜 SDK 데이터로도 돈다"를 보여주기.
3. **Pleos SDK 이름** — 승인 전이라 Vehicle SDK 클래스·메서드 이름을 확인 못 했어요. SDK 를 만지는 곳은 `android/app/.../platform/PleosAdapters.kt` **한 파일**이고, 나머지 코드는 `DriveSignalSource` 인터페이스만 봐요.
4. **주행 중 화면 제한** — 차량 앱은 주행 중 복잡한 화면이 제한될 수 있어요. 주행 화면은 글자 3줄·버튼 없음으로 만들어 두었고, 상호작용은 전부 P단에서.
5. **대외 근거 수치** (기획서의 장롱면허 64%, 65세+ 사고 23.66% 등) — 발표 자료에 출처를 꼭 달기.

---

## 참고
- [Pleos Connect Virtual Console 사용 가이드](https://document.pleos.ai/en/docs/pleos-only/app-test-guide/virtual-console-guide)
- [NaviHelper SDK](https://pleos.ai/playground/resources/api-reference/connect-sdk-pleos/NaviHelper/intro) · [Routes(RouteOption)](https://document.pleos.ai/en/api-reference/connect-sdk-pleos/NaviHelper/routes)
