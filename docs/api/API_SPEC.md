# ESTAID Backend API 명세서

> **Base URL:** `http://localhost:8080`
> **응답 형식:** 모든 API는 `ApiResponse<T>` 래퍼로 응답합니다.
> **최종 수정:** 2026-03-18

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

---

## 1. 인증 (Auth)

**Base Path:** `/auth`

---

### 1.1 로그인

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/auth/login` |
| **상태 코드** | `200 OK` |
| **설명** | 사용자를 인증하고 서버 세션을 생성합니다. |

#### Request Body

```json
{
  "username": "admin",
  "password": "admin1234"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `username` | String | ✅ | `@NotBlank` | 사용자명 |
| `password` | String | ✅ | `@NotBlank` | 비밀번호 |

#### Response Body

```json
{
  "status": "success",
  "message": "로그인 성공",
  "data": {
    "username": "admin"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `data.username` | String | 로그인한 사용자명 |

---

### 1.2 로그아웃

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/auth/logout` |
| **상태 코드** | `200 OK` |
| **설명** | 현재 사용자 세션을 종료합니다. |

#### Request Body
없음

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

---

### 2.1 프로젝트 전체 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects` |
| **상태 코드** | `200 OK` |
| **설명** | 등록된 모든 프로젝트를 조회합니다. |

#### Request
없음

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
      "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\",\"fps\":24,\"duration\":5}",
      "createdAt": "2026-03-18T00:00:00Z",
      "updatedAt": "2026-03-18T00:00:00Z"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `data[].projectId` | String (UUID) | 프로젝트 고유 식별자 |
| `data[].title` | String | 프로젝트 제목 |
| `data[].backgroundImageUrl` | String | 배경 이미지 URL |
| `data[].settingsJson` | String (JSON) | AI 영상 생성 기본 설정 |
| `data[].createdAt` | OffsetDateTime | 생성 시각 |
| `data[].updatedAt` | OffsetDateTime | 최종 수정 시각 |

---

### 2.2 프로젝트 단건 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |
| **설명** | 특정 프로젝트를 단건 조회합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 조회할 프로젝트 UUID |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트 조회 성공",
  "data": {
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "괴물과 싸우는 영상",
    "backgroundImageUrl": "https://example.com/bg.png",
    "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\",\"fps\":24,\"duration\":5}",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T00:00:00Z"
  }
}
```

---

### 2.3 프로젝트 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/projects` |
| **상태 코드** | `201 Created` |
| **설명** | 새 프로젝트를 생성합니다. |

#### Request Body

```json
{
  "title": "봄날 벚꽃 로맨스",
  "backgroundImageUrl": "https://example.com/bg.png",
  "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\",\"fps\":24,\"duration\":5}"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `title` | String | ✅ | `@NotBlank`, 최대 200자 | 프로젝트 제목 |
| `backgroundImageUrl` | String | ❌ | - | 배경 이미지 URL |
| `settingsJson` | String (JSON) | ❌ | - | AI 영상 생성 기본 설정 JSON |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트 생성 성공",
  "data": {
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "봄날 벚꽃 로맨스",
    "backgroundImageUrl": "https://example.com/bg.png",
    "settingsJson": "{\"resolution\":\"720p\",\"aspectRatio\":\"16:9\",\"fps\":24,\"duration\":5}",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T00:00:00Z"
  }
}
```

---

### 2.4 프로젝트 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |
| **설명** | 기존 프로젝트 정보를 수정합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 수정할 프로젝트 UUID |

#### Request Body

```json
{
  "title": "수정된 프로젝트 제목",
  "backgroundImageUrl": "https://example.com/new-bg.png",
  "settingsJson": "{\"resolution\":\"1080p\",\"aspectRatio\":\"16:9\",\"fps\":30,\"duration\":10}"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `title` | String | ✅ | `@NotBlank`, 최대 200자 | 프로젝트 제목 |
| `backgroundImageUrl` | String | ❌ | - | 배경 이미지 URL |
| `settingsJson` | String (JSON) | ❌ | - | AI 영상 생성 기본 설정 JSON |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트 수정 성공",
  "data": {
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "수정된 프로젝트 제목",
    "backgroundImageUrl": "https://example.com/new-bg.png",
    "settingsJson": "{\"resolution\":\"1080p\",\"aspectRatio\":\"16:9\",\"fps\":30,\"duration\":10}",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T12:00:00Z"
  }
}
```

---

### 2.5 프로젝트 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/projects/{projectId}` |
| **상태 코드** | `200 OK` |
| **설명** | 프로젝트를 삭제합니다. 연결된 캐릭터, 플롯, 이미지, 영상도 CASCADE 삭제됩니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 삭제할 프로젝트 UUID |

#### Response Body

```json
{
  "status": "success",
  "message": "프로젝트 삭제 성공",
  "data": null
}
```

---

## 3. 캐릭터 (Character)

**Base Path:** `/api/projects/{projectId}/characters`

---

