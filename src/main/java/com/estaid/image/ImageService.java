package com.estaid.image;

import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.FalAiService;
import com.estaid.image.dto.ImageGenerateRequest;
import com.estaid.image.dto.ImageResponse;
import com.estaid.plot.Plot;
import com.estaid.plot.PlotRepository;
import com.estaid.plot.dto.SceneDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이미지 비즈니스 로직 서비스
 *
 * <p>씬별 첫/마지막 프레임 이미지를 FAL.ai FLUX Kontext로 생성한다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #generate}         - 이미지 생성 요청 (즉시 PENDING 반환, 비동기 처리)</li>
 *   <li>{@link #findById}         - 이미지 단건 조회 (상태 + URL 확인)</li>
 *   <li>{@link #findAllByPlot}    - 플롯의 전체 이미지 목록 조회</li>
 * </ul>
 *
 * <p>생성 흐름:</p>
 * <pre>
 *   1. Plot 조회 → 씬 목록 역직렬화 → 해당 sceneNumber 씬 추출
 *   2. frameType에 따라 firstFramePrompt / lastFramePrompt 선택
 *   3. 캐릭터 referenceImageUrl 조회 (없으면 null)
 *   4. Image 엔티티 저장 (status=PENDING)
 *   5. @Async FAL.ai 호출 → 완료 시 imageUrl 업데이트
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageRepository imageRepository;
    private final PlotRepository plotRepository;
    private final FalAiService falAiService;
    private final ObjectMapper objectMapper;

    /**
     * 씬 이미지 생성 요청
     *
     * <p>이미지 엔티티를 즉시 PENDING 상태로 저장하고,
     * FAL.ai API 호출은 비동기(@Async)로 처리한다.</p>
     *
     * @param request 이미지 생성 요청 DTO (plotId, sceneNumber, frameType)
     * @return 생성된 Image 응답 DTO (status=PENDING)
     * @throws BusinessException 플롯·씬 미존재(404) 시
     */
    @Transactional
    public ImageResponse generate(ImageGenerateRequest request) {
        // 1. 플롯 조회
        Plot plot = getPlotOrThrow(request.getPlotId());

        // 2. 씬 목록 역직렬화 → 해당 씬 추출
        List<SceneDto> scenes = deserializeScenes(plot.getScenesJson());
        SceneDto scene = scenes.stream()
                .filter(s -> s.getSceneNumber() == request.getSceneNumber())
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "씬 번호를 찾을 수 없습니다. sceneNumber=" + request.getSceneNumber(),
                        HttpStatus.NOT_FOUND));

        // 3. frameType에 따라 프롬프트 선택 (artStyle 포함)
        String basePrompt = request.getFrameType() == Image.FrameType.FIRST
                ? scene.getFirstFramePrompt()
                : scene.getLastFramePrompt();
        String artStyle = plot.getArtStyle();
        String finalPrompt = buildImagePrompt(basePrompt, artStyle);

        // 4. 캐릭터 레퍼런스 이미지 URL 조회 (없으면 null)
        String referenceImageUrl = null;
        if (plot.getCharacter() != null) {
            referenceImageUrl = plot.getCharacter().getReferenceImageUrl();
        }

        // 5. Image 엔티티 저장 (status=PENDING)
        Image image = Image.builder()
                .plot(plot)
                .sceneNumber(request.getSceneNumber())
                .frameType(request.getFrameType())
                .prompt(finalPrompt)
                .status(Image.GenerationStatus.PENDING)
                .build();
        Image saved = imageRepository.save(image);
        log.info("이미지 엔티티 저장 완료: imageId={}, plotId={}, sceneNumber={}, frameType={}",
                saved.getImageId(), request.getPlotId(), request.getSceneNumber(), request.getFrameType());

        // 6. 비동기 FAL.ai 호출 (@Async - 즉시 반환, 별도 스레드에서 처리)
        falAiService.processImageGeneration(saved.getImageId(), referenceImageUrl, finalPrompt);

        return ImageResponse.from(saved);
    }

    /**
     * 이미지 단건 조회
     *
     * <p>프론트엔드에서 이 엔드포인트를 폴링하여 생성 완료 여부를 확인한다.
     * {@code status=COMPLETED}이면 {@code imageUrl}에 생성된 이미지 URL이 담겨 있다.</p>
     *
     * @param imageId 조회할 Image UUID
     * @return ImageResponse DTO
     * @throws BusinessException 이미지 미존재(404) 시
     */
    @Transactional(readOnly = true)
    public ImageResponse findById(String imageId) {
        Image image = getImageOrThrow(imageId);
        return ImageResponse.from(image);
    }

    /**
     * 플롯의 이미지 목록 조회
     *
     * @param plotId 조회할 플롯 UUID
     * @return 해당 플롯의 이미지 목록 (씬 순번 오름차순)
     * @throws BusinessException 플롯 미존재(404) 시
     */
    @Transactional(readOnly = true)
    public List<ImageResponse> findAllByPlot(String plotId) {
        getPlotOrThrow(plotId); // 플롯 존재 검증
        return imageRepository.findByPlot_PlotIdOrderBySceneNumberAsc(plotId)
                .stream()
                .map(ImageResponse::from)
                .toList();
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * 이미지 생성 프롬프트를 구성한다.
     * 아트스타일이 있으면 "style: {artStyle}, high quality" 형태로 추가한다.
     *
     * @param basePrompt 씬의 firstFramePrompt 또는 lastFramePrompt
     * @param artStyle   화풍 설정 (null 허용)
     * @return 최종 이미지 프롬프트
     */
    private String buildImagePrompt(String basePrompt, String artStyle) {
        if (artStyle != null && !artStyle.isBlank()) {
            return basePrompt + ", style: " + artStyle + ", high quality";
        }
        return basePrompt + ", high quality";
    }

    /**
     * JSON 문자열을 씬 목록으로 역직렬화한다.
     * scenesJson이 null이거나 비어 있으면 빈 리스트를 반환한다.
     *
     * @param scenesJson JSON 배열 문자열
     * @return 씬 목록 (빈 리스트 가능)
     */
    private List<SceneDto> deserializeScenes(String scenesJson) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(scenesJson, new TypeReference<List<SceneDto>>() {});
        } catch (Exception e) {
            log.error("씬 역직렬화 실패: {}", e.getMessage());
            throw new BusinessException("씬 데이터 파싱에 실패했습니다.");
        }
    }

    /** 이미지 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Image getImageOrThrow(String imageId) {
        return imageRepository.findById(imageId)
                .orElseThrow(() -> new BusinessException(
                        "이미지를 찾을 수 없습니다. id=" + imageId, HttpStatus.NOT_FOUND));
    }

    /** 플롯 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Plot getPlotOrThrow(String plotId) {
        return plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException(
                        "플롯을 찾을 수 없습니다. id=" + plotId, HttpStatus.NOT_FOUND));
    }
}
