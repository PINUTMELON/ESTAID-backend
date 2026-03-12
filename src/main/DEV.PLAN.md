# ESTAID Backend 개발 계획서

> 작성일: 2026-03-12
> 브랜치 전략: `master` (배포) → `Development` (통합) → `Dev_CCL`, `Dev_KYM` (개발)

---

## 1. 현재 상태 요약

| 레이어 | 상태 | 설명 |
|--------|------|------|
| 공통 인프라 | ✅ 완료 | Config, Exception, ApiResponse |
| DB 스키마 | ✅ 완료 | characters, plots, images, videos |
| 도메인 엔티티 | ✅ 완료 | JPA Entity 4개 (Character, Plot, Image, Video) |
| Repository | ✅ 완료 | Spring Data JPA Repository 4개 |
| Project 엔티티 | ❌ 미작성 | 작업공간 프로젝트 파일 관리 |
| Service | ❌ 미작성 | 비즈니스 로직 없음 |
| Controller | ❌ 미작성 | REST API 엔드포인트 없음 |
| Claude 연동 | ❌ 미작성 | WebClient 설정만 있음 |
| 이미지 생성 연동 | ❌ 미작성 | 외부 API 미결정 |
| 영상 생성 연동 | ❌ 미작성 | 외부 API 미결정 |

---

## 2. 서비스 개요

### 2-1. 두 가지 생성 모드

| 모드 | 대상 | 설명 | 개발 여부 |
|------|------|------|-----------|
| **선택지로 생성하기** | 초심자 | 플롯 구조로 영상 생성까지 단계별로 안내 | ✅ **현재 개발 대상** |
| 프롬프트로 생성하기 | 숙련자 | 이미지 장면 편집, 직접 프롬프트 입력 | ❌ 미계획 |

### 2-2. 작업공간 (Project) 개념

- 사용자가 생성한 영상마다 **프로젝트 파일**로 분리 관리
- 프로젝트에는 사용한 캐릭터, 플롯, 이미지, 영상 등 모든 작업 이력 저장
- 나중에 이어서 작업할 수 있도록 프롬프트 정보도 함께 저장

---

## 3. 선택지로 생성하기 — 전체 플로우

```
① 프로젝트 생성
        │  └── 프로젝트 제목 입력 (ex. "괴물과 싸우는 영상")
        │
        ▼
② 캐릭터 + 배경 생성
        │  ├── 참조 이미지 업로드 (캐릭터)
        │  ├── 스타일 템플릿 선택 (anime, realistic 등)
        │  ├── 캐릭터 프롬프트 작성 → 캐릭터 이미지 생성
        │  └── 배경 이미지 업로드 or 생성
        │
        ▼
③ 플롯(씬) 자동 생성  ← Claude API 호출
        │  ├── 씬 수 설정 (ex. 5개)
        │  ├── 사용자가 스토리 아이디어 입력
        │  └── AI가 N개의 씬 생성 → 표(테이블) 형태로 출력
        │       [씬번호 | 등장인물 | 구도 | 배경 | 조명 | 주요스토리 | 첫 프레임 | 마지막 프레임]
        │  ※ 사용자가 표에서 직접 텍스트 수정 가능
        │  ※ 구도 선택 시 GIF 템플릿으로 종류 안내
        │
        ▼
④ 플롯 → 이미지 프롬프트 변환  ← Claude API 호출
        │  ├── AI가 각 씬의 [구도, 배경, 조명, 등장인물, 주요스토리] → 영어 프롬프트 생성
        │  ├── 씬별 첫 프레임 프롬프트 + 마지막 프레임 프롬프트 생성
        │  └── 캐릭터/배경 이미지 + 프롬프트 → 씬 이미지 제작 (외부 이미지 AI)
        │
        ▼
⑤ 영상 생성  ← 외부 영상 AI 호출
        │  ├── AI가 씬의 첫/마지막 프레임 이미지를 연결하는 영상 프롬프트 생성
        │  ├── 영상 프롬프트 사용자 수정 가능 (입력창에 유지)
        │  └── 영상 생성 (3~5초 씬별 영상)
        │
        ▼
⑥ 영상 병합 (Phase 3)
        │  └── 씬 순서대로 concat → 최종 MP4 완성
        │
        ▼
    최종 결과물 다운로드 / 프로젝트 저장
```

