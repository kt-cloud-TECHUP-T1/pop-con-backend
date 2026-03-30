# Anti-Macro: 더치경매 페이지 확장 & 4단계 VQA

## 개요

anti-macro 서비스에 더치경매 페이지 2종이 추가되었다.
VQA 레벨이 기존 3단계(easy/medium/hard)에서 4단계(1/2/3/4)로 변경되었다.
프론트엔드(Next 서버)에서 행동 데이터 수집 → anti-macro 분석 → 결과 기반으로 VQA 제공 및 사일런트드롭을 처리한다.

---

## 페이지 타입

| PageType | 설명 | 시그널 면제 |
|----------|------|-------------|
| `login` | 로그인 | 없음 |
| `popup-detail` | 팝업 상세 | `zero_mouse_touch_events`, `fast_load_to_click` 면제 |
| `draw-application` | 드로우 신청 | 없음 |
| `dutch-auction-detail` | 더치경매 상세 | `zero_mouse_touch_events`, `fast_load_to_click` 면제 (현재 수집 안 함 — 프론트에서 미전송) |
| `dutch-auction-application` | 더치경매 신청(입찰) | 없음 |

---

## API 스펙

### `POST /anti-macro/signals`

#### Request

```json
{
  "timestamp": 1711785600000,
  "visitorId": "fp_abc123",
  "userId": "12345",
  "payload": {
    "page": "dutch-auction-detail",
    "collectedAt": 1711785600000,
    "fingerprint": {
      "visitorId": "fp_abc123",
      "confidence": 0.95,
      "webdriver": false,
      "webglRenderer": "ANGLE (Intel, ...)",
      "webglVendor": "Google Inc. (Intel)",
      "userAgent": "Mozilla/5.0 ...",
      "platform": "Win32",
      "language": "ko-KR",
      "languages": ["ko-KR", "ko", "en-US"],
      "timezone": "Asia/Seoul",
      "screenResolution": { "width": 1920, "height": 1080 },
      "colorDepth": 24,
      "hardwareConcurrency": 8,
      "deviceMemory": 8,
      "components": {}
    },
    "rawData": {
      "clicks": [
        {
          "x": 500, "y": 300,
          "timestamp": 1711785601000,
          "isTrusted": true,
          "targetSelector": ".bid-button",
          "centerDistance": 3.5
        }
      ],
      "mouseMovements": [
        { "x": 100, "y": 200, "timestamp": 1711785600500, "isTrusted": true }
      ],
      "hasUntrustedEvent": false,
      "triggered": false,
      "fieldValue": null,
      "pageLoadTimestamp": 1711785599000,
      "firstInteractionTimestamp": 1711785600500,
      "loadToFirstClickMs": 2000,
      "tabFocusedDuringClicks": true
    }
  }
}
```

#### Response

```json
{
  "received": true,
  "vqaLevel": 1
}
```

- `vqaLevel`: `1` | `2` | `3` | `4` (아래 VQA 레벨 참고)
- `drawResult`: `draw-application` 페이지에서만 반환 (`"pass"` | `"fail"`)
- 에러/검증 실패 시: `{ "received": true, "vqaLevel": 1 }`

---

## VQA 레벨 (4단계)

### 도메인 분리 (Two-Track)

이벤트 참여 방식에 따라 VQA 노출 정책이 다르다.

- **Track A. 더치경매 (100% 의무 VQA):** 스나이핑 방어를 위해 모든 참여자에게 최소 Level 2 이상 강제. Pass 구간 없음.
- **Track B. 일반 드로우 (위험도 기반 적응형 VQA):** 정상 유저의 UX 마찰 최소화를 위해 위험도에 따라 선별 노출.

### 레벨별 정책

| 레벨 | 위험도 | 점수 | Track A (더치경매) | Track B (드로우) |
|------|--------|------|-------------------|-----------------|
| **Level 1** | 안전 | 0 ~ 20 | Level 2로 상향 (Pass 없음) | VQA 면제 |
| **Level 2** | 주의 | 21 ~ 50 | 단답형 VQA | 단답형 VQA |
| **Level 3** | 경고 | 51 ~ 80 | 주관식 VQA | 주관식 VQA |
| **Level 4** | 위험 | 81+ | 인터랙션 VQA | 인터랙션 VQA |

> anti-macro는 점수 기반 `vqaLevel`만 반환한다. Track A의 Level 1→2 상향은 **Next 서버 또는 프론트**에서 처리한다.

### VQA 유형 상세

| 레벨 | 유형 | 문제 예시 | 정답 판정 | AI 정답률 |
|------|------|-----------|-----------|-----------|
| Level 1 | Pass | 퀴즈 없이 즉시 진입 | 백그라운드 통과 | - |
| Level 2 | 단답형 (객체 인식, 시간 순서 추적) | "건전지 없는 리모컨에서 빠진 것?" → '건전지' | 키워드 매칭 | 40~90% |
| Level 3 | 주관식 (상식/인과관계 추론) | "포크로 국물 먹기가 부적합한 이유?" → '구멍' | 코사인 유사도 (0.85+) | 20~40% |
| Level 4 | 인터랙션 (다단계 추론, 우선순위 판단) | "울리는 폰 vs 전기 스파크 중 더 위험한 상황?" | 유사도 매칭 / 좌표 클릭 | 10~20% |

