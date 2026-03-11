-- =====================================================================
-- ESTAID Supabase Schema
-- DB: PostgreSQL (Supabase)
-- 작성일: 2026-03-09
-- 설명: AI 기반 2차 창작 컨텐츠 생성 플랫폼 ESTAID의 테이블 정의
-- =====================================================================

-- =====================================================================
-- 1. characters 테이블
-- 설명: 프로젝트에 속한 캐릭터 기본 정보를 저장한다.
--       이미지 생성 시 레퍼런스 이미지로 활용되어 외형 일관성을 유지한다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS characters (
    -- 캐릭터 고유 식별자 (Java UUID -> 36자리 문자열)
    character_id        VARCHAR(36)  PRIMARY KEY,

    -- 소속 프로젝트 ID
    project_id          VARCHAR(36),

    -- 캐릭터 이름
    name                VARCHAR(100) NOT NULL,

    -- 캐릭터 외형, 성격, 특징 등 상세 설명
    description         TEXT,

    -- 레퍼런스 이미지 URL
    reference_image_url VARCHAR(500),

    -- 화풍 설정 (예: anime, realistic, webtoon)
    art_style           VARCHAR(50),

    -- 레코드 생성 시각
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 레코드 최종 수정 시각
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- -------------------------------------------------------
-- updated_at 자동 갱신 함수
-- 설명: UPDATE 발생 시 updated_at 컬럼을 현재 시각으로 갱신한다.
-- -------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- -------------------------------------------------------
-- characters updated_at 트리거
-- -------------------------------------------------------
DROP TRIGGER IF EXISTS trg_characters_updated_at ON characters;
CREATE TRIGGER trg_characters_updated_at
    BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =====================================================================
-- 2. projects 테이블
-- 설명: 관리자가 여러 프로젝트를 생성하고 관리한다.
--       프로젝트에는 제목, 배경 이미지, 생성 기본 설정이 저장된다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS projects (
    -- 프로젝트 고유 식별자
    project_id           VARCHAR(36)  PRIMARY KEY,

    -- 프로젝트 제목
    title                VARCHAR(200) NOT NULL,

    -- 배경 사진 URL
    background_image_url TEXT,

    -- AI 영상 생성 기본 설정 JSON
    -- 예: 해상도, 종횡비, 프레임 등
    settings_json        TEXT,

    -- 레코드 생성 시각
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),

    -- 레코드 최종 수정 시각
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =====================================================================
-- 3. plots 테이블
-- 설명: 사용자가 입력한 아이디어를 바탕으로 AI가 생성한 스토리 플롯을 저장한다.
--       씬 목록(scenes_json)은 JSON 문자열로 직렬화하여 저장한다.
--       프로젝트 하위 데이터로 관리된다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS plots (
    -- 플롯 고유 식별자
    plot_id      VARCHAR(36)  PRIMARY KEY,

    -- 소속 프로젝트 ID
    project_id   VARCHAR(36)  NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,

    -- 플롯 제목
    title        VARCHAR(200) NOT NULL,

    -- 사용자가 입력한 원본 아이디어 텍스트
    idea         TEXT         NOT NULL,

    -- 화풍 설정
    art_style    VARCHAR(50),

    -- 참조 캐릭터 ID
    character_id VARCHAR(36)  REFERENCES characters(character_id) ON DELETE SET NULL,

    -- AI가 생성한 씬 목록 JSON
    scenes_json  TEXT,

    -- 레코드 생성 시각
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE characters
    DROP CONSTRAINT IF EXISTS fk_characters_project;

ALTER TABLE characters
    ADD CONSTRAINT fk_characters_project
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE;

-- =====================================================================
-- 4. images 테이블
-- 설명: AI가 생성한 씬별 이미지 정보를 저장한다.
--       한 씬당 첫 프레임(FIRST)과 마지막 프레임(LAST) 두 장이 생성된다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS images (
    -- 이미지 고유 식별자
    image_id     VARCHAR(36) PRIMARY KEY,

    -- 연결된 플롯 ID
    plot_id      VARCHAR(36) NOT NULL REFERENCES plots(plot_id) ON DELETE CASCADE,

    -- 씬 순번
    scene_number INT         NOT NULL,

    -- 이미지 프레임 종류
    frame_type   VARCHAR(10) NOT NULL CHECK (frame_type IN ('FIRST', 'LAST')),

    -- 이미지 생성에 사용한 프롬프트
    prompt       TEXT,

    -- 생성된 이미지 URL
    image_url    VARCHAR(500),

    -- 이미지 생성 상태
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    -- 레코드 생성 시각
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- 5. videos 테이블
-- 설명: AI가 생성한 씬별 영상 및 최종 병합 영상 정보를 저장한다.
--       SCENE 타입은 씬 단위, MERGED 타입은 최종 병합 영상이다.
-- =====================================================================
CREATE TABLE IF NOT EXISTS videos (
    -- 영상 고유 식별자
    video_id       VARCHAR(36) PRIMARY KEY,

    -- 연결된 플롯 ID
    plot_id        VARCHAR(36) REFERENCES plots(plot_id) ON DELETE CASCADE,

    -- 씬 순번
    scene_number   INT,

    -- 영상 생성에 사용한 프롬프트
    video_prompt   TEXT,

    -- 시작 프레임 이미지 ID
    first_image_id VARCHAR(36) REFERENCES images(image_id) ON DELETE SET NULL,

    -- 끝 프레임 이미지 ID
    last_image_id  VARCHAR(36) REFERENCES images(image_id) ON DELETE SET NULL,

    -- 생성된 영상 URL
    video_url      VARCHAR(500),

    -- 영상 길이(초)
    duration       INT,

    -- 영상 종류
    video_type     VARCHAR(10) NOT NULL CHECK (video_type IN ('SCENE', 'MERGED')),

    -- 영상 생성 상태
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    -- 레코드 생성 시각
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