---

## 4. 개발 우선순위 및 순서

```
Phase 2-A (도메인 기반)
  1. Project 엔티티 + DB 스키마 추가
  2. Project CRUD API
  3. Character CRUD API (배경 이미지 필드 포함)

Phase 2-B (AI 연동 - 선택지 플로우)
  4. Plot 생성 API (Claude 연동 — 씬 수 지정, 표 형태 응답)
  5. Plot 수정 API (사용자가 표에서 씬 내용 편집)
  6. Image 프롬프트 생성 API (Claude — 플롯 → 영어 이미지 프롬프트 변환)
  7. Image 생성 API (외부 이미지 AI 연동)
  8. Video 프롬프트 생성 API (Claude — 씬 이미지 → 영상 프롬프트 변환)
  9. Video 생성 API (외부 영상 AI 연동)

Phase 3 (부가 기능)
  10. 영상 병합 API
  11. 크리에이터 보상 시스템
  12. 프롬프트로 생성하기 모드 (별도 계획 필요)
```

---

## 5. 패키지 구조 설계

```
src/main/java/com/estaid/
├── EstaidApplication.java
├── common/
│   ├── config/          # ClaudeConfig, CorsConfig, SecurityConfig
│   ├── exception/       # BusinessException, GlobalExceptionHandler
│   └── response/        # ApiResponse
├── project/             # 작업공간 프로젝트 파일 관리
│   ├── Project.java
│   ├── ProjectRepository.java
│   ├── ProjectService.java
│   ├── ProjectController.java
│   └── dto/
│       ├── ProjectCreateRequest.java
│       └── ProjectResponse.java
├── character/           # 캐릭터 + 배경 이미지 관리
│   ├── Character.java
│   ├── CharacterRepository.java
│   ├── CharacterService.java
│   ├── CharacterController.java
│   └── dto/
│       ├── CharacterCreateRequest.java
│       ├── CharacterUpdateRequest.java
│       └── CharacterResponse.java
├── plot/                # 플롯(씬 목록) 관리 + 사용자 편집
│   ├── Plot.java
│   ├── PlotRepository.java
│   ├── PlotService.java
│   ├── PlotController.java
│   └── dto/
│       ├── PlotCreateRequest.java   # 스토리 아이디어 + 씬 수 입력
│       ├── SceneDto.java            # 씬 데이터 (등장인물, 구도, 배경, 조명 등)
│       ├── SceneUpdateRequest.java  # 사용자 씬 수정 요청
│       └── PlotResponse.java
├── image/               # 씬 이미지 생성 (첫/마지막 프레임)
│   ├── Image.java
│   ├── ImageRepository.java
│   ├── ImageService.java
│   ├── ImageController.java
│   └── dto/
│       ├── ImageGenerateRequest.java
│       └── ImageResponse.java
└── video/               # 씬 영상 생성 + 병합
    ├── Video.java
    ├── VideoRepository.java
    ├── VideoService.java
    ├── VideoController.java
    └── dto/
        ├── VideoGenerateRequest.java
        ├── VideoPromptUpdateRequest.java  # 영상 프롬프트 수정 요청
        └── VideoResponse.java
```

---

## 6. 엔티티 설계 (JPA Entity)

### 6-1. Project.java (신규)

```java
@Entity
@Table(name = "projects")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Project {

    /** 프로젝트 고유 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "project_id", length = 36)
    private String projectId;

    /** 프로젝트 제목 (ex. "괴물과 싸우는 영상") */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 프로젝트에서 사용하는 캐릭터 (선택) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    /** 생성일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 수정일시 */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
```

