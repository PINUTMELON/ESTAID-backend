# ESTAID Backend - Q&A

> 개발 중 학습한 내용 정리. 질문이 생길 때마다 추가.

---

## Q. JPA가 뭐지?

**JPA (Java Persistence API)**

Java에서 객체(Object)와 DB 테이블을 자동으로 연결해주는 표준 인터페이스.

### SQL 직접 작성 vs JPA 비교

원래 DB에 데이터를 저장하려면 SQL을 직접 작성해야 한다.

```sql
INSERT INTO characters (character_id, name, art_style) VALUES (?, ?, ?);
```

JPA를 쓰면 Java 객체를 저장하는 것만으로 SQL이 자동 실행된다.

```java
characterRepository.save(character); // INSERT 자동 실행
```

### 이 프로젝트에서의 역할

| JPA 요소 | 파일 | 역할 |
|----------|------|------|
| `@Entity` | `Character.java` 등 | "이 클래스는 DB 테이블이다" 선언 |
| `@Table(name="characters")` | 동일 | 매핑할 테이블 이름 지정 |
| `@Column` | 각 필드 | 컬럼명·제약조건 매핑 |
| `JpaRepository` | `CharacterRepository.java` 등 | CRUD 메서드 자동 생성 |

### `ddl-auto: validate`의 의미

앱 실행 시 JPA가 엔티티 클래스와 실제 DB 테이블 구조를 비교한다.
- 일치하면 → 정상 실행
- 불일치하면 → 앱 시작 실패 (에러)

---

## Q. MyBatis Mapper랑 다른점은?

### 접근 방식 자체가 다르다

| | JPA | MyBatis |
|--|-----|---------|
| 중심 | **객체** | **SQL** |
| SQL 작성 | 자동 생성 | 직접 작성 |
| 학습 난이도 | 높음 | 낮음 |
| SQL 제어권 | 낮음 | 높음 |

### 같은 기능을 구현할 때 비교

캐릭터 이름으로 검색하는 예시:

**MyBatis**
```xml
<!-- CharacterMapper.xml -->
<select id="findByName" resultType="Character">
    SELECT * FROM characters WHERE name = #{name}
</select>
```
```java
// CharacterMapper.java
Character findByName(String name);
```

**JPA**
```java
// CharacterRepository.java - SQL 없이 메서드 이름만으로 끝
Optional<Character> findByName(String name);
```

### 각각 유리한 상황

**JPA가 유리**
- 단순 CRUD 위주
- 테이블 관계(FK)가 많을 때 → `@ManyToOne` 등으로 자동 처리
- 빠른 개발 속도가 중요할 때

**MyBatis가 유리**
- 복잡한 JOIN, 집계 쿼리가 많을 때
- DBA가 SQL을 직접 튜닝해야 할 때
- 레거시 DB 구조가 복잡할 때

### 이 프로젝트에서 JPA를 선택한 이유

테이블 구조가 단순하고 (`characters → plots → images → videos`) 관계가 명확한 FK 구조라
JPA의 `@ManyToOne` 자동 처리가 유리하다. 복잡한 통계 쿼리가 없어서 SQL을 직접 제어할 필요도 적다.

---

## Q. AI API 어디꺼 써야 해? (플롯/이미지/영상)

### 최종 결론

| 단계 | API | 플랫폼 | 이유 |
|------|-----|--------|------|
| 플롯 기획 (idea → text) | **Claude API** | Anthropic | 이미 세팅됨, 스토리보딩 최적 |
| 이미지 생성 (text+ref → image) | **FLUX.1 Kontext [pro]** | FAL.ai | 얼굴+옷+체형 전체 일관성 유지 |
| 영상 생성 개발/테스트 (image → video) | **Wan 2.1 FLF2V** | FAL.ai | 가장 저렴한 Start/End 프레임 |
| 영상 생성 프로덕션 (image → video) | **Veo 3.1 Fast** | FAL.ai | 기획서 모크업에 명시된 모델, 고품질 |

> **핵심: FAL.ai 하나로 이미지 + 영상 모두 처리 → API 키 1개, WebClient 설정 1개**

---

