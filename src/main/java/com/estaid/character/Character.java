package com.estaid.character;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 캐릭터 엔티티
 *
 * <p>사용자가 등록한 원작 캐릭터의 기본 정보를 저장한다.
 * 이미지 생성 시 레퍼런스 이미지로 활용되어 컷씬 간 외형 일관성을 유지한다.</p>
 *
 * <p>DB 테이블: {@code characters}</p>
 */
@Entity
@Table(name = "characters")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Character {

    /**
     * 캐릭터 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "character_id", length = 36)
    private String characterId;

    /**
     * 캐릭터 이름 (예: 강백호, 나루토)
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 캐릭터 외형·성격·특징 등 상세 설명
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 이미지 생성 시 스타일 일관성 유지에 사용하는 레퍼런스 이미지 URL
     */
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    /**
     * 화풍 설정 (예: anime, realistic, webtoon)
     */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /**
     * 레코드 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 레코드 최종 수정 시각 (생성 및 수정 시 자동 갱신)
     */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /**
     * 엔티티 최초 저장 전 호출 - 생성/수정 시각 초기화
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    /**
     * 엔티티 수정 저장 전 호출 - 수정 시각 갱신
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
