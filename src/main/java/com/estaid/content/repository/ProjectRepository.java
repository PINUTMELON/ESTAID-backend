package com.estaid.content.repository;

import com.estaid.content.entity.ProjectEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 프로젝트 엔티티 저장소. */
public interface ProjectRepository extends JpaRepository<ProjectEntity, String> {
}
