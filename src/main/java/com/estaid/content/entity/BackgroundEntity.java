package com.estaid.content.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

/**
 * backgrounds 테이블 매핑 엔티티
 *
 * <p>배경 자산 정보를 저장한다. 캐릭터(characters 테이블)와 동일한 구조를 가지며,
 * 프로젝트({@code project_id})에 속한다.</p>
 *
 * <p>DB 테이블: {@code backgrounds}</p>
 *
 * <p>주요 필드:</p>
 * <ul>
 *   <li>backgroundId      - UUID (자동 생성)</li>
 *   <li>projectId         - 소속 프로젝트 UUID</li>
 *   <li>name              - 배경 이름</li>
 *   <li>description       - 배경 설명</li>
 *   <li>referenceImageUrl - 참조 이미지 URL (Supabase Storage)</li>
 *   <li>artStyle          - 화풍</li>
 * </ul>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "backgrounds")
public class BackgroundEntity {

    /** 배경 고유 식별자 (UUID, 자동 생성) */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_id", nullable = false, length = 36)
    private String backgroundId;

    /** 소속 프로젝트 UUID */
    @Column(name = "project_id", length = 36)
    private String projectId;

    /** 배경 이름 (예: "어두운 숲", "도심 옥상") */
    @Column(name = "name", nullable = false)
    private String name;

    /** 배경 설명 (사용자 입력 또는 AI 생성 상세 묘사) */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** 참조 이미지 URL (Supabase Storage에 업로드된 원본 이미지) */
    @Column(name = "reference_image_url")
    private String referenceImageUrl;

    /** 화풍 (예: REALISTIC, ANIME, 3D, PAINT, SKETCH) */
    @Column(name = "art_style")
    private String artStyle;

    /** 생성 시각 (자동 설정, 수정 불가) */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 엔티티 저장 전 생성/수정 시각을 자동으로 설정한다. */
    @PrePersist
    private void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 엔티티 수정 전 수정 시각을 자동으로 갱신한다. */
    @PreUpdate
    private void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
