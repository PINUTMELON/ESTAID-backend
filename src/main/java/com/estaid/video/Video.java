package com.estaid.video;

import com.estaid.image.Image;
import com.estaid.plot.Plot;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 영상 엔티티
 *
 * <p>AI가 생성한 씬별 영상 및 최종 병합 영상 정보를 저장한다.</p>
 * <ul>
 *   <li>SCENE 타입: 씬 단위 영상 (3~5초, first/last 이미지로 생성)</li>
 *   <li>MERGED 타입: 씬 영상들을 이어 붙인 최종 영상 (15~30초)</li>
 * </ul>
 *
 * <p>DB 테이블: {@code videos}</p>
 */
@Entity
@Table(name = "videos")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Video {

    /**
     * 영상 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "video_id", length = 36)
    private String videoId;

    /**
     * 연결된 플롯 (플롯 삭제 시 영상도 함께 삭제됨)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id")
    private Plot plot;

    /**
     * 씬 순번 (SCENE 타입일 때만 사용, MERGED 타입은 NULL)
     */
    @Column(name = "scene_number")
    private Integer sceneNumber;

    /**
     * 영상 생성에 사용한 AI 프롬프트
     */
    @Column(name = "video_prompt", columnDefinition = "TEXT")
    private String videoPrompt;

    /**
     * 영상의 시작 프레임 이미지 (이미지 삭제 시 NULL로 설정됨)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "first_image_id")
    private Image firstImage;

    /**
     * 영상의 끝 프레임 이미지 (이미지 삭제 시 NULL로 설정됨)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_image_id")
    private Image lastImage;

    /**
     * 생성된 영상의 외부 URL (AI 서비스 또는 스토리지 URL)
     */
    @Column(name = "video_url", length = 500)
    private String videoUrl;

    /**
     * 영상 길이 (단위: 초)
     */
    @Column(name = "duration")
    private Integer duration;

    /**
     * 영상 종류 (SCENE: 씬 단위 / MERGED: 최종 병합)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "video_type", length = 10, nullable = false)
    private VideoType videoType;

    /**
     * 영상 생성 상태 (기본값: PENDING)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private GenerationStatus status = GenerationStatus.PENDING;

    /**
     * 레코드 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 엔티티 최초 저장 전 호출 - 생성 시각 초기화
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }

    /**
     * 영상 종류
     *
     * <ul>
     *   <li>{@link #SCENE}  - 씬 단위 영상 (3~5초)</li>
     *   <li>{@link #MERGED} - 최종 병합 영상 (15~30초)</li>
     * </ul>
     */
    public enum VideoType {
        /** 씬 단위 영상 (3~5초) */
        SCENE,
        /** 최종 병합 영상 (15~30초) */
        MERGED
    }

    /**
     * 영상 생성 상태
     *
     * <ul>
     *   <li>{@link #PENDING}    - 생성 요청 대기 중</li>
     *   <li>{@link #PROCESSING} - AI 생성 진행 중</li>
     *   <li>{@link #COMPLETED}  - 생성 완료</li>
     *   <li>{@link #FAILED}     - 생성 실패</li>
     * </ul>
     */
    public enum GenerationStatus {
        /** 생성 요청 대기 중 */
        PENDING,
        /** AI 생성 진행 중 */
        PROCESSING,
        /** 생성 완료 */
        COMPLETED,
        /** 생성 실패 */
        FAILED
    }
}
