package com.estaid.domain.image.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 이미지 엔티티
 * - AI가 생성한 씬별 이미지 정보를 저장한다.
 * - 첫 프레임(firstFrame)과 마지막 프레임(lastFrame) 두 장이 한 씬에 대응된다.
 */
@Entity
@Table(name = "images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "image_id", updatable = false, nullable = false)
    private String imageId;

    /** 연결된 플롯 ID */
    @Column(name = "plot_id", nullable = false, length = 36)
    private String plotId;

    /** 씬 순번 */
    @Column(name = "scene_number", nullable = false)
    private int sceneNumber;

    /**
     * 이미지 프레임 종류
     * - FIRST: 첫 프레임
     * - LAST: 마지막 프레임
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type", nullable = false, length = 10)
    private FrameType frameType;

    /** 사용한 이미지 생성 프롬프트 */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /** 생성된 이미지 URL (외부 스토리지 또는 AI 서비스 URL) */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 생성 상태
     * - PENDING: 생성 대기
     * - PROCESSING: 생성 중
     * - COMPLETED: 완료
     * - FAILED: 실패
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum FrameType { FIRST, LAST }
    public enum GenerationStatus { PENDING, PROCESSING, COMPLETED, FAILED }
}
