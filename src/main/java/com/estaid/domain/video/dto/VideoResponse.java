package com.estaid.domain.video.dto;

import com.estaid.domain.video.entity.Video;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 영상 응답 DTO
 */
@Getter
@Builder
public class VideoResponse {

    private String videoId;
    private String plotId;
    private Integer sceneNumber;
    private String videoUrl;
    private Integer duration;
    private Video.VideoType videoType;
    private Video.GenerationStatus status;
    private LocalDateTime createdAt;

    /** 엔티티 → DTO 변환 */
    public static VideoResponse from(Video video) {
        return VideoResponse.builder()
                .videoId(video.getVideoId())
                .plotId(video.getPlotId())
                .sceneNumber(video.getSceneNumber())
                .videoUrl(video.getVideoUrl())
                .duration(video.getDuration())
                .videoType(video.getVideoType())
                .status(video.getStatus())
                .createdAt(video.getCreatedAt())
                .build();
    }
}