### 6-2. Character.java (수정 — 배경 이미지 필드 추가)

```java
@Entity
@Table(name = "characters")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Character {

    /** 캐릭터 고유 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "character_id", length = 36)
    private String characterId;

    /** 캐릭터 이름 */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /** 캐릭터 설명 */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 참조 이미지 URL (사용자가 업로드한 캐릭터 참조 이미지) */
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    /** 스타일 템플릿 (ex. anime, realistic, webtoon) */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /** 배경 이미지 URL (사용자가 업로드하거나 생성한 배경 이미지) */
    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    /** 생성일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 수정일시 */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
```

### 6-3. Plot.java (수정 — Project FK, 씬 수 필드 추가)

```java
@Entity
@Table(name = "plots")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Plot {

    /** 플롯 고유 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plot_id", length = 36)
    private String plotId;

    /** 소속 프로젝트 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** 플롯 제목 */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /** 사용자가 입력한 스토리 아이디어 */
    @Column(name = "idea", columnDefinition = "TEXT", nullable = false)
    private String idea;

    /** 사용자가 지정한 씬 수 (기본 5) */
    @Column(name = "scene_count", nullable = false)
    private Integer sceneCount = 5;

    /** 스타일 (캐릭터 artStyle 기본 상속) */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /** 연관 캐릭터 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    /**
     * AI가 생성한 씬 목록 (JSON 직렬화)
     * 각 씬: sceneNumber, title, characters, composition, background,
     *        lighting, mainStory, firstFramePrompt, lastFramePrompt
     */
    @Column(name = "scenes_json", columnDefinition = "TEXT")
    private String scenesJson;

    /** 생성일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
```

### 6-4. Image.java (기존 유지)

```java
@Entity
@Table(name = "images")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Image {

    /** 이미지 고유 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "image_id", length = 36)
    private String imageId;

    /** 소속 플롯 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    /** 씬 번호 */
    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    /** 프레임 종류: 첫 번째 / 마지막 */
    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type", length = 10, nullable = false)
    private FrameType frameType;

    /** 이미지 생성에 사용된 영어 프롬프트 */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 생성된 이미지 URL */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** 생성 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GenerationStatus status = GenerationStatus.PENDING;

    /** 생성일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /** 프레임 종류: 첫 번째 프레임 / 마지막 프레임 */
    public enum FrameType { FIRST, LAST }

    /** 생성 상태 */
    public enum GenerationStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
```

### 6-5. Video.java (수정 — 영상 프롬프트 사용자 수정 가능 명시)

```java
@Entity
@Table(name = "videos")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Video {

    /** 영상 고유 ID (UUID) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_id", length = 36)
    private String videoId;

    /** 소속 플롯 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    /** 씬 번호 */
    @Column(name = "scene_number")
    private Integer sceneNumber;

    /**
     * 영상 생성 프롬프트 (AI가 생성하지만 사용자가 수정 가능)
     * 수정 API: PUT /api/videos/{id}/prompt
     */
    @Column(name = "video_prompt", columnDefinition = "TEXT")
    private String videoPrompt;

    /** 씬의 첫 번째 프레임 이미지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_image_id")
    private Image firstImage;

    /** 씬의 마지막 프레임 이미지 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_image_id")
    private Image lastImage;

    /** 생성된 영상 URL */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** 영상 길이 (초) */
    @Column(name = "duration")
    private Integer duration;

    /** 영상 종류: 씬별 영상 / 최종 병합 영상 */
    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", length = 10, nullable = false)
    private VideoType videoType;

    /** 생성 상태 */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private GenerationStatus status = GenerationStatus.PENDING;

    /** 생성일시 */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /** 영상 종류: 씬별 영상 / 최종 병합 영상 */
    public enum VideoType { SCENE, MERGED }

    /** 생성 상태 */
    public enum GenerationStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
```

---