### 1. 플롯 기획 → Claude API (이미 결정)

`ClaudeConfig.java`에 이미 세팅되어 있고 프로젝트 구조 전체가 이에 맞게 설계되어 있어 변경 불필요.

---

### 2. 이미지 생성 → FLUX.1 Kontext [pro] (FAL.ai)

> **IP-Adapter는 최선이 아니었다.**

처음에 IP-Adapter를 선택했지만, 이것은 **얼굴만** 일관성을 유지한다.
2차 창작에서 핵심은 얼굴뿐 아니라 **옷, 머리색, 체형, 스타일**까지 씬마다 동일해야 하는 것.
**FLUX.1 Kontext**는 레퍼런스 이미지 전체를 조건으로 삼아 전체 외형을 유지한다.

```
레퍼런스 이미지(캐릭터 전체) → FLUX.1 Kontext → 모든 씬에서 동일 외형 유지
```

API 호출 예시:
```json
POST https://fal.run/fal-ai/flux-pro/kontext
{
  "image_url": "https://.../character_reference.jpg",
  "prompt": "same character standing under cherry blossoms, anime style, spring afternoon"
}
// 응답: { "images": [{ "url": "https://..." }] }
```

| | FLUX.1 Kontext | IP-Adapter FaceID | Stability AI |
|--|----------------|-------------------|--------------|
| 일관성 범위 | ✅ **얼굴+옷+체형+스타일 전체** | ❌ 얼굴만 | △ |
| 기반 모델 | ✅ 최신 12B FLUX | 구형 SDXL | 구형 |
| 레퍼런스 이미지 | ✅ 이미지 전체 조건 | CLIP 임베딩만 | △ |
| 가격 | $0.04/장 | $0.003~0.01/장 | 비슷 |
| API 난이도 | ✅ 매우 단순 | 단순 | 보통 |

---

### 3. 영상 생성 → FAL.ai (Wan 2.1 / Veo 3.1)

> **기획서 모크업에 이미 "Veo 3.1"이 명시되어 있었다.** FAL.ai에서 바로 쓸 수 있다.

```
첫 프레임 이미지 + 마지막 프레임 이미지 → FAL.ai → 3~5초 영상
```

**Start + End 프레임 동시 지원 모델 비교:**

| 모델 | 플랫폼 | 5초 영상 가격 | 품질 |
|------|--------|--------------|------|
| Wan 2.1 FLF2V | FAL.ai | **$0.20~$0.40** | 보통 (개발용) |
| Kling O1 | FAL.ai | **$0.56** | 높음 |
| Hailuo-02 (MiniMax) | FAL.ai | **$0.27** | 보통 |
| **Veo 3.1 Fast** | FAL.ai | **$0.50** | ✅ 최고 (프로덕션) |
| Veo 3.1 Standard | FAL.ai | $1.00 | 최고 |

> Kling AI 직접 연동은 불필요 — FAL.ai에서 Kling O1도 사용 가능하고, Veo 3.1이 더 좋다.

**FAL.ai 비동기 큐 패턴 (영상 생성):**
```java
// 1단계: 작업 제출 → request_id 반환 (즉시)
POST https://queue.fal.run/fal-ai/wan-flf2v
{ "first_frame_image_url": "...", "last_frame_image_url": "...", "prompt": "..." }
// → { "request_id": "abc123" }

// 2단계: 상태 폴링 (기존 GET /api/videos/{id} 폴링 구조 그대로 활용)
GET https://queue.fal.run/fal-ai/wan-flf2v/requests/abc123/status
// → { "status": "IN_PROGRESS" | "COMPLETED" | "FAILED" }

// 3단계: 결과 조회
GET https://queue.fal.run/fal-ai/wan-flf2v/requests/abc123
// → { "video": { "url": "https://..." } }
```

이미지 생성은 동기(즉시 응답), 영상 생성은 비동기(큐 폴링) — 기존 `GenerationStatus` enum, `@Async` 구조와 완벽히 일치한다.

---

### 환경변수 추가

`.env`에 `FAL_KEY` 하나만 추가하면 이미지 + 영상 모두 해결된다.

```
FAL_KEY=your_fal_api_key_here
```

