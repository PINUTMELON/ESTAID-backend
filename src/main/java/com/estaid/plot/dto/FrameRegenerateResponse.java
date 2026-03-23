package com.estaid.plot.dto;

import lombok.*;

/**
 * 프레임 이미지 재생성 응답 DTO
 *
 * <p>FAL.ai가 생성한 프레임 이미지 URL을 반환한다.</p>
 */
@Getter
@AllArgsConstructor
public class FrameRegenerateResponse {

    /**
     * 재생성된 프레임 이미지 URL
     * FAL.ai FLUX Kontext가 생성한 이미지의 공개 URL이다.
     */
    private String imageUrl;
}
