package com.estaid.domain.image.dto;

import com.estaid.domain.image.entity.Image;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 이미지 응답 DTO
 */
@Getter
@Builder
public class ImageResponse {

    private String imageId;
    private String plotId;
    private int sceneNumber;
    private Image.FrameType frameType;
    private String prompt;
    private String imageUrl;
    private Image.GenerationStatus status;
    private LocalDateTime createdAt;

    /** 엔티티 → DTO 변환 */
    public static ImageResponse from(Image image) {
        return ImageResponse.builder()
                .imageId(image.getImageId())
                .plotId(image.getPlotId())
                .sceneNumber(image.getSceneNumber())
                .frameType(image.getFrameType())
                .prompt(image.getPrompt())
                .imageUrl(image.getImageUrl())
                .status(image.getStatus())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
