package com.estaid.domain.character.repository;

import com.estaid.domain.character.entity.Character;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 캐릭터 레포지토리
 * - JpaRepository를 상속받아 기본 CRUD를 제공한다.
 * - 추가 쿼리가 필요하면 여기에 메서드를 정의한다.
 */
public interface CharacterRepository extends JpaRepository<Character, String> {
}
