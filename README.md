# ESTAID Backend

> AI 기반 2차 창작 컨텐츠 생성 플랫폼 **ESTAID**의 백엔드 서버

2차 창작 생산자가 복잡한 프롬프팅 없이 **플롯 기획 → 이미지 생성 → 영상 생성** 전 과정을 하나의 플랫폼에서 완성할 수 있도록 지원합니다.

---

## 목차

- [프로젝트 개요](#프로젝트-개요)
- [기술 스택](#기술-스택)
- [주요 기능](#주요-기능)
- [프로젝트 구조](#프로젝트-구조)
- [DB 스키마](#db-스키마)
- [환경 설정](#환경-설정)
- [실행 방법](#실행-방법)
- [개발 계획](#개발-계획)
- [브랜치 전략](#브랜치-전략)

---

## 프로젝트 개요

| 항목 | 내용 |
|------|------|
| 프로젝트명 | ESTAID |
| 분류 | AI Design Company |
| 개발 기간 | 2026.03 (해커톤) |
| 서버 포트 | 8080 |

### 해결하는 문제

**입문자**
- 플롯 기획 개념 없음 (구도 / 조명 / 캐릭터 / 분위기 등)
- 시작 프레임과 종료 프레임 개념 없음

**경험자**
- 기획 / 이미지 제작 / 영상 제작 도구를 동시에 사용해야 하는 불편함
- 반복적인 결과물 뽑기 작업

---

## 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Build Tool | Maven |
| Database | PostgreSQL (Supabase) |
| ORM | Spring Data JPA / Hibernate 6 |
| AI | Claude API (claude-opus-4-6) |
| HTTP Client | WebClient (Spring WebFlux) |
| Security | Spring Security |
| Validation | Spring Validation |
| Util | Lombok |

---

## 주요 기능

### 필수 기능

#### 1. AI 플롯 기획 `idea → text`
- 사용자가 입력한 짧은 키워드나 문장을 분석
- **5~10개** 컷으로 구성된 플롯 자동 생성
- 각 컷별로 등장인물 / 구도 / 배경 / 조명 / 주요 스토리 포함

#### 2. AI 이미지 생성 `text → image`
- 생성된 플롯 기반 컷씬별 이미지 생성
- 컷씬 간 캐릭터 외형 및 화풍(Art Style) **일관성 유지**
- 복잡한 프롬프트 없이 원클릭으로 고품질 이미지 생성

#### 3. AI 영상 생성 `image → video`
- Start 프레임 / End 프레임 기반 **3~5초** 컷별 영상 생성
- 각 컷을 연결하여 mp4 영상으로 생성

### 가산 기능 (택 1)

#### 4. 영상 병합 및 편집
- 짧은 영상들을 매끄럽게 병합
- 유튜브 / 틱톡 숏폼에 적합한 **15~30초** 최종 영상 제작

#### 5. 창작자 보상 체계
- 서비스 고유의 반응 지표 구현
- 실시간 / 주간 인기 창작자 랭킹 보드
- 활동 성과 기반 뱃지 / 등급 부여

---

## 프로젝트 구조

```
src/main/java/com/estaid/
├── EstaidApplication.java              # 앱 진입점
└── common/                             # 공통 인프라
    ├── config/
    │   ├── ClaudeConfig.java           # Claude API 설정
    │   ├── CorsConfig.java             # CORS 설정
    │   └── SecurityConfig.java         # Spring Security 설정
    ├── exception/
    │   ├── BusinessException.java      # 비즈니스 예외
    │   └── GlobalExceptionHandler.java # 전역 예외 핸들러
    └── response/
        └── ApiResponse.java            # 공통 응답 포맷

src/main/resources/
├── application.yml                     # 앱 설정 (환경변수 기반)
└── db/
    └── schema.sql                      # Supabase DDL
```

---

## DB 스키마

```
characters   캐릭터 정보 (레퍼런스 이미지, 화풍)
    │
    └── plots        플롯 (AI 생성 씬 목록 JSON)
            │
            └── images   씬별 이미지 (FIRST / LAST 프레임)
                    │
                    └── videos   씬별 영상 / 병합 영상
```

> 스키마 전체 DDL: `src/main/resources/db/schema.sql`
>
> **Supabase 최초 실행 시** Dashboard → SQL Editor에서 실행 필요

---

## 환경 설정

프로젝트 루트에 `.env` 파일 생성 후 아래 값 입력 (`.gitignore`에 등록되어 있어 커밋되지 않음)

```env
# Supabase DB 연결 정보
# Supabase Dashboard > Settings > Database > Connection string 에서 확인
SUPABASE_DB_URL=jdbc:postgresql://db.xxxxxxxxxxxx.supabase.co:5432/postgres
SUPABASE_DB_USERNAME=postgres
SUPABASE_DB_PASSWORD=your_password

# Claude API 키
# https://console.anthropic.com > API Keys 에서 발급
CLAUDE_API_KEY=your_claude_api_key
```

> Spring Boot은 `.env` 파일을 기본적으로 읽지 않으므로 `spring-dotenv` 라이브러리가 자동으로 로드합니다.

---

## 실행 방법

### 사전 조건
- Java 17 이상
- Maven
- Supabase 프로젝트 생성 및 `schema.sql` 실행 완료
- `.env` 파일 작성 완료

### 1. Supabase 스키마 실행
Supabase Dashboard → SQL Editor → `src/main/resources/db/schema.sql` 내용 붙여넣기 → Run

### 2. 의존성 설치 및 실행
```bash
# STS / IntelliJ에서 실행하거나
mvn spring-boot:run

# 또는 빌드 후 실행
mvn clean package
java -jar target/estaid-backend-0.0.1-SNAPSHOT.jar
```

### 3. 정상 실행 확인
```
HikariPool-1 - Start completed.
Started EstaidApplication in X.XXX seconds
```

---

## 개발 계획

### Phase 1 - 기반 세팅 ✅
- [x] Spring Boot 3.2 프로젝트 세팅
- [x] Supabase(PostgreSQL) 연동
- [x] `schema.sql` DDL 작성
- [x] 공통 인프라 구성 (예외처리, 응답포맷, CORS, Security)
- [x] `.env` 기반 환경변수 관리

### Phase 2 - 핵심 도메인 개발 🔄
- [ ] Character API (캐릭터 등록 / 조회)
- [ ] Plot API (아이디어 입력 → Claude API → 플롯 생성)
- [ ] Image API (플롯 기반 이미지 생성 요청 / 상태 관리)
- [ ] Video API (이미지 기반 영상 생성 요청 / 상태 관리)

### Phase 3 - 가산 기능 개발
- [ ] 영상 병합 API (씬 영상 → 최종 영상)
- [ ] 창작자 보상 체계 (랭킹, 뱃지)

---

## 브랜치 전략

| 브랜치 | 역할 |
|--------|------|
| `master` | 배포 브랜치 |
| `Development` | 통합 개발 브랜치 |
| `Dev_CCL` | 조창래 개발 브랜치 |
| `Dev_KYM` | 김영민 개발 브랜치 |
