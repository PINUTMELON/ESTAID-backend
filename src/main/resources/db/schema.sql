-- =====================================================================
-- ESTAID Supabase Schema
-- DB: PostgreSQL (Supabase)
-- Description: Table definitions used by the ESTAID backend.
--
-- Table structure:
--   1. projects
--   2. backgrounds
--   3. characters
--   4. plots
--   5. images
--   6. videos
--
-- Run: Supabase Dashboard > SQL Editor > paste entire file > Run
-- =====================================================================

-- =====================================================================
-- 1. projects
-- =====================================================================
CREATE TABLE IF NOT EXISTS projects (
    project_id   VARCHAR(36) PRIMARY KEY,
    title        VARCHAR(200) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- =====================================================================
-- 2. backgrounds
-- =====================================================================
CREATE TABLE IF NOT EXISTS backgrounds (
    background_id        VARCHAR(36) PRIMARY KEY,
    project_id           VARCHAR(36),
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    reference_image_url  VARCHAR(500),
    art_style            VARCHAR(50),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT background_project_id_fkey
        FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

-- =====================================================================
-- 3. characters
-- =====================================================================
CREATE TABLE IF NOT EXISTS characters (
    character_id         VARCHAR(36) PRIMARY KEY,
    project_id           VARCHAR(36),
    name                 VARCHAR(100) NOT NULL,
    description          TEXT,
    reference_image_url  VARCHAR(500),
    art_style            VARCHAR(50),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_characters_project
        FOREIGN KEY (project_id) REFERENCES projects(project_id)
);

-- ---------------------------------------------------------------------
-- updated_at trigger function
-- ---------------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_projects_updated_at ON projects;
CREATE TRIGGER trg_projects_updated_at
    BEFORE UPDATE ON projects
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

DROP TRIGGER IF EXISTS trg_backgrounds_updated_at ON backgrounds;
CREATE TRIGGER trg_backgrounds_updated_at
    BEFORE UPDATE ON backgrounds
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

DROP TRIGGER IF EXISTS trg_characters_updated_at ON characters;
CREATE TRIGGER trg_characters_updated_at
    BEFORE UPDATE ON characters
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- =====================================================================
-- 4. plots
-- =====================================================================
CREATE TABLE IF NOT EXISTS plots (
    plot_id        VARCHAR(36) PRIMARY KEY,
    project_id     VARCHAR(36),
    title          VARCHAR(200) NOT NULL,
    idea           TEXT NOT NULL,
    art_style      VARCHAR(50),
    character_id   VARCHAR(36),
    scenes_json    TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    background_id  VARCHAR(36),
    CONSTRAINT plots_project_id_fkey
        FOREIGN KEY (project_id) REFERENCES projects(project_id),
    CONSTRAINT plots_character_id_fkey
        FOREIGN KEY (character_id) REFERENCES characters(character_id),
    CONSTRAINT plots_background_id_fkey
        FOREIGN KEY (background_id) REFERENCES backgrounds(background_id)
);

-- =====================================================================
-- 5. images
-- =====================================================================
CREATE TABLE IF NOT EXISTS images (
    image_id      VARCHAR(36) PRIMARY KEY,
    plot_id       VARCHAR(36) NOT NULL,
    scene_number  INT NOT NULL,
    frame_type    VARCHAR(10) NOT NULL
                  CHECK (frame_type IN ('FIRST', 'LAST')),
    prompt        TEXT,
    image_url     VARCHAR(500),
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                  CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT images_plot_id_fkey
        FOREIGN KEY (plot_id) REFERENCES plots(plot_id)
);

-- =====================================================================
-- 6. videos
-- =====================================================================
CREATE TABLE IF NOT EXISTS videos (
    video_id        VARCHAR(36) PRIMARY KEY,
    plot_id         VARCHAR(36),
    scene_number    INT,
    video_prompt    TEXT,
    first_image_id  VARCHAR(36),
    last_image_id   VARCHAR(36),
    video_url       VARCHAR(500),
    duration        INT,
    video_type      VARCHAR(10) NOT NULL
                    CHECK (video_type IN ('SCENE', 'MERGED')),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                    CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT videos_plot_id_fkey
        FOREIGN KEY (plot_id) REFERENCES plots(plot_id),
    CONSTRAINT videos_first_image_id_fkey
        FOREIGN KEY (first_image_id) REFERENCES images(image_id),
    CONSTRAINT videos_last_image_id_fkey
        FOREIGN KEY (last_image_id) REFERENCES images(image_id)
);