### 3.1 캐릭터 전체 목록 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}/characters` |
| **상태 코드** | `200 OK` |
| **설명** | 특정 프로젝트에 속한 모든 캐릭터를 조회합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 프로젝트 UUID |

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
      "description": "20대 초반 여성, 긴 갈색 머리, 청순하고 밝은 이미지",
      "referenceImageUrl": "https://example.com/ref.png",
      "artStyle": "anime",
      "createdAt": "2026-03-18T00:00:00Z",
      "updatedAt": "2026-03-18T00:00:00Z"
    }
  ]
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `data[].characterId` | String (UUID) | 캐릭터 고유 식별자 |
| `data[].projectId` | String (UUID) | 소속 프로젝트 UUID |
| `data[].name` | String | 캐릭터 이름 |
| `data[].description` | String | 캐릭터 외형·성격·특징 설명 (씬 생성 프롬프트에 활용) |
| `data[].referenceImageUrl` | String | 레퍼런스 이미지 URL (FAL.ai FLUX Kontext에 전달) |
| `data[].artStyle` | String | 화풍 설정 (`anime` / `realistic` / `webtoon` 등) |
| `data[].createdAt` | OffsetDateTime | 생성 시각 |
| `data[].updatedAt` | OffsetDateTime | 최종 수정 시각 |

---

### 3.2 캐릭터 단건 조회

| 항목 | 내용 |
|------|------|
| **Method** | `GET` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |
| **설명** | 특정 캐릭터를 단건 조회합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 프로젝트 UUID |
| `characterId` | String (UUID) | 조회할 캐릭터 UUID |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 조회 성공",
  "data": {
    "characterId": "550e8400-e29b-41d4-a716-446655440001",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "아이유",
    "description": "20대 초반 여성, 긴 갈색 머리, 청순하고 밝은 이미지",
    "referenceImageUrl": "https://example.com/ref.png",
    "artStyle": "anime",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T00:00:00Z"
  }
}
```

---

### 3.3 캐릭터 생성

| 항목 | 내용 |
|------|------|
| **Method** | `POST` |
| **URL** | `/api/projects/{projectId}/characters` |
| **상태 코드** | `201 Created` |
| **설명** | 프로젝트에 새 캐릭터를 추가합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 프로젝트 UUID |

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

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `projectId` | String (UUID) | ✅ | `@NotBlank` | 소속 프로젝트 UUID |
| `name` | String | ✅ | `@NotBlank`, 최대 100자 | 캐릭터 이름 |
| `description` | String | ❌ | - | 캐릭터 외형·성격·특징 설명. Claude API 씬 생성 프롬프트에 삽입됨 |
| `referenceImageUrl` | String | ❌ | - | 레퍼런스 이미지 URL. FAL.ai FLUX Kontext의 `image_url`로 전달되어 씬 간 캐릭터 외형 일관성 유지 |
| `artStyle` | String | ❌ | - | 화풍 설정. 이미지 생성 프롬프트에 `style: {artStyle}` 형태로 삽입 |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 생성 성공",
  "data": {
    "characterId": "550e8400-e29b-41d4-a716-446655440001",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "강백호",
    "description": "10대 후반 남성, 빨간 머리, 농구를 좋아하는 열정적인 성격",
    "referenceImageUrl": "https://example.com/ref.png",
    "artStyle": "webtoon",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T00:00:00Z"
  }
}
```

---

### 3.4 캐릭터 수정

| 항목 | 내용 |
|------|------|
| **Method** | `PUT` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |
| **설명** | 캐릭터 정보를 수정합니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 프로젝트 UUID |
| `characterId` | String (UUID) | 수정할 캐릭터 UUID |

#### Request Body

```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "강백호 (수정)",
  "description": "수정된 캐릭터 설명",
  "referenceImageUrl": "https://example.com/new-ref.png",
  "artStyle": "realistic"
}
```

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `projectId` | String (UUID) | ✅ | `@NotBlank` | 소속 프로젝트 UUID |
| `name` | String | ✅ | `@NotBlank`, 최대 100자 | 캐릭터 이름 |
| `description` | String | ❌ | - | 캐릭터 외형·성격·특징 설명 |
| `referenceImageUrl` | String | ❌ | - | 레퍼런스 이미지 URL |
| `artStyle` | String | ❌ | - | 화풍 설정 |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 수정 성공",
  "data": {
    "characterId": "550e8400-e29b-41d4-a716-446655440001",
    "projectId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "강백호 (수정)",
    "description": "수정된 캐릭터 설명",
    "referenceImageUrl": "https://example.com/new-ref.png",
    "artStyle": "realistic",
    "createdAt": "2026-03-18T00:00:00Z",
    "updatedAt": "2026-03-18T12:00:00Z"
  }
}
```

---

### 3.5 캐릭터 삭제

| 항목 | 내용 |
|------|------|
| **Method** | `DELETE` |
| **URL** | `/api/projects/{projectId}/characters/{characterId}` |
| **상태 코드** | `200 OK` |
| **설명** | 캐릭터를 삭제합니다. 해당 캐릭터를 참조하는 플롯의 `character_id`는 `NULL`로 설정됩니다. |

#### Path Variable

| 변수 | 타입 | 설명 |
|------|------|------|
| `projectId` | String (UUID) | 프로젝트 UUID |
| `characterId` | String (UUID) | 삭제할 캐릭터 UUID |

#### Response Body

```json
{
  "status": "success",
  "message": "캐릭터 삭제 성공",
  "data": null
}
```
