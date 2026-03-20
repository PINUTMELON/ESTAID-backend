# ESTAID Backend API 명세서

> **Base URL:** `http://localhost:8080`
> **응답 형식:** 모든 API는 `ApiResponse<T>` 래퍼로 응답합니다.
> **최종 수정:** 2026-03-20

---

## 인증 방식

로그인 외 모든 API는 **JWT 토큰** 이 필요합니다.

```
Authorization: Bearer {accessToken}
```

토큰 없이 요청하면 `401 Unauthorized` 가 반환됩니다.

---

## 공통 응답 형식

```json
{
  "status": "success | error",
  "message": "결과 메시지",
  "data": { }
}
```

---

## 목차

1. [인증 (Auth)](#1-인증-auth)
2. [프로젝트 (Project)](#2-프로젝트-project)
3. [캐릭터 (Character)](#3-캐릭터-character)
4. [플롯 (Plot)](#4-플롯-plot)
5. [이미지 생성 (Image)](#5-이미지-생성-image)
6. [영상 생성 (Video)](#6-영상-생성-video)
7. [Asset (캐릭터·배경 생성 및 저장)](#7-asset-캐릭터배경-생성-및-저장)

---

## 1. 인증 (Auth)

**Base Path:** `/auth`
> 인증 없이 호출 가능합니다.

---

### 1.1 로그인

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/auth/login` |
| **상태 코드** | `200 OK` |
| **인증** | 불필요 |
| **설명** | 어드민 계정으로 로그인하고 JWT 토큰을 발급받습니다. |

#### Request Body

```json
{
  "username": "admin",
  "password": "admin1234"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `username` | String | ✅ | 사용자명 |
| `password` | String | ✅ | 비밀번호 |

#### Response Body

```json
{
  "status": "success",
  "message": "로그인 성공",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "username": "admin",
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `data.accessToken` | String | 이후 모든 API 요청 헤더에 포함해야 하는 JWT 토큰 |
| `data.tokenType` | String | 항상 `"Bearer"` |

---

### 1.2 로그아웃

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/auth/logout` |
| **상태 코드** | `200 OK` |
| **인증** | 불필요 |
| **설명** | Stateless JWT 방식이므로 클라이언트에서 토큰을 삭제하면 됩니다. |

#### Response Body

```json
{
  "status": "success",
  "message": "로그아웃 성공",
  "data": null
}
```

---

## 2. 프로젝트 (Project)

**Base Path:** `/api/projects`
> 모든 API에 JWT 토큰이 필요합니다.

---

### 2.1 프로젝트 전체 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects` |
| **상태 코드** | `200 OK` |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트 목록 조회 성공",
  "data": [
    {
      "projectId": "550e8400-e29b-41d4-a716-446655440000",
      "title": "괴물과 싸우는 영상",
      "backgroundImageUrl": "https://example.com/bg.png",
      "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\"}",
      "createdAt": "2026-03-20T00:00:00Z",
      "updatedAt": "2026-03-20T00:00:00Z"
    }
  ]
}
```

---

### 2.2 프로젝트 단건 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |

---

### 2.3 프로젝트 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/projects` |
| **상태 코드** | `201 Created` |

#### Request Body

```json
{
  "title": "봄날 벚꽃 로맨스",
  "backgroundImageUrl": "https://example.com/bg.png",
  "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\"}"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `title` | String | ✅ | 프로젝트 제목 |
| `backgroundImageUrl` | String | ❌ | 배경 이미지 URL |
| `settingsJson` | String | ❌ | AI 영상 생성 기본 설정 JSON |

---

### 2.4 프로젝트 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |

---

### 2.5 프로젝트 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |
| **설명** | 연결된 캐릭터, 플롯, 이미지, 영상도 CASCADE 삭제됩니다. |

---

## 3. 캐릭터 (Character)

**Base Path:** `/api/projects/{projectId}/characters`
> 모든 API에 JWT 토큰이 필요합니다.

---

### 3.1 캐릭터 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}/characters` |
| **상태 코드** | `200 OK` |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 목록 조회 성공",
  "data": [
    {
      "characterId": "550e8400-e29b-41d4-a716-446655440001",
      "projectId": "550e8400-e29b-41d4-a716-446655440000",
      "name": "아이유",
      "description": "20대 초반 여성, 긴 갈색 머리, 청순한 이미지",
      "referenceImageUrl": "https://example.com/ref.png",
      "artStyle": "anime",
      "createdAt": "2026-03-20T00:00:00Z",
      "updatedAt": "2026-03-20T00:00:00Z"
    }
  ]
}
```

---

### 3.2 캐릭터 단건 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |

---

### 3.3 캐릭터 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/projects/{projectId}/characters` |
| **상태 코드** | `201 Created` |

#### Request Body

```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "강백호",
  "description": "10대 후반 남성, 빨간 머리, 농구를 좋아하는 열정적인 성격",
  "referenceImageUrl": "https://example.com/ref.png",
  "artStyle": "webtoon"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `projectId` | String | ✅ | 소속 프로젝트 UUID |
| `name` | String | ✅ | 캐릭터 이름 |
| `description` | String | ❌ | 캐릭터 외형·성격 설명 (Claude 프롬프트에 삽입됨) |
| `referenceImageUrl` | String | ❌ | 레퍼런스 이미지 URL (FAL.ai FLUX Kontext에 전달되어 씬 간 외형 일관성 유지) |
| `artStyle` | String | ❌ | 화풍 (`anime` / `realistic` / `webtoon` 등) |

---

### 3.4 캐릭터 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |

---

### 3.5 캐릭터 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |
| **설명** | 삭제 시 해당 캐릭터를 참조하는 플롯의 `character_id`는 NULL로 설정됩니다. |

---

## 4. 플롯 (Plot)

**Base Path:** `/api/plots`
> 모든 API에 JWT 토큰이 필요합니다.

---

### 4.1 플롯 생성 (Claude 씬 자동 생성)

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/plots` |
| **상태 코드** | `201 Created` |
| **설명** | 플롯을 생성하고 Claude API가 씬 목록을 자동 생성합니다. (최대 60초 소요) |

#### Request Body

```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "title": "봄날 벚꽃 로맨스",
  "idea": "벚꽃 공원에서 두 사람이 처음 만나 첫눈에 반하는 이야기",
  "artStyle": "anime",
  "characterId": "550e8400-e29b-41d4-a716-446655440001",
  "sceneCount": 5
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `projectId` | String | ✅ | 소속 프로젝트 UUID |
| `title` | String | ✅ | 플롯 제목 |
| `idea` | String | ✅ | 스토리 아이디어 (Claude에 전달) |
| `artStyle` | String | ❌ | 화풍 설정 |
| `characterId` | String | ❌ | 참조 캐릭터 UUID |
| `sceneCount` | Integer | ❌ | 생성할 씬 수 (기본 5, 최소 1, 최대 10) |

#### Response Body

```json
{
  "status": "success",
  "message": "플롯 생성 성공",
  "data": {
    "plotId": "550e8400-e29b-41d4-a716-446655440002",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "봄날 벚꽃 로맨스",
    "idea": "벚꽃 공원에서 두 사람이 처음 만나 첫눈에 반하는 이야기",
    "artStyle": "anime",
    "characterId": "550e8400-e29b-41d4-a716-446655440001",
    "scenes": [
      {
        "sceneNumber": 1,
        "title": "공원 입장",
        "characters": "주인공",
        "composition": "롱샷, 벚꽃 터널",
        "background": "봄날 벚꽃 공원",
        "lighting": "오후 햇살, 따뜻한 톤",
        "mainStory": "주인공이 벚꽃 공원에 들어서는 장면",
        "firstFramePrompt": "A young woman entering a cherry blossom park, long shot",
        "lastFramePrompt": "A young woman standing under cherry blossoms, looking ahead"
      }
    ],
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

---

### 4.2 플롯 단건 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/plots/{plotId}` |
| **상태 코드** | `200 OK` |

---

### 4.3 프로젝트의 플롯 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/plots/project/{projectId}` |
| **상태 코드** | `200 OK` |

---

### 4.4 씬 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/plots/{plotId}/scenes` |
| **상태 코드** | `200 OK` |
| **설명** | 사용자가 씬 표에서 직접 수정한 내용을 저장합니다. |

#### Request Body

```json
[
  {
    "sceneNumber": 1,
    "title": "공원 입장 (수정)",
    "characters": "주인공, 친구",
    "composition": "미디엄샷",
    "background": "봄날 벚꽃 공원",
    "lighting": "오후 햇살",
    "mainStory": "수정된 스토리",
    "firstFramePrompt": "수정된 첫 프레임 프롬프트",
    "lastFramePrompt": "수정된 마지막 프레임 프롬프트"
  }
]
```

---

### 4.5 플롯 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/plots/{plotId}` |
| **상태 코드** | `200 OK` |

---

## 5. 이미지 생성 (Image)

**Base Path:** `/api/images`
> 모든 API에 JWT 토큰이 필요합니다.

생성 요청 즉시 `PENDING` 상태로 응답하고, FAL.ai에서 비동기로 처리됩니다.
완료 여부는 단건 조회 API로 폴링하여 확인합니다.

```
PENDING → PROCESSING → COMPLETED / FAILED
```

---

### 5.1 이미지 생성 요청

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/images/generate` |
| **상태 코드** | `201 Created` |
| **설명** | 씬의 첫/마지막 프레임 이미지를 FAL.ai FLUX Kontext로 생성합니다. |

#### Request Body

```json
{
  "plotId": "550e8400-e29b-41d4-a716-446655440002",
  "sceneNumber": 1,
  "frameType": "FIRST"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `plotId` | String | ✅ | 플롯 UUID |
| `sceneNumber` | Integer | ✅ | 씬 순번 (1부터 시작) |
| `frameType` | String | ✅ | `FIRST` (첫 프레임) 또는 `LAST` (마지막 프레임) |

#### Response Body

```json
{
  "status": "success",
  "message": "이미지 생성을 요청했습니다.",
  "data": {
    "imageId": "550e8400-e29b-41d4-a716-446655440010",
    "plotId": "550e8400-e29b-41d4-a716-446655440002",
    "sceneNumber": 1,
    "frameType": "FIRST",
    "prompt": "A young woman entering a cherry blossom park...",
    "imageUrl": null,
    "status": "PENDING",
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

---

### 5.2 이미지 단건 조회 (상태 폴링)

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/images/{imageId}` |
| **상태 코드** | `200 OK` |
| **설명** | `status`가 `COMPLETED`가 될 때까지 주기적으로 호출합니다. |

#### Response Body

```json
{
  "status": "success",
  "message": "이미지 조회 성공",
  "data": {
    "imageId": "550e8400-e29b-41d4-a716-446655440010",
    "plotId": "550e8400-e29b-41d4-a716-446655440002",
    "sceneNumber": 1,
    "frameType": "FIRST",
    "prompt": "A young woman entering a cherry blossom park...",
    "imageUrl": "https://fal.media/files/...",
    "status": "COMPLETED",
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

| `status` 값 | 의미 |
|-------------|------|
| `PENDING` | 생성 대기 중 |
| `PROCESSING` | FAL.ai 처리 중 |
| `COMPLETED` | 생성 완료 → `imageUrl` 에 결과 URL |
| `FAILED` | 생성 실패 |

---

### 5.3 플롯의 이미지 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/images/plot/{plotId}` |
| **상태 코드** | `200 OK` |
| **설명** | 플롯에 속한 모든 씬의 이미지를 씬 순번 오름차순으로 반환합니다. |

---

## 6. 영상 생성 (Video)

**Base Path:** `/api/videos`
> 모든 API에 JWT 토큰이 필요합니다.

생성 요청 즉시 `PENDING` 상태로 응답하고, FAL.ai Wan 2.1 FLF2V 비동기 큐에서 처리됩니다.
완료 여부는 단건 조회 API로 폴링하여 확인합니다.

```
PENDING → PROCESSING → COMPLETED / FAILED
```

---

### 6.1 영상 생성 요청

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/videos/generate` |
| **상태 코드** | `201 Created` |
| **설명** | 첫/마지막 프레임 이미지로 씬 영상을 생성합니다. Claude가 영상 프롬프트를 자동 생성합니다. |

#### Request Body

```json
{
  "plotId": "550e8400-e29b-41d4-a716-446655440002",
  "sceneNumber": 1,
  "firstImageId": "550e8400-e29b-41d4-a716-446655440010",
  "lastImageId": "550e8400-e29b-41d4-a716-446655440011"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `plotId` | String | ✅ | 플롯 UUID |
| `sceneNumber` | Integer | ✅ | 씬 순번 |
| `firstImageId` | String | ✅ | 첫 프레임 이미지 UUID (status=COMPLETED 필수) |
| `lastImageId` | String | ✅ | 마지막 프레임 이미지 UUID (status=COMPLETED 필수) |

#### Response Body

```json
{
  "status": "success",
  "message": "영상 생성을 요청했습니다.",
  "data": {
    "videoId": "550e8400-e29b-41d4-a716-446655440020",
    "plotId": "550e8400-e29b-41d4-a716-446655440002",
    "sceneNumber": 1,
    "videoType": "SCENE",
    "videoPrompt": "A young woman walks through cherry blossoms, cinematic, anime style...",
    "firstImageId": "550e8400-e29b-41d4-a716-446655440010",
    "lastImageId": "550e8400-e29b-41d4-a716-446655440011",
    "videoUrl": null,
    "duration": null,
    "status": "PENDING",
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

---

### 6.2 영상 단건 조회 (상태 폴링)

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/videos/{videoId}` |
| **상태 코드** | `200 OK` |
| **설명** | `status`가 `COMPLETED`가 될 때까지 주기적으로 호출합니다. |

#### Response Body

```json
{
  "status": "success",
  "message": "영상 조회 성공",
  "data": {
    "videoId": "550e8400-e29b-41d4-a716-446655440020",
    "plotId": "550e8400-e29b-41d4-a716-446655440002",
    "sceneNumber": 1,
    "videoType": "SCENE",
    "videoPrompt": "A young woman walks through cherry blossoms...",
    "firstImageId": "550e8400-e29b-41d4-a716-446655440010",
    "lastImageId": "550e8400-e29b-41d4-a716-446655440011",
    "videoUrl": "https://fal.media/files/...",
    "duration": 5,
    "status": "COMPLETED",
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

| `status` 값 | 의미 |
|-------------|------|
| `PENDING` | 생성 대기 중 |
| `PROCESSING` | FAL.ai 처리 중 (수 분 소요) |
| `COMPLETED` | 생성 완료 → `videoUrl` 에 결과 URL |
| `FAILED` | 생성 실패 |

---

### 6.3 영상 프롬프트 수정 + 재생성

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/videos/{videoId}/prompt` |
| **상태 코드** | `200 OK` |
| **설명** | Claude가 생성한 프롬프트를 수정하고 영상을 재생성합니다. 즉시 `PENDING` 상태로 초기화됩니다. |

#### Request Body

```json
{
  "videoPrompt": "A young woman walks slowly through falling cherry blossoms, dreamy atmosphere, anime style, cinematic"
}
```

---

### 6.4 플롯의 영상 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/videos/plot/{plotId}` |
| **상태 코드** | `200 OK` |
| **설명** | 플롯에 속한 모든 씬의 영상을 씬 순번 오름차순으로 반환합니다. |

---

## 7. Asset (캐릭터·배경 생성 및 저장)

> 모든 API에 JWT 토큰이 필요합니다.

**흐름:**
```
1. POST /api/characters/generate 또는 /api/backgrounds/generate
   → FAL.ai가 이미지 생성 → imageUrl 반환 (DB 저장 X)
2. 마음에 들면 POST /api/projects/{projectId}/assets 로 확정 저장
   → "프로젝트에 사용하기"
```

---

### 7.1 캐릭터 이미지 임시 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/characters/generate` |
| **상태 코드** | `200 OK` |
| **설명** | FAL.ai로 캐릭터 이미지를 생성합니다. DB에 저장하지 않습니다. |

#### Request Body

```json
{
  "prompt": "anime style young woman with long brown hair standing in cherry blossom park",
  "style": "anime",
  "referenceImageUrl": "https://example.com/ref.png"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `prompt` | String | ✅ | 이미지 생성 프롬프트 |
| `style` | String | ❌ | 화풍 (anime, realistic, webtoon 등) |
| `referenceImageUrl` | String | ❌ | 기존 레퍼런스 이미지 URL (외형 일관성 유지) |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 이미지가 생성되었습니다.",
  "data": {
    "imageUrl": "https://fal.media/files/..."
  }
}
```

---

### 7.2 배경 이미지 임시 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/backgrounds/generate` |
| **상태 코드** | `200 OK` |
| **설명** | FAL.ai로 배경 이미지를 생성합니다. DB에 저장하지 않습니다. |

#### Request Body

```json
{
  "prompt": "cherry blossom park in spring, sunny afternoon, soft bokeh",
  "style": "anime"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `prompt` | String | ✅ | 이미지 생성 프롬프트 |
| `style` | String | ❌ | 화풍 |

---

### 7.3 "프로젝트에 사용하기" - Asset 저장

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/projects/{projectId}/assets` |
| **상태 코드** | `201 Created` |
| **설명** | 임시 생성된 이미지를 프로젝트에 확정 저장합니다. |

#### Request Body

```json
{
  "type": "CHARACTER",
  "imageUrl": "https://fal.media/files/...",
  "prompt": "anime style young woman with long brown hair",
  "style": "anime"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `type` | String | ✅ | `CHARACTER` 또는 `BACKGROUND` |
| `imageUrl` | String | ✅ | 임시 생성 API에서 받은 이미지 URL |
| `prompt` | String | ❌ | 생성에 사용한 프롬프트 |
| `style` | String | ❌ | 화풍 |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트에 저장되었습니다.",
  "data": {
    "assetId": "550e8400-e29b-41d4-a716-446655440030",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "CHARACTER",
    "imageUrl": "https://fal.media/files/...",
    "prompt": "anime style young woman with long brown hair",
    "style": "anime",
    "createdAt": "2026-03-20T00:00:00Z"
  }
}
```

---

### 7.4 프로젝트의 Asset 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}/assets` |
| **상태 코드** | `200 OK` |
| **설명** | 프로젝트에 저장된 캐릭터·배경 이미지 목록을 조회합니다. |
