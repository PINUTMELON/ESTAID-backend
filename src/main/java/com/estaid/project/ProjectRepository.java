package com.estaid.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 프로젝트 Repository
 *
 * <p>Spring Data JPA가 기본 CRUD 메서드를 자동으로 생성한다.
 * 기본 제공 메서드: save, findById, findAll, deleteById 등</p>
 *
 * <p>연결 엔티티: {@link Project}</p>
 * <p>기본 키 타입: {@link String} (UUID)</p>
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, String> {
}
