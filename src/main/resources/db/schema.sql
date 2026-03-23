-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

CREATE TABLE public.assets (
  asset_id character varying NOT NULL,
  project_id character varying NOT NULL,
  type character varying NOT NULL CHECK (type::text = ANY (ARRAY['CHARACTER'::character varying, 'BACKGROUND'::character varying]::text[])),
  image_url text NOT NULL,
  prompt text,
  style character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT assets_pkey PRIMARY KEY (asset_id),
  CONSTRAINT assets_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(project_id)
);
CREATE TABLE public.backgrounds (
  background_id character varying NOT NULL,
  project_id character varying,
  name character varying NOT NULL,
  description text,
  reference_image_url character varying,
  art_style character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT backgrounds_pkey PRIMARY KEY (background_id),
  CONSTRAINT background_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(project_id)
);
CREATE TABLE public.characters (
  character_id character varying NOT NULL,
  project_id character varying,
  name character varying NOT NULL,
  description text,
  reference_image_url character varying,
  art_style character varying,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT characters_pkey PRIMARY KEY (character_id),
  CONSTRAINT fk_characters_project FOREIGN KEY (project_id) REFERENCES public.projects(project_id)
);
CREATE TABLE public.images (
  image_id character varying NOT NULL,
  plot_id character varying NOT NULL,
  scene_number integer NOT NULL,
  frame_type character varying NOT NULL CHECK (frame_type::text = ANY (ARRAY['FIRST'::character varying, 'LAST'::character varying]::text[])),
  prompt text,
  image_url character varying,
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying]::text[])),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT images_pkey PRIMARY KEY (image_id),
  CONSTRAINT images_plot_id_fkey FOREIGN KEY (plot_id) REFERENCES public.plots(plot_id)
);
CREATE TABLE public.plots (
  plot_id character varying NOT NULL,
  project_id character varying,
  title character varying NOT NULL,
  idea text NOT NULL,
  art_style character varying,
  character_id character varying,
  scenes_json text,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  background_id character varying,
  CONSTRAINT plots_pkey PRIMARY KEY (plot_id),
  CONSTRAINT plots_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(project_id),
  CONSTRAINT plots_character_id_fkey FOREIGN KEY (character_id) REFERENCES public.characters(character_id),
  CONSTRAINT plots_background_id_fkey FOREIGN KEY (background_id) REFERENCES public.backgrounds(background_id)
);
CREATE TABLE public.project_ratings (
  rating_id character varying NOT NULL,
  project_id character varying NOT NULL,
  user_id character varying NOT NULL,
  rating integer NOT NULL CHECK (rating >= 1 AND rating <= 5),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT project_ratings_pkey PRIMARY KEY (rating_id),
  CONSTRAINT project_ratings_project_id_fkey FOREIGN KEY (project_id) REFERENCES public.projects(project_id),
  CONSTRAINT project_ratings_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id)
);
CREATE TABLE public.projects (
  project_id character varying NOT NULL,
  title character varying NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  background_image_url text,
  user_id character varying,
  settings_json text,
  rating_sum integer NOT NULL DEFAULT 0,
  rating_count integer NOT NULL DEFAULT 0,
  average_rating numeric NOT NULL DEFAULT 0.00,
  CONSTRAINT projects_pkey PRIMARY KEY (project_id),
  CONSTRAINT projects_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(user_id)
);
CREATE TABLE public.users (
  user_id character varying NOT NULL,
  username character varying NOT NULL,
  CONSTRAINT users_pkey PRIMARY KEY (user_id)
);
CREATE TABLE public.videos (
  video_id character varying NOT NULL,
  plot_id character varying,
  scene_number integer,
  video_prompt text,
  first_image_id character varying,
  last_image_id character varying,
  video_url character varying,
  duration integer,
  video_type character varying NOT NULL CHECK (video_type::text = ANY (ARRAY['SCENE'::character varying, 'MERGED'::character varying]::text[])),
  status character varying NOT NULL DEFAULT 'PENDING'::character varying CHECK (status::text = ANY (ARRAY['PENDING'::character varying, 'PROCESSING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying]::text[])),
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  CONSTRAINT videos_pkey PRIMARY KEY (video_id),
  CONSTRAINT videos_plot_id_fkey FOREIGN KEY (plot_id) REFERENCES public.plots(plot_id),
  CONSTRAINT videos_first_image_id_fkey FOREIGN KEY (first_image_id) REFERENCES public.images(image_id),
  CONSTRAINT videos_last_image_id_fkey FOREIGN KEY (last_image_id) REFERENCES public.images(image_id)
);

-- ============================================================
-- 성능 최적화 인덱스
-- PostgreSQL은 FK에 자동 인덱스를 생성하지 않으므로 명시적 생성 필요
-- ============================================================

-- [High Priority] 핫스팟 쿼리 대응 -------------------------

-- 이미지 조회: plot_id + scene_number + frame_type 복합 인덱스
-- 사용처: ContentQueryService에서 씬별 이미지 조회 (7회 호출)
CREATE INDEX idx_images_plot_scene_frame
    ON public.images (plot_id, scene_number, frame_type);

-- 비디오 조회: plot_id + scene_number + video_type + created_at 복합 인덱스
-- 사용처: ContentQueryService에서 최신 씬 비디오 조회 (5회 호출)
CREATE INDEX idx_videos_plot_scene_type_created
    ON public.videos (plot_id, scene_number, video_type, created_at DESC);

-- 플롯 조회: project_id + created_at 복합 인덱스
-- 사용처: 프로젝트별 플롯 목록 조회 (4회 호출)
CREATE INDEX idx_plots_project_created
    ON public.plots (project_id, created_at);

-- [Medium Priority] FK 조회 및 목록/랭킹 -------------------

-- 프로젝트 목록: user_id + created_at 복합 인덱스
-- 사용처: 유저별 프로젝트 목록, 갤러리 조회
CREATE INDEX idx_projects_user_created
    ON public.projects (user_id, created_at DESC);

-- 프로젝트 랭킹: 평점순 정렬 인덱스 (풀테이블 스캔 방지)
-- 사용처: 랭킹 페이지 전체 프로젝트 정렬
CREATE INDEX idx_projects_ranking
    ON public.projects (average_rating DESC, rating_count DESC, created_at DESC);

-- 평점 집계: project_id 인덱스
-- 사용처: 평점 합계/개수 집계 쿼리
CREATE INDEX idx_project_ratings_project
    ON public.project_ratings (project_id);

-- 평점 중복 확인: project_id + user_id 복합 인덱스
-- 사용처: 유저별 프로젝트 평점 존재 여부 조회
CREATE INDEX idx_project_ratings_project_user
    ON public.project_ratings (project_id, user_id);

-- [Low Priority] 기타 FK 조회 ------------------------------

-- 캐릭터: 프로젝트별 캐릭터 조회
CREATE INDEX idx_characters_project
    ON public.characters (project_id);

-- 배경: 프로젝트별 배경 조회
CREATE INDEX idx_backgrounds_project
    ON public.backgrounds (project_id);

-- 에셋: 프로젝트별 에셋 목록 (생성일순)
CREATE INDEX idx_assets_project_created
    ON public.assets (project_id, created_at);

-- 유저: username 검색
CREATE INDEX idx_users_username
    ON public.users (username);