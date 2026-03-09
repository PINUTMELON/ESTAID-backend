package com.estaid.domain.video.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 영상 엔티티
 * - AI가 생성한 씬별 영상 또는 병합된 최종 영상 정보를 저장한다.
 */
@Entity
@Table(name = "videos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_id", updatable = false, nullable = false)
    private String videoId;

    /** 연결된 플롯 ID */
    @Column(name = "plot_id", length = 36)
    private String plotId;

    /**
     * 씬 순번 (단일 씬 영상인 경우)
     * - 병합 영상의 경우 null
     */
    @Column(name = "scene_number")
    private Integer sceneNumber;

    /** 영상 생성에 사용한 프롬프트 */
    @Column(name = "video_prompt", columnDefinition = "TEXT")
    private String videoPrompt;

    /** 첫 프레임 이미지 ID */
    @Column(name = "first_image_id", length = 36)
    private String firstImageId;

    /** 마지막 프레임 이미지 ID */
    @Column(name = "last_image_id", length = 36)
    private String lastImageId;

    /** 생성된 영상 URL */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /** 영상 길이 (초) */
    @Column(name = "duration")
    private Integer duration;

    /**
     * 영상 종류
     * - SCENE: 씬 단위 영상 (3~5초)
     * - MERGED: 병합 최종 영상 (15~30초)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", nullable = false, length = 10)
    private VideoType videoType;

    /**
     * 생성 상태
     * - PENDING / PROCESSING / COMPLETED / FAILED
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum VideoType { SCENE, MERGED }
    public enum GenerationStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
