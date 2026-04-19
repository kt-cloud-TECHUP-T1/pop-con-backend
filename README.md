# 🍿 Pop-Con Backend

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.2-green) ![MySQL](https://img.shields.io/badge/MySQL-8.0-blue) ![Redis](https://img.shields.io/badge/Redis-Distributed%20Lock-red)


## 💁‍♂️ 프로젝트 팀원


|                           백엔드                              |                            백엔드                            |                              풀스택                              |
|:----------------------------------------------------------:|:---------------------------------------------------------:|:-------------------------------------------------------------:|
| <img src="https://github.com/son2304.png" width="130px" /> | <img src="https://github.com/youngyii.png" width="130px" /> | <img src="https://github.com/Kimmingi1127.png" width="130px" /> |
|      손무경 <br/> [@son2304](https://github.com/son2304)      |         이신영 <br/> [@youngyii](https://github.com/youngyii)          |    김민기  <br/> [@Kimmingi1127](https://github.com/Kimmingi1127)    |



<br/>

## 🏗️ 프로젝트 아키텍처 (Project Architecture)
<img width="1300" height="1254" alt="image" src="https://github.com/user-attachments/assets/72e11595-840f-40ff-aac9-b59343ca3038" />

<br/>

## 🛠️ 기술 스택 (Tech Stack)

### Backend
- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 4.0.2
- **Database:** MySQL 8.0, Redis (Cache & Lock)
- **ORM:** Spring Data JPA, QueryDSL
- **Test:** JUnit5, Mockito

### Infrastructure (Planned)
- **CI/CD:** GitHub Actions
- **Deploy:** AWS EC2, Docker
- **Monitoring:** Prometheus, Grafana

<br/>

## 🚀 실행 방법 (Getting Started)

이 프로젝트를 로컬 환경에서 실행하는 방법입니다.

### 사전 요구사항 (Prerequisites)
* JDK 21 이상
* MySQL, Redis (Docker 실행 권장)

### Run
```bash
# 1. 저장소 클론
git clone [https://github.com/kt-cloud-TECHUP-T1/pop-con-backend.git](https://github.com/조직명/pop-con-backend.git)

# 2. 프로젝트 이동
cd pop-con-backend

# 3. 환경 변수 설정 (팀 노션 참고)

# 4. 빌드 및 실행
./gradlew bootRun
```

## 🤝 협업 컨벤션 (Convention)
### **브랜치 전략 (Git Flow)**
* main: 배포 가능한 안정 버전 (Production)
* dev: 다음 배포를 위한 개발 브랜치
* feat/기능명(이슈명): 새로운 기능 개발 (예: feat/login)
* fix/기능명(이슈명): 버그 수정 (예: fix/inventory-error)

## 📝 커밋 메시지 규칙
* feat: 새로운 기능 추가
* fix: 버그 수정
* docs: 문서 수정 (README, Swagger 등)
* test: 테스트 코드 추가
* refactor: 코드 리팩토링 (기능 변경 없음)
* chore: 빌드, 패키지 매니저 설정 변경
