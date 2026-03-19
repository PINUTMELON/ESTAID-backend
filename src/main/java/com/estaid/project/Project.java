package com.estaid.project;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 프로젝트 엔티티
 *
 * <p>사용자가 생성하는 작업 단위(프로젝트 파일)를 나타낸다.
 * 하나의 프로젝트 안에 캐릭터, 플롯, 이미지, 영상이 모두 속하며
 * 나중에 이어서 작업할 수 있도록 모든 작업 이력을 프로젝트 단위로 관리한다.</p>
 *
 * <p>예시: "괴물과 싸우는 영상" 프로젝트 / "화장품 광고 영상" 프로젝트</p>
 *
 * <p>DB 테이블: {@code projects}</p>
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {

    /**
     * 프로젝트 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "project_id", length = 36)
    private String projectId;

    /**
     * 프로젝트 제목
     * 예: "괴물과 싸우는 영상", "봄날 벚꽃 로맨스"
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Column(name = "user_id", length = 36)
    private String userId;

    /**
     * 프로젝트 배경 이미지 URL
     * 사용자가 업로드한 배경 이미지로, 이미지 생성 시 배경으로 활용된다.
     */
    @Column(name = "background_image_url", columnDefinition = "TEXT")
    private String backgroundImageUrl;

    /**
     * AI 영상 생성 기본 설정 (JSON 직렬화)
     * 예: {"resolution":"720p","aspectRatio":"16:9","fps":24,"duration":5}
     * 프로젝트 생성 시 기본값을 설정해두면 하위 플롯/영상 생성 시 자동으로 적용된다.
     */
    @Column(name = "settings_json", columnDefinition = "TEXT")
    private String settingsJson;

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
