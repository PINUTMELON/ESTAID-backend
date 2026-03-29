package com.estaid.image;

import com.estaid.common.response.ApiResponse;
import com.estaid.image.dto.ImageGenerateRequest;
import com.estaid.image.dto.ImageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 이미지 REST 컨트롤러
 *
 * <p>기본 경로: {@code /api/images}</p>
 *
 * <ul>
 *   <li>POST /api/images/generate            - 씬 이미지 생성 요청 (즉시 PENDING 반환)</li>
 *   <li>GET  /api/images/{imageId}           - 이미지 단건 조회 (상태 + URL 폴링용)</li>
 *   <li>GET  /api/images/plot/{plotId}       - 플롯의 이미지 목록 조회</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    /**
     * 씬 이미지 생성 요청
     *
     * <p>요청 즉시 {@code status=PENDING}인 응답을 반환하고,
     * 실제 FAL.ai 이미지 생성은 비동기로 처리된다.
     * 완료 여부는 {@code GET /api/images/{imageId}}로 폴링하여 확인한다.</p>
     *
     * @param request 이미지 생성 요청 바디 (plotId, sceneNumber, frameType)
     * @return 201 Created + 생성된 이미지 정보 (status=PENDING)
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<ImageResponse>> generate(
            @Valid @RequestBody ImageGenerateRequest request) {
        ImageResponse response = imageService.generate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("이미지 생성을 요청했습니다.", response));
    }

    /**
     * 씬의 첫/마지막 프레임 이미지 동시 생성 (배치)
     *
     * <p>한 번의 요청으로 FIRST + LAST 두 프레임 이미지를 동시에 생성한다.
     * 두 프레임은 @Async로 병렬 처리되어 순차 호출 대비 ~50% 빠르다.</p>
     *
     * @param plotId      플롯 UUID
     * @param sceneNumber 씬 번호 (1 이상)
     * @return 201 Created + FIRST/LAST 두 이미지 정보 (status=PENDING)
     */
    @PostMapping("/generate/batch")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> generateBatch(
            @RequestParam String plotId,
            @RequestParam Integer sceneNumber) {
        List<ImageResponse> responses = imageService.generateBatch(plotId, sceneNumber);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("이미지 배치 생성을 요청했습니다.", responses));
    }

    /**
     * 이미지 단건 조회 (상태 폴링용)
     *
     * <p>프론트엔드에서 주기적으로 호출하여 생성 완료 여부를 확인한다.
     * {@code status=COMPLETED}이면 {@code imageUrl}에 결과가 담겨 있다.</p>
     *
     * @param imageId 조회할 이미지 UUID
     * @return 200 OK + 이미지 정보 (status, imageUrl 포함)
     */
    @GetMapping("/{imageId}")
    public ResponseEntity<ApiResponse<ImageResponse>> findById(@PathVariable String imageId) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.findById(imageId)));
    }

    /**
     * 플롯의 이미지 목록 조회
     *
     * <p>특정 플롯에 속한 모든 씬의 이미지를 씬 순번 오름차순으로 반환한다.</p>
     *
     * @param plotId 조회할 플롯 UUID
     * @return 200 OK + 이미지 목록 (씬 순번 오름차순)
     */
    @GetMapping("/plot/{plotId}")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> findAllByPlot(@PathVariable String plotId) {
        return ResponseEntity.ok(ApiResponse.ok(imageService.findAllByPlot(plotId)));
    }
}
