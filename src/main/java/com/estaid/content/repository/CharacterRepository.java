package com.estaid.content.repository;

import com.estaid.content.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** 캐릭터 엔티티 저장소. */
public interface CharacterRepository extends JpaRepository<CharacterEntity, String> {
}