## 7. REST API 설계

### 7-1. Project API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/projects` | 프로젝트 생성 |
| GET | `/api/projects` | 프로젝트 목록 조회 (작업공간) |
| GET | `/api/projects/{id}` | 프로젝트 단건 조회 (전체 작업 이력 포함) |
| PUT | `/api/projects/{id}` | 프로젝트 수정 |
| DELETE | `/api/projects/{id}` | 프로젝트 삭제 |

**POST /api/projects 요청 바디:**
```json
{
  "title": "괴물과 싸우는 영상"
}
```

---

### 7-2. Character API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/characters` | 캐릭터 + 배경 등록 |
| GET | `/api/characters` | 캐릭터 목록 조회 |
| GET | `/api/characters/{id}` | 캐릭터 단건 조회 |
| PUT | `/api/characters/{id}` | 캐릭터 수정 |
| DELETE | `/api/characters/{id}` | 캐릭터 삭제 |

**POST /api/characters 요청 바디:**
```json
{
  "name": "아이유",
  "description": "20대 초반 여성, 청순하고 밝은 이미지",
  "referenceImageUrl": "https://...",
  "artStyle": "anime",
  "backgroundImageUrl": "https://..."
}
```

---

### 7-3. Plot API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/plots` | 플롯 생성 (Claude AI — 씬 수 지정) |
| PUT | `/api/plots/{id}/scenes/{sceneNumber}` | 특정 씬 내용 수정 (사용자 편집) |
| GET | `/api/plots` | 플롯 목록 조회 |
| GET | `/api/plots/{id}` | 플롯 단건 조회 (씬 목록 포함) |
| DELETE | `/api/plots/{id}` | 플롯 삭제 |

**POST /api/plots 요청 바디:**
```json
{
  "projectId": "uuid-...",
  "title": "괴물과의 대결",
  "idea": "주인공이 도심 폐허에서 거대 괴물과 맞닥뜨려 싸우는 장면",
  "sceneCount": 5,
  "artStyle": "anime",
  "characterId": "uuid-..."
}
```

**응답 (AI가 생성한 씬 목록 — 표 형태):**
```json
{
  "success": true,
  "data": {
    "plotId": "uuid-...",
    "title": "괴물과의 대결",
    "scenes": [
      {
        "sceneNumber": 1,
        "title": "괴물의 등장",
        "characters": "주인공, 괴물",
        "composition": "wide cinematic shot",
        "background": "폐허가 된 도심, 먼지와 잔해",
        "lighting": "dark storm clouds, dramatic lightning",
        "mainStory": "안개 속에서 거대 괴물의 실루엣이 서서히 드러나며 주인공과 대치한다",
        "firstFramePrompt": "A lone hero holding a sword stands in the ruins of an ancient destroyed city...",
        "lastFramePrompt": "The giant monster fully emerges from the dust roaring toward the hero..."
      }
    ],
    "createdAt": "2026-03-12T10:00:00Z"
  }
}
```

**PUT /api/plots/{id}/scenes/{sceneNumber} 요청 바디 (사용자 씬 수정):**
```json
{
  "characters": "주인공 단독",
  "composition": "low angle shot",
  "background": "벚꽃 공원",
  "lighting": "오후 햇살, 따뜻한 오렌지 톤",
  "mainStory": "수정된 스토리 내용",
  "firstFramePrompt": "수정된 첫 프레임 프롬프트",
  "lastFramePrompt": "수정된 마지막 프레임 프롬프트"
}
```

---

### 7-4. Image API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/images/generate` | 씬 이미지 생성 요청 (외부 이미지 AI) |
| GET | `/api/images/plot/{plotId}` | 플롯의 이미지 목록 조회 |
| GET | `/api/images/{id}` | 이미지 단건 조회 (상태 포함) |

**POST /api/images/generate 요청 바디:**
```json
{
  "plotId": "uuid-...",
  "sceneNumber": 1,
  "frameType": "FIRST"
}
```

