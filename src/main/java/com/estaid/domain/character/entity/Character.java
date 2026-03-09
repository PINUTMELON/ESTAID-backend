package com.estaid.domain.character.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 캐릭터 엔티티
 * - 사용자가 등록한 원작 캐릭터 정보를 저장한다.
 * - 이미지 생성 시 레퍼런스 이미지로 활용되어 컷씬 간 외형 일관성을 유지한다.
 */
@Entity
@Table(name = "characters")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Character {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "character_id", updatable = false, nullable = false)
    private String characterId;

    /** 캐릭터 이름 (예: 강백호, 나루토) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 캐릭터 설명 (외형, 성격, 특징 등) */
    @Column(columnDefinition = "TEXT")
    private String description;

    /** 레퍼런스 이미지 URL - 이미지 생성 시 스타일 일관성 유지에 사용 */
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    /** 화풍 (예: anime, realistic, webtoon) */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
