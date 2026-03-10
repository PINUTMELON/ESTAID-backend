package com.estaid.plot;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 플롯 Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD 메서드({@code save}, {@code findById}, {@code findAll}, {@code deleteById} 등)를
 * {@link JpaRepository}에서 상속한다.</p>
 */
public interface PlotRepository extends JpaRepository<Plot, String> {

    /**
     * 특정 캐릭터에 연결된 플롯 목록을 조회한다.
     *
     * @param characterId 캐릭터 고유 식별자
     * @return 해당 캐릭터의 플롯 목록
     */
    List<Plot> findByCharacter_CharacterId(String characterId);
}
