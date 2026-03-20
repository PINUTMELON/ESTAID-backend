package com.estaid.asset;

import com.estaid.asset.dto.AssetResponse;
import com.estaid.asset.dto.AssetSaveRequest;
import com.estaid.asset.dto.GenerateRequest;
import com.estaid.asset.dto.GenerateResponse;
import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.FalAiService;
import com.estaid.project.Project;
import com.estaid.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Asset 비즈니스 로직 서비스
 *
 * <p>캐릭터·배경 이미지 임시 생성 및 "프로젝트에 사용하기" 저장을 담당한다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #generateImage}  - FAL.ai로 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>{@link #saveAsset}      - 생성된 이미지를 프로젝트에 확정 저장</li>
 *   <li>{@link #findAllByProject} - 프로젝트의 Asset 목록 조회</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProjectRepository projectRepository;
    private final FalAiService falAiService;

    /**
     * FAL.ai로 이미지를 임시 생성한다 (DB 저장 없음)
     *
     * <p>캐릭터·배경 생성 페이지에서 미리보기 용도로 사용한다.
     * 마음에 들면 {@link #saveAsset}으로 프로젝트에 저장한다.</p>
     *
     * @param request 생성 요청 (prompt, style, referenceImageUrl)
     * @return 생성된 이미지 URL
     * @throws BusinessException FAL.ai 호출 실패 시
     */
    public GenerateResponse generateImage(GenerateRequest request) {
        // style이 있으면 프롬프트 끝에 붙여준다
        String fullPrompt = buildPrompt(request.getPrompt(), request.getStyle());

        log.info("이미지 임시 생성 요청: prompt={}", fullPrompt);

        try {
            String imageUrl = falAiService.generateImageSync(request.getReferenceImageUrl(), fullPrompt);
            log.info("이미지 임시 생성 완료: imageUrl={}", imageUrl);
            return new GenerateResponse(imageUrl);
        } catch (Exception e) {
            log.error("이미지 임시 생성 실패: {}", e.getMessage());
            throw new BusinessException("이미지 생성에 실패했습니다: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    /**
     * 임시 생성된 이미지를 프로젝트에 확정 저장한다
     *
     * <p>"프로젝트에 사용하기" 버튼 클릭 시 호출된다.
     * Asset 엔티티를 생성하여 DB에 저장하고 응답을 반환한다.</p>
     *
     * @param projectId 저장할 프로젝트 UUID
     * @param request   저장 요청 (type, imageUrl, prompt, style)
     * @return 저장된 Asset 응답 DTO
     * @throws BusinessException 프로젝트 미존재(404) 시
     */
    @Transactional
    public AssetResponse saveAsset(String projectId, AssetSaveRequest request) {
        // 프로젝트 조회
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId, HttpStatus.NOT_FOUND));

        // Asset 엔티티 생성 및 저장
        Asset asset = Asset.builder()
                .project(project)
                .type(request.getType())
                .imageUrl(request.getImageUrl())
                .prompt(request.getPrompt())
                .style(request.getStyle())
                .build();

        Asset saved = assetRepository.save(asset);
        log.info("Asset 저장 완료: assetId={}, projectId={}, type={}", saved.getAssetId(), projectId, saved.getType());

        return AssetResponse.from(saved);
    }

    /**
     * 프로젝트의 전체 Asset 목록을 조회한다
     *
     * @param projectId 프로젝트 UUID
     * @return Asset 목록 (생성 시각 오름차순)
     * @throws BusinessException 프로젝트 미존재(404) 시
     */
    @Transactional(readOnly = true)
    public List<AssetResponse> findAllByProject(String projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException("프로젝트를 찾을 수 없습니다. id=" + projectId, HttpStatus.NOT_FOUND);
        }
        return assetRepository.findByProject_ProjectIdOrderByCreatedAtAsc(projectId)
                .stream()
                .map(AssetResponse::from)
                .toList();
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * 프롬프트에 화풍(style)을 붙여서 최종 프롬프트를 구성한다.
     *
     * @param prompt 기본 프롬프트
     * @param style  화풍 (null 허용)
     * @return 최종 프롬프트
     */
    private String buildPrompt(String prompt, String style) {
        if (style != null && !style.isBlank()) {
            return prompt + ", " + style + " style, high quality";
        }
        return prompt + ", high quality";
    }
}