---

## Q. FAL.ai가 정확히 뭐야? API 비용은 비싼가?

### FAL.ai 정체

개발자를 위한 **AI 모델 추론 클라우드 플랫폼**. 직접 AI 모델을 만드는 게 아니라,
FLUX·Stable Diffusion·Google Imagen 등 600개 이상의 모델을 **하나의 API**로 쓸 수 있게 해주는 서비스.
NVIDIA, a16z 등에서 투자받은 안정적인 기업. 자체 최적화 추론 엔진으로 경쟁사 대비 최대 4배 빠름.

### 가격 (이미지 1024×1024 기준)

| 모델 | 1장당 가격 | 특징 |
|------|-----------|------|
| FLUX.1 schnell | **$0.003** (~4원) | 가장 저렴, 프로토타이핑용 |
| FLUX.2 Turbo | $0.008 (~11원) | 빠르고 저렴 |
| FLUX.1 dev | $0.025 (~35원) | IP-Adapter 통합 지원 |
| FLUX.1 Kontext pro | $0.03 (~42원) | 캐릭터 일관성 특화 |
| FLUX.2 pro | $0.03 (~42원) | 최고 품질, 레퍼런스 4장 |

씬 5개 × 프레임 2장 = 이미지 10장 기준
- schnell 사용 시 → **약 40원**
- pro 사용 시 → **약 420원**

신규 가입 시 무료 크레딧 제공, 신용카드 불필요.

### 캐릭터 일관성 지원 방식 (이 프로젝트 핵심)

| 방법 | 모델 | 설명 |
|------|------|------|
| 얼굴 기반 | `ip-adapter-face-id` | 얼굴 사진 1장으로 모든 씬에 같은 얼굴 유지 |
| 이미지 기반 | `flux-pro/kontext` | 파인튜닝 없이 캐릭터 전체 외형 유지 |
| 멀티 레퍼런스 | `flux-2-pro` | 레퍼런스 최대 4장, 가장 정교한 일관성 |

### 주의사항

- 생성된 이미지 **7일 후 자동 삭제** → Supabase Storage 등에 따로 저장 필요
- API 키 유출 시 무단 과금 책임은 사용자에게 있음

---

## Q. 플롯(Plot)이 뭐야?

### 한 줄 요약

영상을 만들기 전에 짜는 **장면 계획서**.

### 쉬운 설명

영화 감독이 촬영 전에 "1장면: 주인공 공원 입장 → 2장면: 벚꽃 아래 마주침 → 3장면: 눈이 마주침..." 미리 계획을 짜는 것이 플롯이다.

### 이 프로젝트에서의 흐름

```
사용자: "벚꽃 공원에서 첫눈에 반하는 이야기"
           ↓ Claude API
플롯 생성:
  씬 1 - 공원 입장: 주인공이 벚꽃 공원에 들어서는 장면
  씬 2 - 첫 만남:   두 사람의 시선이 마주치는 장면
  씬 3 - 설렘:      벚꽃이 흩날리며 가까워지는 장면
           ↓
각 씬별로 이미지 생성 (첫 프레임 + 마지막 프레임)
           ↓
각 씬별로 영상 생성 (첫 프레임 → 마지막 프레임 = 3~5초)
           ↓
씬들을 합쳐서 최종 영상 완성
```

플롯 없이 바로 이미지/영상을 만들면 각 씬이 따로 놀아서 하나의 스토리처럼 보이지 않는다.
플롯은 **"어떤 장면을 어떤 순서로 만들지"** 를 AI가 설계해주는 단계다.

---

## Q. Character가 정확히 뭐 하는 애야?

### 한 줄 요약

이미지/영상 생성 시 "이 캐릭터처럼 그려줘" 하고 AI한테 넘겨주는 **레퍼런스 정보 저장소**.

### 핵심 역할: 캐릭터 일관성 유지

여러 씬에서 같은 캐릭터처럼 보이게 하는 것이 이 프로젝트의 핵심 난제다.
AI한테 그냥 "아이유 그려줘"라고 하면 씬마다 얼굴이 달라진다.
Character에 레퍼런스 이미지 URL과 설명을 저장해두고, 이미지 생성 API를 호출할 때마다 이 정보를 함께 보낸다.

