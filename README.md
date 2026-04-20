# 🍿 Pop-Con Backend

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-brightgreen?logo=springboot)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?logo=mysql)
![Redis](https://img.shields.io/badge/Redis-Cluster-red?logo=redis)

<br/>

## 📌 프로젝트 소개

**팝콘(POP-CON)** 은 인기 팝업스토어 입장권을 드로우(추첨)와 경매 두 가지 방식으로 예약할 수 있는 웹 서비스입니다.

<br/>

대규모 동시 접속 환경에서 **트래픽 폭주를 안정적으로 제어**하는 것이 핵심 목표로,<br/>
드로우·경매 오픈 시 발생하는 스파이크 트래픽을 Redis 기반 대기열과 분산 처리로 수용하고,<br/>
매크로 차단과 동시성 제어를 통해 실사용자에게 공정한 기회를 보장합니다.

<br/>

## 👥 팀원

|                           백엔드                              |                            백엔드                            |                              풀스택                              |
|:----------------------------------------------------------:|:---------------------------------------------------------:|:-------------------------------------------------------------:|
| <img src="https://github.com/son2304.png" width="130px" /> | <img src="https://github.com/youngyii.png" width="130px" /> | <img src="https://github.com/Kimmingi1127.png" width="130px" /> |
| 손무경 <br/> [@son2304](https://github.com/son2304) | 이신영 <br/> [@youngyii](https://github.com/youngyii) | 김민기 <br/> [@Kimmingi1127](https://github.com/Kimmingi1127) |

<br/>

## 🏗️ 시스템 아키텍처

<img width="1300" height="1254" alt="image" src="https://github.com/user-attachments/assets/72e11595-840f-40ff-aac9-b59343ca3038" />

<br/>

## 📦 모듈 구조

```
pop-con-backend/
├── auth-service        # JWT 발급, OAuth 소셜 로그인, 본인인증
├── user-service        # 회원 정보, 빌링키, 마이페이지
├── popup-service       # 팝업 스토어 조회, 좋아요
├── draw-service        # 응모 접수, 당첨 확인
├── auction-service     # 실시간 경매 입찰 (SSE)
├── ticket-service      # 티켓 발급 및 관리
├── queue-service       # 대기열 진입, 실시간 폴링, VQA 인증
├── queue-worker        # 대기열 승격 · 만료 처리 스케줄러
├── queue-common        # 대기열 공통 Redis 레포지토리 (라이브러리)
├── common              # 공통 예외/응답, JWT, PortOne 클라이언트 (라이브러리)
└── anti-macro-service  # 봇 방지 · 매크로 감지 (VQA 기반, Node.js)
```

<br/>

## 🛠️ 기술 스택

### Backend
| 기술 | 선택 이유 |
|------|----------|
| **Java 21 + Spring Boot 3.5.5** | 최신 Java 성능 활용, 안정적인 MSA 서버 구축 |
| **Gradle 멀티모듈** | 도메인별 독립 모듈 분리, 관심사 분리 및 의존성 관리 |
| **Spring Cloud OpenFeign** | 선언적 인터페이스 기반 서비스 간 HTTP 통신 |
| **MySQL 8.0 + JPA + QueryDSL** | 트랜잭션 안정성, 타입 안전한 동적 쿼리 |
| **Redis + Redisson** | 대기열·세션 인메모리 처리, 분산 락 동시성 제어 |
| **JWT + Spring Security** | Stateless 토큰 기반 인증/인가 |

### Infrastructure
> AWS EKS · ArgoCD · Istio · GitHub Actions · Prometheus + Grafana · Cloudflare · AWS ALB

<br/>

## ✨ 핵심 기능

| 기능 | 설명 |
|------|------|
| **소셜 로그인** | 카카오 / 네이버 OAuth 2.0, JWT 인증 |
| **본인인증 / 결제** | PortOne V2 (본인인증 + 빌링키 결제) |
| **팝업 탐색** | 카테고리, 랭킹, 추천, 마감 임박, 좋아요 |
| **대기열** | Redis ZSet 순번 대기 → VQA 봇 방지 → 도메인 API 접근 |
| **응모 (드로우)** | 대기열 통과 후 응모 → 스케줄러 자동 추첨 |
| **경매** | SSE 실시간 가격 변동, 즉시 입찰 |
| **티켓 발급** | 당첨 / 낙찰 시 디지털 티켓 발급 |

<br/>

## 🚀 로컬 실행 방법

**사전 요구사항:** JDK 21, Docker

```bash
# 1. 저장소 클론
git clone https://github.com/kt-cloud-TECHUP-T1/pop-con-backend.git
cd pop-con-backend

# 2. 로컬 인프라 실행 (MySQL, Redis)
docker-compose up -d

# 3. 환경 변수 설정 (팀 노션 참고)

# 4. 특정 서비스 실행
./gradlew :auth-service:bootRun
```

<br/>

## 🤝 협업 컨벤션

### 브랜치 전략
| 브랜치 | 용도 |
|--------|------|
| `main` | 프로덕션 배포 |
| `staging` | 스테이징 배포 |
| `dev` | 개발 통합 브랜치 |
| `feat/이슈명` | 기능 개발 |
| `fix/이슈명` | 버그 수정 |

### 커밋 메시지
| 태그 | 설명 |
|------|------|
| `feat` | 새로운 기능 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `refactor` | 리팩토링 |
| `test` | 테스트 코드 |
| `chore` | 빌드 · 설정 변경 |
