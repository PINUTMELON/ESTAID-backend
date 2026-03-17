package com.estaid.content.repository;

import com.estaid.content.entity.BackgroundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 배경 엔티티 저장소. */
public interface BackgroundRepository extends JpaRepository<BackgroundEntity, String> {
}
