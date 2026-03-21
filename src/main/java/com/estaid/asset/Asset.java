package com.estaid.asset;

import com.estaid.project.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Asset 엔티티
 *
 * <p>캐릭터/배경 생성 페이지에서 "프로젝트에 사용하기"를 클릭할 때 저장되는 이미지 자산.
 * 임시 생성(API 호출 결과)을 확정하여 프로젝트에 연결하는 역할을 한다.</p>
 *
 * <p>DB 테이블: {@code assets}</p>
 */
@Entity
@Table(name = "assets")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Asset {

    /**
     * Asset 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "asset_id", length = 36)
    private String assetId;

    /**
     * 소속 프로젝트
     * 프로젝트 삭제 시 Asset도 함께 삭제된다 (CASCADE).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * Asset 종류
     * CHARACTER: 캐릭터 이미지
     * BACKGROUND: 배경 이미지
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 20, nullable = false)
    private AssetType type;

    /**
     * 생성된 이미지 URL (FAL.ai가 반환한 URL)
     */
    @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
    private String imageUrl;

    /**
     * 이미지 생성에 사용한 프롬프트
     */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /**
     * 화풍 설정 (anime, realistic, webtoon 등)
     */
    @Column(name = "style", length = 50)
    private String style;

    /**
     * 레코드 생성 시각
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
     * Asset 종류 enum
     */
    public enum AssetType {
        /** 캐릭터 이미지 */
        CHARACTER,
        /** 배경 이미지 */
        BACKGROUND
    }
}
