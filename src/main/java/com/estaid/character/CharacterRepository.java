package com.estaid.character;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 캐릭터 Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD 메서드({@code save}, {@code findById}, {@code findAll}, {@code deleteById} 등)를
 * {@link JpaRepository}에서 상속한다.</p>
 */
public interface CharacterRepository extends JpaRepository<Character, String> {
}
