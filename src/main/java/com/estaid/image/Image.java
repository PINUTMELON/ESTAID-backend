package com.estaid.image;

import com.estaid.plot.Plot;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 이미지 엔티티
 *
 * <p>AI가 생성한 씬별 이미지 정보를 저장한다.
 * 한 씬당 첫 프레임(FIRST)과 마지막 프레임(LAST) 두 장이 생성된다.
 * 생성된 이미지는 영상 생성(videos) 시 시작/끝 프레임으로 사용된다.</p>
 *
 * <p>DB 테이블: {@code images}</p>
 */
@Entity
@Table(name = "images")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Image {

    /**
     * 이미지 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "image_id", length = 36)
    private String imageId;

    /**
     * 연결된 플롯 (필수 - 플롯 삭제 시 이미지도 함께 삭제됨)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plot_id", nullable = false)
    private Plot plot;

    /**
     * 씬 순번 (1부터 시작)
     */
    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    /**
     * 이미지 프레임 종류 (FIRST: 첫 프레임 / LAST: 마지막 프레임)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frame_type", length = 10, nullable = false)
    private FrameType frameType;

    /**
     * 이미지 생성에 사용한 AI 프롬프트
     */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /**
     * 생성된 이미지의 외부 URL (AI 서비스 또는 스토리지 URL)
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * 이미지 생성 상태 (기본값: PENDING)
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
     * 이미지 프레임 종류
     *
     * <ul>
     *   <li>{@link #FIRST} - 씬의 첫 번째 프레임 이미지</li>
     *   <li>{@link #LAST}  - 씬의 마지막 프레임 이미지</li>
     * </ul>
     */
    public enum FrameType {
        /** 씬의 첫 번째 프레임 이미지 */
        FIRST,
        /** 씬의 마지막 프레임 이미지 */
        LAST
    }

    /**
     * 이미지 생성 상태
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