---

### 7-5. Video API

| Method | URL | 설명 |
|--------|-----|------|
| POST | `/api/videos/generate` | 씬 영상 생성 요청 |
| PUT | `/api/videos/{id}/prompt` | 영상 프롬프트 수정 (사용자 편집 후 재생성 전 호출) |
| POST | `/api/videos/merge` | 전체 씬 영상 병합 요청 |
| GET | `/api/videos/plot/{plotId}` | 플롯의 영상 목록 조회 |
| GET | `/api/videos/{id}` | 영상 단건 조회 (상태 + 프롬프트 포함) |

**POST /api/videos/generate 요청 바디:**
```json
{
  "plotId": "uuid-...",
  "sceneNumber": 1,
  "firstImageId": "uuid-...",
  "lastImageId": "uuid-..."
}
```

**PUT /api/videos/{id}/prompt 요청 바디 (영상 프롬프트 수정):**
```json
{
  "videoPrompt": "The camera slowly pushes forward... (수정된 프롬프트)"
}
```

---

## 8. 상세 비즈니스 로직

### 8-1. Plot 생성 로직 (Claude API — 씬 수 지정)

```
[사용자] POST /api/plots
  {projectId, title, idea, sceneCount, artStyle, characterId}
       │
       ▼
[PlotService.createPlot()]
  1. characterId로 Character 조회 (없으면 404)
  2. Claude API 호출 → sceneCount개의 씬 목록(JSON) 생성
  3. Plot 엔티티 저장 (scenes_json = Claude 응답)
  4. PlotResponse 반환 (씬 목록 표 형태)
```

**Claude API 씬 생성 프롬프트:**

```
[시스템 프롬프트]
당신은 2차 창작 만화/영상의 스토리보드 작가입니다.
사용자의 아이디어를 받아 지정된 수의 씬으로 구성된 플롯을 JSON 형식으로 생성하세요.
각 씬은 영상 제작에 필요한 구체적인 정보를 포함해야 합니다.

[사용자 프롬프트]
제목: {title}
아이디어: {idea}
씬 수: {sceneCount}개
아트 스타일: {artStyle}
캐릭터: {character.name} - {character.description}

다음 JSON 형식으로 정확히 {sceneCount}개의 씬을 생성해주세요:
[
  {
    "sceneNumber": 1,
    "title": "씬 제목",
    "characters": "등장인물",
    "composition": "카메라 구도 (ex. wide shot, close-up, low angle)",
    "background": "배경 묘사",
    "lighting": "조명/분위기",
    "mainStory": "주요 스토리 (2~3문장)",
    "firstFramePrompt": "영어 이미지 생성 프롬프트 (첫 프레임)",
    "lastFramePrompt": "영어 이미지 생성 프롬프트 (마지막 프레임)"
  }
]
```

---

### 8-2. 씬 수정 로직 (사용자 편집)

```
[사용자] PUT /api/plots/{id}/scenes/{sceneNumber}
  {characters, composition, background, lighting, mainStory, firstFramePrompt, lastFramePrompt}
       │
       ▼
[PlotService.updateScene()]
  1. Plot 조회 (없으면 404)
  2. scenes_json 파싱 → List<SceneDto>
  3. 해당 sceneNumber의 씬 내용 교체
  4. scenes_json 재직렬화 후 저장
  5. 수정된 SceneDto 반환
```

---

### 8-3. Image 생성 로직 (외부 이미지 AI)

> ⚠️ 이미지 생성 외부 API는 확정 전입니다.
> 후보: **Stability AI (SDXL)**, **Replicate**, **FAL.ai**

