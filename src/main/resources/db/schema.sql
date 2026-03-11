-- =============================
-- ESTAID Supabase Schema
-- PostgreSQL / Supabase
-- =============================

-- 1. characters
CREATE TABLE IF NOT EXISTS characters (
    character_id        VARCHAR(36)  PRIMARY KEY,
    project_id          VARCHAR(36),
    name                VARCHAR(100) NOT NULL,
    description         TEXT,
    reference_image_url VARCHAR(500),
    art_style           VARCHAR(50),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- updated_at 자동 갱신 트리거
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_characters_updated_at ON characters;
CREATE TRIGGER trg_characters_updated_at
    BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

ALTER TABLE characters
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(36);

-- 2. projects 사용자가 여러 프로젝트를 관리
CREATE TABLE IF NOT EXISTS projects (
    project_id     VARCHAR(36)  PRIMARY KEY, -- 프로젝트 id
    title          VARCHAR(200) NOT NULL, -- 프로젝트 제목
    background_image_url TEXT, -- 배경 사진 URL
    settings_json  TEXT, -- 기본설정: ai api 영상 생성 설정 (해상도, 종횡비, 프레임 등) 이거는 api 통해서 
                        -- 영상 만들때 기본 설정 가능한것들을 확인해 주셔야 할거같아요.. 
                        --필요하면 setting table을 따로 만들어야 할것같아요
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- 3. plots
CREATE TABLE IF NOT EXISTS plots (
    plot_id      VARCHAR(36)  PRIMARY KEY,
    project_id   VARCHAR(36)  NOT NULL REFERENCES projects(project_id) ON DELETE CASCADE,
    title        VARCHAR(200) NOT NULL,
    idea         TEXT         NOT NULL,
    art_style    VARCHAR(50),
    character_id VARCHAR(36)  REFERENCES characters(character_id) ON DELETE SET NULL,
    scenes_json  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE plots
    ADD COLUMN IF NOT EXISTS project_id VARCHAR(36);

ALTER TABLE characters
    DROP CONSTRAINT IF EXISTS fk_characters_project;

ALTER TABLE characters
    ADD CONSTRAINT fk_characters_project
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE;

-- 4. images
CREATE TABLE IF NOT EXISTS images (
    image_id     VARCHAR(36) PRIMARY KEY,
    plot_id      VARCHAR(36) NOT NULL REFERENCES plots(plot_id) ON DELETE CASCADE,
    scene_number INT         NOT NULL,
    frame_type   VARCHAR(10) NOT NULL CHECK (frame_type IN ('FIRST', 'LAST')),
    prompt       TEXT,
    image_url    VARCHAR(500),
    status       VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 5. videos
CREATE TABLE IF NOT EXISTS videos (
    video_id       VARCHAR(36) PRIMARY KEY,
    plot_id        VARCHAR(36) REFERENCES plots(plot_id) ON DELETE CASCADE,
    scene_number   INT,
    video_prompt   TEXT,
    first_image_id VARCHAR(36) REFERENCES images(image_id) ON DELETE SET NULL,
    last_image_id  VARCHAR(36) REFERENCES images(image_id) ON DELETE SET NULL,
    video_url      VARCHAR(500),
    duration       INT,
    video_type     VARCHAR(10) NOT NULL CHECK (video_type IN ('SCENE', 'MERGED')),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
