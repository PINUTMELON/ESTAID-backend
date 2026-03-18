package com.estaid.image.dto;

import com.estaid.image.Image;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

/**
 * 이미지 응답 DTO
 *
 * <p>이미지 생성 요청 후 즉시 반환되는 응답 및 단건 조회 응답에 사용된다.
 * 생성 직후에는 {@code status=PENDING}이며, 비동기 처리 완료 후 {@code status=COMPLETED}로 변경된다.
 * 프론트엔드에서 {@code GET /api/images/{imageId}}를 폴링하여 완료 여부를 확인한다.</p>
 */
@Getter
@Builder
public class ImageResponse {

    /** 이미지 고유 식별자 (UUID) */
    private String imageId;

    /** 소속 플롯 UUID */
    private String plotId;

    /** 씬 순번 (1부터 시작) */
    private Integer sceneNumber;

    /** 프레임 종류 (FIRST: 첫 프레임 / LAST: 마지막 프레임) */
    private Image.FrameType frameType;

    /** 이미지 생성에 사용된 프롬프트 */
    private String prompt;

    /**
     * 생성된 이미지 URL
     * 생성 중에는 null이며, {@code status=COMPLETED}일 때 값이 채워진다.
     */
    private String imageUrl;

    /**
     * 이미지 생성 상태
     * PENDING → PROCESSING → COMPLETED / FAILED
     */
    private Image.GenerationStatus status;

    /** 레코드 생성 시각 */
    private OffsetDateTime createdAt;

    /**
     * {@link Image} 엔티티로부터 응답 DTO를 생성하는 팩토리 메서드
     *
     * @param image Image 엔티티
     * @return ImageResponse DTO
     */
    public static ImageResponse from(Image image) {
        return ImageResponse.builder()
                .imageId(image.getImageId())
                .plotId(image.getPlot() != null ? image.getPlot().getPlotId() : null)
                .sceneNumber(image.getSceneNumber())
                .frameType(image.getFrameType())
                .prompt(image.getPrompt())
                .imageUrl(image.getImageUrl())
                .status(image.getStatus())
                .createdAt(image.getCreatedAt())
                .build();
    }
}
