package com.estaid.content.repository;

import com.estaid.content.entity.BackgroundEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 배경(BackgroundEntity) Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD는 {@link JpaRepository}에서 상속한다.</p>
 */
public interface BackgroundRepository extends JpaRepository<BackgroundEntity, String> {

    /**
     * 특정 프로젝트에 속한 배경 목록을 조회한다.
     *
     * @param projectId 프로젝트 UUID
     * @return 해당 프로젝트의 배경 목록
     */
    List<BackgroundEntity> findByProjectId(String projectId);
}