```
Character 저장 정보
├── name:               "아이유"
├── description:        "20대 초반 여성, 긴 갈색 머리, 청순한 이미지"
├── referenceImageUrl:  "https://.../iu_reference.jpg"  ← 핵심
└── artStyle:           "anime"

           ↓ 이미지 생성할 때 FAL.ai에 전달

FAL.ai IP-Adapter:
  "이 레퍼런스 이미지(아이유)의 외형을 유지하면서
   anime 스타일로 벚꽃 공원 장면 그려줘"
```

### 전체 흐름에서의 위치

```
① Character 등록  ← 이름, 설명, 레퍼런스 이미지, 화풍 저장
        ↓
② Plot 생성       ← Character 정보를 Claude에 넘겨서 씬 생성 (캐릭터 설명이 씬 묘사에 반영)
        ↓
③ Image 생성      ← referenceImageUrl + artStyle을 FAL.ai에 전달 (모든 씬에서 같은 외형 유지)
        ↓
④ Video 생성      ← Image 기반이라 캐릭터 일관성 자동 유지
```

### 각 필드의 실제 쓰임

| 필드 | 실제 용도 |
|------|----------|
| `characterId` | Plot, Image에서 FK로 참조할 때 사용 |
| `name` | Claude 프롬프트에 "캐릭터: 아이유" 형태로 삽입 |
| `description` | Claude 프롬프트에 캐릭터 외형 설명으로 삽입 |
| `referenceImageUrl` | FAL.ai IP-Adapter에 레퍼런스로 전달 |
| `artStyle` | 이미지 프롬프트에 "style: anime" 형태로 삽입 |

### Character가 없으면?

`Plot.character`가 nullable이라 캐릭터 없이도 플롯 생성은 가능하다.
하지만 AI가 캐릭터 외형을 마음대로 그리기 때문에 씬마다 얼굴이 달라진다.
**2차 창작의 핵심인 "원작 캐릭터처럼 보이게"가 불가능**해진다.

---

## Q. 랭체인이 뭐야? 이 프로젝트에 쓸 수 있어?

### 한 줄 요약

AI API들을 연결해서 자동화된 흐름을 만드는 프레임워크.

```
Claude API  ──┐
FAL.ai      ──┤  → LangChain → 하나의 자동화된 파이프라인
Kling AI    ──┘
```

### 핵심 기능

| 기능 | 설명 | 이 프로젝트 해당 |
|------|------|----------------|
| Chain | API 호출을 순서대로 연결 | 플롯→이미지→영상 자동화 |
| Prompt Template | 프롬프트를 변수로 관리 | Claude 프롬프트 관리 |
| Memory | 대화 맥락 유지 | 씬 간 일관성 |
| Tool | 외부 API를 도구로 등록 | FAL.ai, Kling AI 등록 |

### 결론: 지금은 필요 없다

LangChain은 Python/JavaScript 기반이고 Java용(LangChain4j)은 기능이 적고 레퍼런스도 부족하다.
이 프로젝트의 각 단계는 사용자 요청마다 독립적으로 실행되므로 WebClient 직접 호출이 더 단순하고 적합하다.

---

## Q. WebClient랑 LangChain 차이가 뭐야?

### 한 줄 요약

| | WebClient | LangChain |
|--|-----------|-----------|
| 정체 | HTTP 요청 도구 | AI 워크플로우 프레임워크 |
| 비유 | **전화기** | **콜센터 자동화 시스템** |

### 계층이 다르다

```
LangChain (흐름 설계)
    └── HTTP Client (실제 통신) ← WebClient가 하는 일
            └── Claude API / FAL.ai / Kling AI
```

LangChain 내부에도 결국 HTTP 호출이 있다. WebClient 같은 도구가 LangChain 안에 들어있는 것.

### LangChain이 빛을 발하는 구조

```
사용자 요청 → AI가 판단 → API A 호출
                        → 결과 보고 AI가 재판단
                        → API B 호출
                        → 결과 종합 → 최종 답변
```

