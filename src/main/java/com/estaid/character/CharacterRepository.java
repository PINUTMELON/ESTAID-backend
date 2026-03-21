package com.estaid.character;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 캐릭터 Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD 메서드({@code save}, {@code findById}, {@code findAll}, {@code deleteById} 등)를
 * {@link JpaRepository}에서 상속한다.</p>
 */
@Repository
public interface CharacterRepository extends JpaRepository<Character, String> {

    /**
     * 특정 프로젝트에 속한 캐릭터 목록 조회
     *
     * @param projectId 프로젝트 UUID
     * @return 해당 프로젝트의 캐릭터 목록
     */
    List<Character> findByProject_ProjectId(String projectId);
}