```
[사용자] POST /api/images/generate
  {plotId, sceneNumber, frameType}
       │
       ▼
[ImageService.generateImage()]
  1. Plot 조회 → scenes_json에서 해당 sceneNumber 씬 추출
  2. frameType에 따라 firstFramePrompt / lastFramePrompt 선택
  3. 캐릭터의 artStyle, referenceImageUrl, backgroundImageUrl 추가하여 최종 프롬프트 구성
  4. Image 엔티티 저장 (status = PENDING)
  5. 이미지 생성 API 비동기 호출
  6. 완료 시 image_url 업데이트, status = COMPLETED
  7. 실패 시 status = FAILED
```

**이미지 프롬프트 최종 구성:**
```
{firstFramePrompt or lastFramePrompt}
character reference: {referenceImageUrl}
background: {backgroundImageUrl}
style: {artStyle}
high quality, detailed, professional illustration
```

---

### 8-4. Video 프롬프트 생성 + 영상 생성 로직

> ⚠️ 영상 생성 외부 API는 확정 전입니다.
> 후보: **RunwayML Gen-3**, **Kling AI**, **Pika Labs**

```
[사용자] POST /api/videos/generate
  {plotId, sceneNumber, firstImageId, lastImageId}
       │
       ▼
[VideoService.generateVideo()]
  1. Plot, 첫/마지막 Image 엔티티 조회
  2. 두 이미지 모두 COMPLETED 상태인지 검증
  3. Claude API 호출 → 씬 정보(mainStory, composition 등) 기반 영상 프롬프트 생성
  4. Video 엔티티 저장 (video_prompt = Claude 생성 프롬프트, status = PENDING)
  5. ※ 사용자가 PUT /api/videos/{id}/prompt 로 프롬프트 수정 가능
  6. 영상 생성 API 비동기 호출 (첫 프레임 → 마지막 프레임, 3~5초)
  7. 완료 시 video_url, duration 업데이트, status = COMPLETED
```

**Claude API 영상 프롬프트 생성:**
```
씬 정보:
- 주요 스토리: {mainStory}
- 구도: {composition}
- 배경: {background}
- 조명: {lighting}
- 첫 프레임: {firstImage.imageUrl}
- 마지막 프레임: {lastImage.imageUrl}

위 씬을 3~5초 영상으로 만드는 영어 영상 생성 프롬프트를 작성해주세요.
카메라 움직임, 동작, 분위기를 구체적으로 묘사하세요.
```

---

### 8-5. Video 병합 로직 (Phase 3)

```
[사용자] POST /api/videos/merge
  {plotId}
       │
       ▼
[VideoService.mergeVideos()]
  1. plotId로 video_type=SCENE, status=COMPLETED인 영상 모두 조회
  2. scene_number 순으로 정렬
  3. 모든 씬 영상이 완료됐는지 검증
  4. Video 엔티티 저장 (video_type = MERGED, status = PENDING)
  5. 병합 API or FFmpeg 호출 (씬 순서대로 concat)
  6. 완료 시 merged video_url 저장
```

---

## 9. DTO 설계

### SceneDto.java (scenes_json 직렬화 — 필드 업데이트)
```java
public class SceneDto {
    /** 씬 번호 */
    private int sceneNumber;
    /** 씬 제목 */
    private String title;
    /** 등장인물 */
    private String characters;
    /** 카메라 구도 */
    private String composition;
    /** 배경 묘사 */
    private String background;
    /** 조명/분위기 */
    private String lighting;
    /** 주요 스토리 */
    private String mainStory;
    /** 첫 프레임 이미지 생성 프롬프트 (영어) */
    private String firstFramePrompt;
    /** 마지막 프레임 이미지 생성 프롬프트 (영어) */
    private String lastFramePrompt;
}
```

### PlotCreateRequest.java
```java
public class PlotCreateRequest {

    /** 소속 프로젝트 ID */
    private String projectId;

    /** 플롯 제목 */
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 200)
    private String title;

    /** 사용자 스토리 아이디어 */
    @NotBlank(message = "아이디어는 필수입니다")
    private String idea;

    /** 씬 수 (기본 5, 최소 1, 최대 10) */
    @Min(1) @Max(10)
    private Integer sceneCount = 5;

    /** 스타일 템플릿 */
    @Size(max = 50)
    private String artStyle;

    /** 캐릭터 ID (선택) */
    private String characterId;
}
```