이 프로젝트는 `사용자 요청 → API 호출 1번 → DB 저장 → 끝` 구조라 WebClient가 적합하다.

---

## Q. 기획서 반영 후 DEV_PLAN.md에서 뭐가 바뀐 거야?

### 한 줄 요약

기획서에서 **"작업공간(Project) 개념"** 과 **"선택지로 생성하기 상세 플로우"** 가 새로 정해지면서
엔티티·API·로직 전반이 이에 맞게 업데이트됐다.

---

### 변경 1: Project 엔티티 신규 추가

**기존:** 캐릭터 → 플롯 → 이미지 → 영상으로 바로 이어지는 단순 구조.
**변경:** 작업공간에서 **프로젝트 파일 단위**로 작업을 묶어 관리하는 구조 추가.

```
기존 구조:   Character → Plot → Image → Video
변경 구조:   Project → (Character) → Plot → Image → Video
```

프로젝트별로 영상을 분리 저장하고, 나중에 이어서 작업할 수 있도록 하기 위함.

추가된 것:
- `Project.java` 엔티티 (projectId, title, characterId FK, created_at, updated_at)
- `projects` DB 테이블
- `GET/POST/PUT/DELETE /api/projects` API
- `Plot`에 `project_id` FK 컬럼 추가

---

### 변경 2: Character에 배경 이미지 필드 추가

**기존:** 캐릭터 정보만 저장 (name, description, referenceImageUrl, artStyle).
**변경:** 배경 이미지 URL 필드 추가.

```java
// 추가된 필드
private String backgroundImageUrl;  // 배경 이미지 URL (사용자 업로드 or 생성)
```

기획서에서 캐릭터 생성과 배경 이미지 업로드가 **같은 화면에서** 이루어지기 때문.
이미지 생성 시 캐릭터 레퍼런스 이미지 + 배경 이미지를 함께 프롬프트에 전달.

---

### 변경 3: Plot에 씬 수 설정 + 씬 수정 API 추가

**기존:** 사용자가 아이디어를 입력하면 AI가 알아서 5~8개 씬 생성.
**변경:** 사용자가 씬 수를 직접 지정 (`sceneCount`), 생성 후 표에서 씬 내용 **직접 편집 가능**.

추가된 것:
- `Plot.sceneCount` 필드 (기본 5, 최소 1, 최대 10)
- `PlotCreateRequest.sceneCount` 필드
- `PUT /api/plots/{id}/scenes/{sceneNumber}` API — 사용자가 특정 씬을 수정

```
기존: 사용자 아이디어 → Claude → 씬 목록 생성 (끝)
변경: 사용자 아이디어 → Claude → 씬 목록 생성 → 사용자가 표에서 씬 편집 가능
```

---

### 변경 4: SceneDto 필드 변경

**기존 SceneDto:**
```
sceneNumber, title, description, composition, background, lighting,
firstFramePrompt, lastFramePrompt
```

**변경 SceneDto:**
```
sceneNumber, title, characters, composition, background, lighting,
mainStory, firstFramePrompt, lastFramePrompt
```

| 변경 내용 | 이유 |
|-----------|------|
| `description` → `mainStory` | 기획서 표 컬럼명이 "주요스토리"로 명시됨 |
| `characters` 필드 신규 추가 | 기획서 표에 "등장인물" 컬럼이 명시됨 |

---

### 변경 5: 영상 프롬프트 수정 API 추가

**기존:** 영상 프롬프트는 AI가 생성하면 고정.
**변경:** AI가 영상 프롬프트를 먼저 생성하고, 사용자가 **입력창에서 수정 후** 영상 생성 가능.

추가된 것:
- `PUT /api/videos/{id}/prompt` API
- `VideoPromptUpdateRequest.java` DTO

```
기존: 씬 이미지 → 영상 생성 API 바로 호출
변경: 씬 이미지 → Claude가 영상 프롬프트 생성 → 사용자가 프롬프트 확인·수정 → 영상 생성
```

---

### 변경 6: 두 가지 서비스 모드 명확화 (설명 추가)

기존에는 하나의 플로우만 있었는데, 기획서에서 두 모드를 명확히 구분했다.

