-- WARNING: This schema is for context only and is not meant to be run.
-- Table order and constraints may not be valid for execution.

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
CREATE TABLE public.projects (
  project_id character varying NOT NULL,
  title character varying NOT NULL,
  created_at timestamp with time zone NOT NULL DEFAULT now(),
  updated_at timestamp with time zone NOT NULL DEFAULT now(),
  background_image_url text,
  user_id character varying,
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