---

## 10. DB 스키마 변경 사항

기존 `schema.sql`에 아래 내용 추가 필요:

```sql
-- =============================================
-- projects 테이블: 작업공간 프로젝트 파일 관리
-- =============================================
CREATE TABLE IF NOT EXISTS projects (
    project_id   VARCHAR(36)  NOT NULL,                    -- 프로젝트 고유 ID (UUID)
    title        VARCHAR(200) NOT NULL,                    -- 프로젝트 제목
    character_id VARCHAR(36)  REFERENCES characters(character_id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),      -- 생성일시
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),      -- 수정일시
    CONSTRAINT pk_projects PRIMARY KEY (project_id)
);

COMMENT ON TABLE  projects              IS '작업공간 프로젝트 파일';
COMMENT ON COLUMN projects.project_id  IS '프로젝트 고유 ID (UUID)';
COMMENT ON COLUMN projects.title       IS '프로젝트 제목 (ex. 괴물과 싸우는 영상)';
COMMENT ON COLUMN projects.character_id IS '프로젝트에서 사용하는 캐릭터 ID';
```

`characters` 테이블에 컬럼 추가:
```sql
ALTER TABLE characters ADD COLUMN IF NOT EXISTS
    background_image_url VARCHAR(500);   -- 배경 이미지 URL

COMMENT ON COLUMN characters.background_image_url IS '배경 이미지 URL (사용자 업로드 또는 생성)';
```

`plots` 테이블에 컬럼 추가:
```sql
ALTER TABLE plots ADD COLUMN IF NOT EXISTS
    project_id  VARCHAR(36) REFERENCES projects(project_id) ON DELETE SET NULL;
ALTER TABLE plots ADD COLUMN IF NOT EXISTS
    scene_count INTEGER NOT NULL DEFAULT 5;   -- 사용자가 지정한 씬 수

COMMENT ON COLUMN plots.project_id  IS '소속 프로젝트 ID';
COMMENT ON COLUMN plots.scene_count IS '사용자가 지정한 씬 수 (기본 5)';
```

---

## 11. 개발 단계별 체크리스트

### Step 1: Project 엔티티 + DB 스키마
- [ ] `schema.sql`에 `projects` 테이블 추가
- [ ] `characters`에 `background_image_url` 컬럼 추가
- [ ] `plots`에 `project_id`, `scene_count` 컬럼 추가
- [ ] `Project.java` 엔티티 작성
- [ ] `ProjectRepository.java` 작성
- [ ] 앱 실행 후 `ddl-auto: validate` 통과 확인

### Step 2: Character + Project CRUD
- [ ] `CharacterCreateRequest.java` (backgroundImageUrl 포함)
- [ ] `CharacterResponse.java`
- [ ] `CharacterService.java`
- [ ] `CharacterController.java`
- [ ] `ProjectCreateRequest.java`, `ProjectResponse.java`
- [ ] `ProjectService.java`
- [ ] `ProjectController.java`
- [ ] Postman/curl로 CRUD 테스트

### Step 3: Claude API 연동 + Plot 생성 + 씬 수정
- [ ] `ClaudeService.java` (generateScenes — sceneCount 지정)
- [ ] `PlotCreateRequest.java` (sceneCount 필드 포함)
- [ ] `SceneDto.java` (characters, mainStory 등 신규 필드 포함)
- [ ] `SceneUpdateRequest.java`
- [ ] `PlotService.java` (createPlot, updateScene, findAll, findById, delete)
- [ ] `PlotController.java`
- [ ] Claude 응답 JSON 파싱 검증
- [ ] 씬 수정 API 테스트

