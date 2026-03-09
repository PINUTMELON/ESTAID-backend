-- =============================
-- ESTAID Supabase Schema
-- PostgreSQL / Supabase
-- =============================

-- 1. characters
CREATE TABLE IF NOT EXISTS characters (
    character_id        VARCHAR(36)  PRIMARY KEY,
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

-- 2. plots
CREATE TABLE IF NOT EXISTS plots (
    plot_id      VARCHAR(36)  PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    idea         TEXT         NOT NULL,
    art_style    VARCHAR(50),
    character_id VARCHAR(36)  REFERENCES characters(character_id) ON DELETE SET NULL,
    scenes_json  TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- 3. images
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

-- 4. videos
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
