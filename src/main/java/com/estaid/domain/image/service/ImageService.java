package com.estaid.domain.image.service;

import com.estaid.domain.image.dto.ImageGenerateRequest;
import com.estaid.domain.image.dto.ImageResponse;

import java.util.List;

/**
 * 이미지 서비스 인터페이스
 */
public interface ImageService {

    /** 이미지 생성 요청 (비동기 - 즉시 PENDING 상태 반환) */
    ImageResponse generateImage(ImageGenerateRequest request);

    /** 이미지 단건 조회 */
    ImageResponse getImage(String imageId);

    /** 특정 플롯의 전체 이미지 조회 */
    List<ImageResponse> getImagesByPlot(String plotId);
}