> VQA 정답 검증은 **별도 VQA 서비스**에서 처리한다.

### 사일런트드롭

- 점수 > 50 (Level 3 이상) → 사일런트드롭 대상
- Next 서버에서 `vqaLevel`로 판단: Level 1~2 → 정상 처리, Level 3~4 → 사일런트드롭
- 200 성공 응답을 반환하되 실제 백엔드 로직(입찰/드로우)은 실행하지 않음

---

## 점수 체계

### 시그널 & 가중치

| 시그널 | 등급 | 가중치 | 설명 |
|--------|------|--------|------|
| `webdriver_detected` | hard | 50 | Selenium/webdriver 감지 |
| `untrusted_event` | hard | 50 | isTrusted=false 이벤트 |
| `honeypot_triggered` | hard | 40 | 허니팟 필드 입력됨 |
| `click_speed_inhuman` | hard | 40 | 클릭 간격 < 50ms |
| `zero_mouse_touch_events` | hard | 30 | 마우스 이동 0건 |
| `click_button_center` | medium | 20 | 80%+ 클릭이 버튼 정중앙 |
| `click_interval_uniform` | medium | 15 | 클릭 간격 표준편차 < 20ms |
| `abnormal_webgl` | medium | 15 | WebGL 렌더러 의심 |
| `ua_touch_mismatch` | medium | 15 | UA/터치 불일치 |
| `timezone_language_mismatch` | medium | 10 | 타임존 != Asia/Seoul |
| `fast_load_to_click` | soft | 5 | 페이지 로드 후 200ms 내 클릭 |
| `tab_not_focused` | soft | 5 | 클릭 중 탭 비활성 |
| `low_fingerprint_confidence` | soft | 5 | fingerprint confidence < 0.3 |
| `non_korean_language` | soft | 3 | 언어가 ko/en이 아님 |

- 점수 상한(cap) 없음. 81점 이상은 모두 Level 4.
- 점수는 Redis에서 페이지별로 합산되어 누적됨.

---

## Redis 점수 구조

```
Key:    score:{userId 또는 visitorId}
Type:   Hash
TTL:    30분 (1800초)
Fields: { "login": "15", "dutch-auction-detail": "20", "dutch-auction-application": "0", ... }
```

- 각 페이지별 점수가 별도 필드로 저장
- 총점 = 모든 필드 값의 합
- visitorId → userId 점수 병합: 둘 다 전송 시 visitorId 점수가 userId로 복사됨

---

## 프론트엔드(Next 서버) 연동 흐름

### 더치경매 상세 페이지

```
1. 브라우저: 행동 데이터 수집 (클릭, 마우스, fingerprint 등)
2. 브라우저 → Next API Route: 행동 데이터 전송
3. Next 서버 → POST /anti-macro/signals (page: "dutch-auction-detail")
4. Next 서버 ← 응답 (vqaLevel)
5. vqaLevel 확인 (Track A이므로 Level 1이어도 최소 Level 2 VQA 적용)
6. 대기열이 있으면 대기열 진입 → 대기열 완료 후 VQA 노출
7. 대기열이 없으면 즉시 VQA 노출
8. VQA 통과 시 → 신청 페이지로 이동
```

### 더치경매 신청(입찰) 페이지

```
1. 브라우저: 행동 데이터 수집
2. 브라우저 → Next API Route: 행동 데이터 + 입찰 데이터 함께 전송
3. Next 서버 → POST /anti-macro/signals (page: "dutch-auction-application")
4. Next 서버 ← 응답 (vqaLevel)
5-A. vqaLevel >= 3 (사일런트드롭):
     → Next 서버 → 브라우저: 200 성공 응답 (실제 입찰 API 미호출)
     → 사용자는 성공으로 인식하지만 실제 입찰은 미처리
5-B. vqaLevel <= 2 (정상):
     → Next 서버 → POST /auctions/bids (실제 입찰 API 호출)
     → Next 서버 → 브라우저: 실제 결과 응답
```

### 일반 드로우 상세 페이지

```
1. 브라우저: 행동 데이터 수집
2. 브라우저 → Next API Route: 행동 데이터 전송
3. Next 서버 → POST /anti-macro/signals (page: "popup-detail")
4. Next 서버 ← 응답 (vqaLevel)
5. Track B이므로 vqaLevel 1이면 VQA 면제
6. vqaLevel 2 이상이면 해당 레벨 VQA 노출
```

---

## 주의사항

1. **visitorId / userId 전송 규칙**
   - 로그인 전: `visitorId`만 전송
   - 로그인 후: `visitorId` + `userId` 둘 다 전송 (점수 병합을 위해)

2. **수집 타이밍**
   - 상세 페이지: 버튼 클릭 시점에 수집 → anti-macro 호출 → (대기열 → ) VQA 표시
   - 신청 페이지: 최종 제출 버튼 클릭 시점에 수집 → anti-macro 호출 → 사일런트드롭 or 실제 신청

3. **anti-macro 장애 시**
   - 에러/타임아웃 시 → 정상 처리로 fallback (차단하지 않음)
   - anti-macro 내부 에러 시 `{ received: true, vqaLevel: 1 }` 반환

4. **Track A Level 상향**
   - anti-macro는 점수 기반 vqaLevel만 반환
   - 더치경매의 Level 1 → Level 2 상향 로직은 Next 서버/프론트에서 처리