### Step 4: Image 생성 API
- [ ] 이미지 생성 외부 API 결정 및 테스트
- [ ] `ImageService.java` (캐릭터 참조이미지 + 배경이미지 프롬프트 조합)
- [ ] `ImageController.java`
- [ ] 비동기 처리 (`@Async`) 구현
- [ ] 상태 폴링 테스트

### Step 5: Video 프롬프트 생성 + Video 생성 API
- [ ] `ClaudeService.java`에 generateVideoPrompt 메서드 추가
- [ ] 영상 생성 외부 API 결정 및 테스트
- [ ] `VideoService.java` (generateVideo, updatePrompt)
- [ ] `VideoController.java`
- [ ] 영상 프롬프트 수정 API (`PUT /api/videos/{id}/prompt`) 테스트
- [ ] 비동기 처리 구현

### Step 6: 통합 테스트
- [ ] 전체 플로우 E2E 테스트
  - 프로젝트 생성 → 캐릭터 등록 → 플롯 생성 → 씬 편집 → 이미지 생성 → 영상 생성
- [ ] 에러 케이스 테스트
- [ ] Development 브랜치에 병합

---

## 12. 외부 AI API (확정)

> **FAL.ai 하나로 이미지 + 영상 모두 처리. `.env`에 `FAL_KEY` 하나만 추가.**

### 이미지 생성 → FLUX.1 Kontext [pro] (FAL.ai)

```
POST https://fal.run/fal-ai/flux-pro/kontext
{ "image_url": "{referenceImageUrl}", "prompt": "{scenePrompt}" }
```

| 항목 | 내용 |
|------|------|
| 일관성 범위 | 얼굴 + 옷 + 체형 + 스타일 전체 |
| 가격 | $0.04/장 |
| 응답 방식 | 동기 (즉시 반환) |

### 영상 생성 → Wan 2.1 FLF2V (개발/테스트) / Veo 3.1 Fast (프로덕션)

> 기획서 모크업에 "Veo 3.1" 명시됨. FAL.ai에서 바로 접근 가능.

```
// 개발/테스트
POST https://queue.fal.run/fal-ai/wan-flf2v

// 프로덕션
POST https://queue.fal.run/fal-ai/veo3.1/fast/first-last-frame-to-video
```

| 모델 | 5초 영상 가격 | Start+End 프레임 | 용도 |
|------|-------------|-----------------|------|
| Wan 2.1 FLF2V | $0.20~$0.40 | ✅ | 개발/테스트 |
| Veo 3.1 Fast | $0.50 | ✅ | 프로덕션 |
| Kling O1 | $0.56 | ✅ | 대안 |

비동기 큐 패턴: `POST 제출 → request_id → GET 폴링 → 완료 시 video_url`
기존 `GenerationStatus` enum + `@Async` 폴링 구조와 완벽히 일치.

---

## 13. 주의사항 및 컨벤션

### 코드 규칙
- 모든 Java 클래스/필드/메서드: **한국어 Javadoc + 인라인 주석**
- SQL: 테이블·컬럼마다 **한국어 COMMENT**
- Lombok: `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` 기본 조합

### API 응답 규칙
- 모든 응답은 `ApiResponse<T>` 래핑
- 성공: `ApiResponse.ok(data)` → HTTP 200
- 생성: `ApiResponse.ok("생성 성공", data)` → HTTP 201
- 실패: `ApiResponse.fail(message)` → `BusinessException`으로 처리

### 비동기 처리 규칙
- Claude API: `WebClient.block()` (동기) → 타임아웃 30초 설정
- 이미지/영상 생성: `@Async` 비동기 → 즉시 PENDING 상태 반환 후 백그라운드 처리
- 프론트: 폴링 방식으로 `GET /api/images/{id}` or `GET /api/videos/{id}` 상태 확인

### 환경변수 추가 시
- `.env` 파일에 추가
- `application.yml`에 `${NEW_VAR}` 추가
- README에 변수 목록 업데이트

---

*이 문서는 개발 진행에 따라 지속적으로 업데이트됩니다.*
