package com.estaid.domain.image.service;

import com.estaid.common.exception.BusinessException;
import com.estaid.domain.image.dto.ImageGenerateRequest;
import com.estaid.domain.image.dto.ImageResponse;
import com.estaid.domain.image.entity.Image;
import com.estaid.domain.image.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 이미지 서비스 구현체
 *
 * [개발 가이드]
 * 이미지 생성 AI API 연동 방법:
 * - 추천: fal.ai (FLUX 모델), Stability AI, DALL-E 3
 * - 캐릭터 일관성 유지: referenceImageUrl을 img2img 또는 ControlNet에 전달
 * - 비동기 처리: 생성 요청 시 PENDING 반환 → 완료 후 imageUrl 업데이트
 *
 * TODO:
 *  1. 이미지 생성 AI API 선택 및 WebClient 설정 추가 (ClaudeConfig 참고)
 *  2. generateImage()에서 외부 AI API 호출 구현
 *  3. 비동기 처리가 필요하면 @Async + 상태 폴링 방식으로 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final ImageRepository imageRepository;

    /**
     * 이미지 생성 요청
     * - DB에 PENDING 상태로 먼저 저장한 뒤 AI API를 호출한다.
     * - 동기 처리: API 응답 받은 후 URL 업데이트
     * - 비동기 처리가 필요하면 별도 스레드나 메시지 큐 사용
     *
     * TODO: 실제 이미지 생성 AI API 호출 구현 필요
     */
    @Transactional
    @Override
    public ImageResponse generateImage(ImageGenerateRequest request) {
        log.info("이미지 생성 요청 - plotId: {}, sceneNumber: {}, frameType: {}",
                request.getPlotId(), request.getSceneNumber(), request.getFrameType());

        // 1. PENDING 상태로 먼저 저장
        Image image = Image.builder()
                .plotId(request.getPlotId())
                .sceneNumber(request.getSceneNumber())
                .frameType(request.getFrameType())
                .prompt(request.getPrompt())
                .status(Image.GenerationStatus.PENDING)
                .build();

        Image saved = imageRepository.save(image);

        // TODO: 2. 이미지 생성 AI API 호출
        // TODO: 3. 결과 URL로 상태 업데이트 (COMPLETED or FAILED)

        return ImageResponse.from(saved);
    }

    /** 이미지 단건 조회 */
    @Transactional(readOnly = true)
    @Override
    public ImageResponse getImage(String imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException("이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return ImageResponse.from(image);
    }

    /** 특정 플롯의 전체 이미지 조회 */
    @Transactional(readOnly = true)
    @Override
    public List<ImageResponse> getImagesByPlot(String plotId) {
        return imageRepository.findByPlotIdOrderBySceneNumberAsc(plotId)
                .stream()
                .map(ImageResponse::from)
                .collect(Collectors.toList());
    }
}
