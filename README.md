<div align="center">

<h1>Otboo — 옷장을 부탁해 👗🌤️</h1>

<p>
<b>날씨(기상청)</b>과 <b>개인 취향/보유 의상</b>을 결합해 상황별 <b>설명 가능한 코디 추천</b>을 제공하는 서비스
</p>

<small>개발 기간: 2025-09-09 ~ 2025-10-24</small>

</div>

<p align="center">
  <a href="#"><img alt="Java" src="https://img.shields.io/badge/Java-17%2B-informational"></a>
  <a href="#"><img alt="Spring Boot" src="https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen"></a>
  <a href="#"><img alt="Gradle" src="https://img.shields.io/badge/Gradle-8.x-blue"></a>
  <a href="#"><img alt="AWS" src="https://img.shields.io/badge/Deploy-AWS%20ECS%20%7C%20RDS%20%7C%20S3%20%7C%20ElastiCache-orange"></a>
  <a href="#"><img alt="License" src="https://img.shields.io/badge/License-MIT-lightgrey"></a>
</p>

---

## 목차
- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [담당 역할(RnR)](#담당-역할rnr)
- [사용 기술 스택](#사용-기술-스택)
- [배포 링크](#배포-링크)
- [스크린샷·시연 영상](#스크린샷시연-영상)
- [환경 변수](#환경-변수)
- [프로젝트 구조](#프로젝트-구조)
- [아키텍처](#아키텍처)
- [회고(배운-점개선점)](#회고배운점-개선점)

---

## 프로젝트 개요
Otboo는 사용자의 **보유 의상 + 날씨 + 취향**을 바탕으로 상황에 맞는 **코디 조합을 자동 추천**합니다.  
추천 결과는 **점수(Score)와 사유(Reason)**를 함께 제공하여 신뢰성과 설명 가능성을 높였습니다.  
또한 **OOTD 피드·댓글/좋아요·팔로우·알림(SSE/WebSocket)** 등 소셜 기능을 제공합니다.

---

## 주요 기능
- **개인화 코디 추천**
  - 기온/강수/풍속을 고려한 **체감 온도(ET)** 기반 점수화
  - 계절/두께/레이어링/스타일/재질 등 **가중치 기반 조합 최적화**
  - 각 코디에 **추천 점수 & 이유** 제공
- **옷장 관리**
  - 의류/속성 CRUD, 스타일·색상·두께 등 메타 속성 부여
  - 중복/무결성 제약, 조회 최적화 인덱스 설계
- **피드 & 소셜**
  - OOTD 업로드, 댓글/좋아요
  - 팔로우/언팔로우, **실시간 알림(SSE·WebSocket)**
- **인증/보안**
  - OAuth2 로그인 + **JWT** 발급/리프레시
- **운영/배포**
  - Docker 이미지화, **AWS ECS(Fargate)** 배포, **RDS(PostgreSQL)**, **S3**, (옵션) **ElastiCache/Redis**
  - GitHub Actions CI/CD, 로그/메트릭 수집

---

## 담당 역할(RnR)
> 팀원 이름/닉네임을 채워 넣어 주세요. (요청 주시면 제가 반영해 드립니다.)

| 역할 | 이름 | 주요 담당 | 상세 |
|---|---|---|---|
| 팀장/PM | 이주용  | 의류관리/의류정의&속성/의류추천시스템 | 일정 관리, 스프린트/스크럼 운영, 목표 설정, 위험/의사결정 관리 |
| 팀원/형상관리 | 정윤지  | 사용자/팔로우/알림 | OAuth2·JWT, 팔로우/언팔로우, 사용자관리, SMTP 메일링 |
| 팀원/회의록관리 | 고희준  | 피드/댓글/좋아요/날씨 | 피드 CRUD, 댓글/좋아요, 기상청/Kakao Local 연동, 검색 |
| 팀원/인프라&DB | 강호  | 팔로우,DM,알림 | AWS(ECS/RDS/S3) 인프라, 로그/메트릭 |

> 프로젝트 노션: https://ohgiraffers.notion.site/2-207649136c11805ba5c0db995da95c51?pvs=74

---

## 사용 기술 스택
- **Language/Framework**: Java 17, Spring Boot 3.5.5, Spring Web, Spring Data JPA, Validation, Actuator  
- **DB/Cache**: PostgreSQL(RDS), Redis, Caffeine  
- **Infra/DevOps**: Docker, AWS ECS, NGNIX S3, CloudWatch, DNS  
- **Build**: Gradle 8.x, QueryDSL  
- **Realtime**: WebSocket(STOMP), SSE  
- **Auth**: OAuth2, JWT 
- **External**: KMA(기상청) API, Kakao Local API, OpenAI API

---

## 배포 링크
- **서비스 (Prod)**: https://ikuzo.duckdns.org/  
- **Swagger UI**: https://ikuzo.duckdns.org/swagger-ui/index.html

---

## 스크린샷·시연 영상
[시연영상 보려가기](https://drive.google.com/file/d/1U_vjqKZW9kpxipU8Od90ru9OwPeNoIfW/view?usp=sharing)

## 환경 변수
| 키 | 예시 | 설명 |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` | 실행 프로파일 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` |  | RDS(PostgreSQL) 연결 |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_SSL` |  | ElastiCache Redis 연결 |
| `S3_BUCKET` / `AWS_REGION` |  | 이미지 저장 버킷/리전 |
| `JWT_SECRET` |  | JWT 서명 시크릿 |
| `KMA_KEY` / `KAKAO_REST_KEY` |  | 외부 API |
| `OPENAI_API_KEY` |  | 구매링크로 의상불러오기 기능 |
| `MAIL_USERNAME` / `MAIL_PASSWORD` |  | SMTP 발송 |

---

## 프로젝트 구조
```text
com.example.myproject
├── global
│   ├── config
│   │   └── SwaggerConfig.java
│   ├── exception
│   │   └── GlobalExceptionHandler.java
│   ├── dto            # PageResponse, ErrorResponse 등
│   ├── util           # DateUtil, StringUtil 등
│   └── base           # BaseEntity, 공통 응답 등
├── domain
│   ├── user
│   │   ├── controller
│   │   │   ├── api
│   │   │   └── UserController.java
│   │   ├── service
│   │   ├── repository
│   │   ├── entity
│   │   ├── mapper
│   │   └── dto
│   │       ├── data
│   │       └── request
│   └── notification
│       ├── controller
│       ├── service
│       ├── repository
│       ├── entity
│       ├── mapper
│       └── dto
└── Application.java
```

## 아키텍처
- **클린 레이어링**

 - Controller → Service → Repository (도메인 중심)

- **성능 최적화**

  - 사용자별 최신 의류 조회를 위한 복합 인덱스: (owner_id, created_at DESC)

  - 의류-속성 조회 가속: clothes_id / definition_id 인덱스

  - 읽기 위주 경로는 Cache(Caffeine ↔ Redis 승격) 활용

- **실시간성**

  - SSE/웹소켓(STOMP) 기반 알림

  - 확장/운영

- **이미지 저장: S3**

- **배포: Docker → ECS, ngnix, CloudWatch 로깅/메트릭**

- **CI/CD: GitHub Actions**

## 회고배운점-개선점
- **배운 점**

  - 계절/스타일/레이어링/재질/두께를 수치화해 설명 가능한 추천으로 연결하는 방법

  - 컬렉션 fetch join 제약을 우회하는 안전한 페이징 패턴

  - ECS 작업 정의 분리의 중요성과 스케일링 설계 팁

  - TLS/쿠키 보안 속성 운영 노하우

- **개선점**

  - 추천 가중치 A/B 테스트와 사용자 피드백 루프 고도화

  - 캐시 계층(Caffeine ↔ Redis) 정책 정교화 및 장애 내성 강화

  - S3 이미지 리사이즈/웹최적화 파이프라인 자동화
