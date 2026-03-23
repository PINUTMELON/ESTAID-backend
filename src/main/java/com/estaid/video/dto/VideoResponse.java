package com.estaid.video.dto;

import com.estaid.video.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 영상 응답 DTO
 *
 * <p>영상 생성 요청 후 즉시 반환되는 응답 및 단건 조회 응답에 사용된다.
 * 생성 직후에는 {@code status=PENDING}이며, 비동기 FAL.ai 처리 완료 후
 * {@code status=COMPLETED}로 변경된다.
 * 프론트엔드에서 {@code GET /api/videos/{videoId}}를 폴링하여 완료 여부를 확인한다.</p>
 */
@Getter
@Builder
public class VideoResponse {

    /** 영상 고유 식별자 (UUID) */
    private String videoId;

    /** 소속 플롯 UUID */
    private String plotId;

    /** 씬 순번 (SCENE 타입일 때 사용, MERGED 타입은 null) */
    private Integer sceneNumber;

    /** 영상 종류 (SCENE: 씬 단위 / MERGED: 최종 병합) */
    private Video.VideoType videoType;

    /**
     * 영상 생성에 사용된 프롬프트
     * Claude가 자동 생성하거나 사용자가 수정한 값이다.
     */
    private String videoPrompt;

    /** 씬 첫 프레임 이미지 UUID (영상 생성에 사용된 이미지) */
    private String firstImageId;

    /** 씬 마지막 프레임 이미지 UUID (영상 생성에 사용된 이미지) */
    private String lastImageId;

    /**
     * 생성된 영상 URL
     * 생성 중에는 null이며, {@code status=COMPLETED}일 때 값이 채워진다.
     */
    private String videoUrl;

    /**
     * 영상 썸네일 이미지 URL
     * Video 엔티티에는 별도 thumbnail 컬럼이 없으므로,
     * 해당 씬의 첫 프레임 이미지 URL(SceneDto.firstFrameImageUrl)로 대체한다.
     * null일 수 있음.
     */
    private String thumbnail;

    /** 영상 길이 (초 단위, null 가능) */
    private Integer duration;

    /**
     * 영상 생성 상태
     * PENDING → PROCESSING → COMPLETED / FAILED
     */
    private Video.GenerationStatus status;

    /** 레코드 생성 시각 */
    private OffsetDateTime createdAt;

    /**
     * {@link Video} 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드 (thumbnail 없음)
     *
     * @param video Video 엔티티
     * @return VideoResponse DTO (thumbnail=null)
     */
    public static VideoResponse from(Video video) {
        return from(video, null);
    }

    /**
     * {@link Video} 엔티티 + thumbnail URL로 응답 DTO를 생성하는 팩토리 메서드
     *
     * <p>썸네일은 Video 엔티티에 없으므로, 씬의 첫 프레임 이미지 URL을 전달받아 채운다.</p>
     *
     * @param video     Video 엔티티
     * @param thumbnail 썸네일 이미지 URL (null 허용)
     * @return VideoResponse DTO
     */
    public static VideoResponse from(Video video, String thumbnail) {
        return VideoResponse.builder()
                .videoId(video.getVideoId())
                .plotId(video.getPlot() != null ? video.getPlot().getPlotId() : null)
                .sceneNumber(video.getSceneNumber())
                .videoType(video.getVideoType())
                .videoPrompt(video.getVideoPrompt())
                .firstImageId(video.getFirstImage() != null ? video.getFirstImage().getImageId() : null)
                .lastImageId(video.getLastImage() != null ? video.getLastImage().getImageId() : null)
                .videoUrl(video.getVideoUrl())
                .duration(video.getDuration())
                .status(video.getStatus())
                .thumbnail(thumbnail)
                .createdAt(video.getCreatedAt())
                .build();
    }
}