| 모드 | 대상 | 개발 여부 |
|------|------|-----------|
| 선택지로 생성하기 | 초심자 — 단계별 안내 | ✅ 현재 개발 대상 |
| 프롬프트로 생성하기 | 숙련자 — 직접 편집 | ❌ 아직 미계획 |

DEV_PLAN은 **선택지로 생성하기** 기준으로 작성된 것.

---

### 변경 7: 전체 플로우 다이어그램 세분화

**기존:** ①~⑤ 5단계 단순 나열.
**변경:** 각 단계에 구체적인 동작 설명 추가.

```
기존 ② POST /api/plots  ← Claude API 호출 (끝)

변경 ③ 플롯(씬) 자동 생성
       ├── 씬 수 설정
       ├── 스토리 아이디어 입력
       └── AI가 N개 씬 생성 → 표 형태로 출력
            [씬번호 | 등장인물 | 구도 | 배경 | 조명 | 주요스토리 | 첫 프레임 | 마지막 프레임]
       ※ 사용자가 표에서 직접 텍스트 수정 가능
       ※ 구도 선택 시 GIF 템플릿으로 종류 안내
```

---

### 변경 8: DB 스키마 변경사항 섹션 신규 추가 (섹션 10)

기존에는 엔티티 설계만 있었고 실제 DDL 변경 내용이 없었다.
`ddl-auto: validate` 설정 때문에 **DB 스키마와 엔티티가 항상 일치**해야 하므로,
엔티티가 바뀔 때 schema.sql에 어떤 SQL을 추가해야 하는지 명시.

---

### 변경 요약표

| 항목 | 기존 | 변경 후 |
|------|------|---------|
| Project 엔티티 | 없음 | 신규 추가 |
| Character.backgroundImageUrl | 없음 | 추가 |
| Plot.sceneCount | 없음 | 추가 |
| Plot.project FK | 없음 | 추가 |
| SceneDto.characters / mainStory | 없음 | 추가 |
| 씬 수정 API | 없음 | 추가 |
| 영상 프롬프트 수정 API | 없음 | 추가 |
| 서비스 모드 구분 | 없음 | 선택지 vs 프롬프트로 명확화 |
| DB 스키마 변경사항 | 없음 | 섹션 10으로 추가 |

---

## Q. AI 품질은 WebClient vs LangChain 중 어느 게 더 좋아?

### 결론: 도구가 품질을 결정하는 게 아니다

WebClient든 LangChain이든 결국 **같은 Claude API, 같은 모델**을 호출한다.
**품질은 프롬프트가 결정한다.**

LangChain이 제공하는 프롬프트 기법들이 품질을 올려주는 건데, 이건 WebClient로도 똑같이 구현 가능하다.

### 품질을 높이는 프롬프트 기법

**1. Few-shot Prompting** - 예시를 보여줘서 원하는 출력 형식 학습
```
"아래 예시처럼 씬을 만들어줘:
예시) 씬1: { sceneNumber: 1, title: '공원 입장', description: '...' }
이제 '벚꽃 아래에서' 스토리의 씬 5개를 같은 형식으로 만들어줘"
```

**2. Chain of Thought (CoT)** - AI한테 단계별로 생각하게 시키기
```
"다음 순서로 생각해서 씬을 만들어줘:
1. 전체 스토리 흐름 파악
2. 감정 곡선 설계 (기승전결)
3. 각 씬의 시각적 임팩트 고려
4. JSON으로 출력"
```

**3. 역할 부여 (Role Prompting)** - 전문가 페르소나 부여
```
"당신은 10년 경력의 애니메이션 스토리보드 작가입니다.
K-pop MV와 2차 창작 팬 영상을 전문으로 합니다."
```

### 정리

| | WebClient + 좋은 프롬프트 | LangChain + 기본 프롬프트 |
|--|--------------------------|--------------------------|
| 출력 품질 | ✅ 높음 | 보통 |
| 구현 난이도 | ✅ 낮음 | 높음 |
| 결론 | **이게 맞음** | 품질은 도구에서 나오지 않음 |

**도구를 바꾸는 것보다 프롬프트를 잘 짜는 게 품질에 10배 더 영향을 준다.**

---
