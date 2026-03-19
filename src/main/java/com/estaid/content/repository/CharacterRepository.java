package com.estaid.content.repository;

import com.estaid.content.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 캐릭터 엔티티 저장소 (조회 전용). */
@Repository("contentCharacterRepository")
public interface CharacterRepository extends JpaRepository<CharacterEntity, String> {
}
