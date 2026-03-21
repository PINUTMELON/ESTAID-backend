package com.estaid.character;

import com.estaid.project.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 캐릭터 엔티티
 *
 * <p>사용자가 등록한 원작 캐릭터의 기본 정보를 저장한다.
 * 이미지 생성 시 레퍼런스 이미지로 활용되어 컷씬 간 외형 일관성을 유지한다.</p>
 *
 * <p>하나의 프로젝트({@link Project})에 속하며,
 * 캐릭터에 등록된 레퍼런스 이미지가 FAL.ai FLUX Kontext 호출 시
 * 조건 이미지로 전달되어 씬마다 동일한 외형(얼굴+옷+체형)이 유지된다.</p>
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
     * 소속 프로젝트
     * 프로젝트 삭제 시 캐릭터도 함께 삭제된다 (ON DELETE CASCADE).
     * LAZY 로딩: 캐릭터 조회 시 프로젝트 정보는 실제 접근 시점에 로드된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * 캐릭터 이름 (예: 강백호, 나루토, 아이유)
     */
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    /**
     * 캐릭터 외형·성격·특징 등 상세 설명
     * Claude API 호출 시 씬 생성 프롬프트에 포함되어 캐릭터 묘사에 활용된다.
     * 예: "20대 초반 여성, 긴 갈색 머리, 청순하고 밝은 이미지"
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 레퍼런스 이미지 URL (사용자가 업로드한 캐릭터 참조 이미지)
     * FAL.ai FLUX Kontext의 image_url 파라미터로 전달되어
     * 모든 씬에서 동일한 캐릭터 외형(얼굴+옷+체형+스타일)을 유지하는 핵심 값이다.
     */
    @Column(name = "reference_image_url", length = 500)
    private String referenceImageUrl;

    /**
     * 화풍 설정 (예: anime, realistic, webtoon)
     * 이미지 생성 프롬프트에 "style: {artStyle}" 형태로 삽입된다.
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
