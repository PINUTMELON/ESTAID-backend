# ESTAID — AI 기반 2차 창작 컨텐츠 생성 플랫폼

> **AI Design Company ESTAID** | 2026.03

---

## 프로젝트 소개

**ESTAID**는 K-pop, 애니, 게임, 버튜버 등 2차 창작 컨텐츠를 누구나 쉽게 만들 수 있도록 돕는 AI 기반 플랫폼입니다.

복잡한 프롬프트 지식 없이도 아이디어 하나만으로 **플롯 기획 → 이미지 생성 → 영상 제작**까지 하나의 플로우로 완성할 수 있습니다.

---

## 해결하는 문제

| 대상 | 문제 |
|---|---|
| **입문자** | 플롯 기획 개념 없음 (구도 / 조명 / 캐릭터 / 분위기 등) |
| **입문자** | 시작 프레임과 종료 프레임 개념을 몰라 영상 제작 불가 |
| **경험자** | 기획 / 이미지 제작 / 영상 제작 도구를 동시에 써야 하는 번거로움 |
| **경험자** | 결과물이 뽑기 수준 — 반복 작업을 줄이고 싶음 |

---

## 핵심 기능

### 필수 기능

#### 1. AI 플롯 기획 `idea → text`
- 짧은 키워드나 문장을 입력하면 AI가 5~10개 씬으로 구성된 플롯을 자동 생성
- 각 씬별로 **등장인물 / 구도 / 배경 / 조명 / 분위기 / 카메라 움직임 / 주요 스토리** 포함
- 생성된 표를 수정하여 원하는 방향으로 커스터마이징 가능

```
예시 입력: "주인공이 괴물과 싸우는 장면" / 5씬

→ Scene 1: 괴물의 등장  — 와이드 샷, 폐허가 된 고대 도시, 어두운 하늘과 번개
→ Scene 2: 첫 공격      — 미디엄 액션 샷, 번개와 불꽃이 번쩍이는 조명
→ Scene 3: 격렬한 전투  — 빠른 액션 클로즈샷, 불꽃과 먼지 속 강한 대비
→ Scene 4: 위기의 순간  — 로우 앵글, 붉은 빛과 먼지로 어두운 분위기
→ Scene 5: 최종 반격    — 영웅적 클로즈업 → 와이드 샷, 새벽빛
```

#### 2. AI 이미지 생성 `text → image`
- 플롯 표를 기반으로 각 씬의 **첫 프레임 / 마지막 프레임** 이미지를 자동 생성
- 캐릭터 레퍼런스 이미지를 등록하면 컷씬 간 **외형 및 화풍 일관성** 유지
- 복잡한 프롬프트 없이 원클릭으로 고품질 이미지 생성

#### 3. AI 영상 생성 `image → video`
- 각 씬의 첫 프레임과 마지막 프레임을 연결하여 **3~5초 씬별 영상** 제작
- 카메라 움직임이 반영된 자연스러운 영상 생성
- 씬 영상들을 연결하여 하나의 mp4 / gif로 출력

### 가산 기능

#### 4. 영상 병합 및 편집
- 씬별 영상을 매끄럽게 병합하여 **15~30초 최종 영상** 제작
- 유튜브, 틱톡 등 숏폼 플랫폼에 최적화된 편집 기능

---

## 기술 스택

| 영역 | 기술 |
|---|---|
| **Frontend** | Next.js 14, TypeScript, Tailwind CSS |
| **Backend** | Spring Boot 3.2, Java 17, Spring Data JPA |
| **AI (플롯 생성)** | Claude API (Anthropic) |
| **Database** | H2 (개발) / MySQL (운영) |
| **CI/CD** | GitHub Actions + Gemini AI 자동 코드리뷰 |

---

## 프로젝트 구조

```
ESTAID/
├── backend/                  # Spring Boot 백엔드
│   └── src/main/java/com/estaid/
│       ├── common/
│       │   ├── config/       # CORS, Security, Claude API 설정
│       │   ├── exception/    # 전역 예외 처리
│       │   └── response/     # 표준 응답 래퍼
│       └── domain/
│           ├── character/    # 캐릭터 등록 및 조회
│           ├── plot/         # AI 플롯 생성
│           ├── image/        # AI 이미지 생성
│           └── video/        # AI 영상 생성 및 병합
│
└── frontend/                 # Next.js 프론트엔드
    └── src/
        ├── app/
        │   ├── character/    # 캐릭터 등록 페이지
        │   ├── plot/         # 플롯 기획 페이지
        │   ├── image/        # 이미지 생성 페이지
        │   └── video/        # 영상 생성 페이지
        ├── lib/api.ts        # 백엔드 API 클라이언트
        └── types/index.ts    # TypeScript 타입 정의
```

---

## 로컬 실행 방법

### 백엔드

```bash
cd backend
./mvnw spring-boot:run
```

> 기본 포트: `http://localhost:8080`
> 개발 환경은 H2 인메모리 DB 자동 사용 (`/h2-console` 접근 가능)

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

> 기본 포트: `http://localhost:3000`

### 환경 변수 설정

**백엔드** — `backend/src/main/resources/application.yml`
```yaml
claude:
  api:
    key: ${CLAUDE_API_KEY}   # Anthropic API 키 입력
```

**프론트엔드** — `frontend/.env.local`
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## API 엔드포인트

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/api/characters` | 캐릭터 등록 |
| `GET` | `/api/characters/{id}` | 캐릭터 조회 |
| `POST` | `/api/plots` | AI 플롯 생성 |
| `GET` | `/api/plots/{id}` | 플롯 조회 |
| `POST` | `/api/images/generate` | 이미지 생성 요청 |
| `GET` | `/api/images/plot/{plotId}` | 플롯 내 이미지 전체 조회 |
| `POST` | `/api/videos/generate` | 영상 생성 요청 |
| `POST` | `/api/videos/merge` | 영상 병합 |
| `GET` | `/api/videos/plot/{plotId}` | 플롯 내 영상 전체 조회 |

---

## 브랜치 전략

| 브랜치 | 용도 |
|---|---|
| `main` | 배포용 최신 코드 |
| `master` | 프로젝트 초기 세팅 기준 |
| `Backend` | 백엔드 개발 |
| `Be-development` | 백엔드 기능 개발 |
| `frontend` | 프론트엔드 개발 |
| `fe-development` | 프론트엔드 기능 개발 |

---

## GitHub Actions

PR 생성, 이슈 등록, main 브랜치 push 시 **Gemini AI가 자동으로** 코드리뷰 및 분석을 수행합니다.

활성화하려면 GitHub 레포 **Settings → Secrets → Actions**에 아래 키를 등록하세요.

```
GEMINI_API_KEY=your-google-ai-studio-key
```
