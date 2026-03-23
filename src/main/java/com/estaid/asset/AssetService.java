package com.estaid.asset;

import com.estaid.asset.dto.AssetCreateResponse;
import com.estaid.asset.dto.AssetResponse;
import com.estaid.asset.dto.AssetSaveRequest;
import com.estaid.asset.dto.GenerateRequest;
import com.estaid.asset.dto.GenerateResponse;
import com.estaid.background.BackgroundService;
import com.estaid.character.Character;
import com.estaid.character.CharacterService;
import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.FalAiService;
import com.estaid.common.service.StorageService;
import com.estaid.content.entity.BackgroundEntity;
import com.estaid.project.Project;
import com.estaid.project.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Asset 비즈니스 로직 서비스
 *
 * <p>캐릭터·배경 이미지 임시 생성 및 "프로젝트에 사용하기" 저장을 담당한다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #generateImage}    - FAL.ai로 이미지 임시 생성 (DB 저장 X)</li>
 *   <li>{@link #saveAsset}        - 생성된 이미지를 프로젝트에 확정 저장 (기존 API)</li>
 *   <li>{@link #findAllByProject} - 프로젝트의 Asset 목록 조회</li>
 *   <li>{@link #createAsset}      - 1단계 통합 생성 (Multipart 업로드 → AI 생성 → 저장)</li>
 *   <li>{@link #deleteAsset}      - 자산 개별 삭제 (characters / backgrounds)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetService {

    private final AssetRepository assetRepository;
    private final ProjectRepository projectRepository;
    private final FalAiService falAiService;

    /** Supabase Storage 파일 업로드 서비스 */
    private final StorageService storageService;

    /** 캐릭터 엔티티 생성·삭제 서비스 */
    private final CharacterService characterService;

    /** 배경 엔티티 생성·삭제 서비스 */
    private final BackgroundService backgroundService;

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
    // 신규 API: 통합 생성 / 개별 삭제
    // ─────────────────────────────────────────

    /**
     * 자산 1단계 통합 생성 (Multipart)
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. referenceImage → StorageService.uploadImage() → Supabase URL
     *   2. name + style로 FAL.ai 프롬프트 자동 생성
     *   3. FalAiService.generateImageSync(referenceImageUrl, prompt) → AI 이미지 URL
     *   4. assetType 분기:
     *      - "characters"  → CharacterService.createRaw() → CharacterEntity 저장
     *      - "backgrounds" → BackgroundService.create()   → BackgroundEntity 저장
     *   5. Asset 엔티티에도 AI 이미지 저장 (CHARACTER / BACKGROUND 타입)
     *   6. AssetCreateResponse 반환
     * </pre>
     *
     * @param projectId      소속 프로젝트 UUID (경로 변수)
     * @param assetType      자산 종류 ("characters" 또는 "backgrounds")
     * @param name           자산 이름 (필수)
     * @param referenceImage 참조 이미지 파일 (Multipart, 필수)
     * @param style          화풍 (예: REALISTIC, ANIME, 3D, PAINT, SKETCH, 필수)
     * @param ratio          이미지 비율 (예: 16:9, 9:16, 1:1, 선택)
     * @param quality        이미지 품질 (예: Standard, High, 선택)
     * @return 생성된 자산 정보 (id, name, referenceImageUrl, imageUrl, status)
     * @throws BusinessException 프로젝트 미존재(404), 잘못된 assetType(400), 생성 실패(502)
     */
    @Transactional
    public AssetCreateResponse createAsset(String projectId, String assetType,
                                           String name, MultipartFile referenceImage,
                                           String style, String ratio, String quality) {
        // 프로젝트 존재 여부 검증
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId, HttpStatus.NOT_FOUND));

        // assetType 유효성 검증 (characters 또는 backgrounds만 허용)
        if (!"characters".equals(assetType) && !"backgrounds".equals(assetType)) {
            throw new BusinessException(
                    "지원하지 않는 자산 유형입니다. assetType=" + assetType
                    + " (허용값: characters, backgrounds)", HttpStatus.BAD_REQUEST);
        }

        // ── Step 1: 참조 이미지 → Supabase Storage 업로드 ──────────────────
        log.info("참조 이미지 업로드 시작: projectId={}, assetType={}, name={}", projectId, assetType, name);
        String referenceImageUrl = storageService.uploadImage(referenceImage);
        log.info("참조 이미지 업로드 완료: referenceImageUrl={}", referenceImageUrl);

        // ── Step 2: FAL.ai 프롬프트 생성 ────────────────────────────────────
        String prompt = buildAssetPrompt(name, assetType, style, ratio, quality);

        // ── Step 3: FAL.ai로 AI 이미지 생성 (동기) ──────────────────────────
        log.info("AI 이미지 생성 시작: prompt={}", prompt);
        String aiImageUrl;
        try {
            aiImageUrl = falAiService.generateImageSync(referenceImageUrl, prompt);
        } catch (Exception e) {
            log.error("AI 이미지 생성 실패: {}", e.getMessage());
            throw new BusinessException("이미지 생성에 실패했습니다: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
        log.info("AI 이미지 생성 완료: imageUrl={}", aiImageUrl);

        // ── Step 4: 자산 엔티티 저장 (Character / Background) ───────────────
        String savedEntityId;
        Asset.AssetType assetTypeEnum;

        if ("characters".equals(assetType)) {
            // 캐릭터 엔티티 생성 및 저장
            Character character = characterService.createRaw(projectId, name, referenceImageUrl, style);
            savedEntityId = character.getCharacterId();
            assetTypeEnum = Asset.AssetType.CHARACTER;
        } else {
            // 배경 엔티티 생성 및 저장 (description은 null — 통합 생성에서는 별도 입력 없음)
            BackgroundEntity background = backgroundService.create(
                    projectId, name, null, referenceImageUrl, style);
            savedEntityId = background.getBackgroundId();
            assetTypeEnum = Asset.AssetType.BACKGROUND;
        }

        // ── Step 5: Asset 엔티티에 AI 이미지 저장 ───────────────────────────
        Asset asset = Asset.builder()
                .project(project)
                .type(assetTypeEnum)
                .imageUrl(aiImageUrl)
                .prompt(prompt)
                .style(style)
                .build();
        assetRepository.save(asset);
        log.info("Asset 저장 완료: assetId={}, type={}, projectId={}",
                asset.getAssetId(), assetTypeEnum, projectId);

        // ── Step 6: 응답 반환 ────────────────────────────────────────────────
        return AssetCreateResponse.builder()
                .id(savedEntityId)
                .name(name)
                .referenceImageUrl(referenceImageUrl)
                .imageUrl(aiImageUrl)
                .status("COMPLETED")
                .build();
    }

    /**
     * 자산 개별 삭제
     *
     * <p>assetType에 따라 캐릭터 또는 배경 엔티티를 삭제한다.
     * 연관 Asset 레코드는 별도로 정리하지 않으며, DB ON DELETE 정책에 따른다.</p>
     *
     * @param projectId 소속 프로젝트 UUID (경로 변수, 현재는 존재 확인용으로만 사용)
     * @param assetType 자산 종류 ("characters" 또는 "backgrounds")
     * @param assetId   삭제할 자산 UUID (characterId 또는 backgroundId)
     * @throws BusinessException 프로젝트 미존재(404), 잘못된 assetType(400), 자산 미존재(404)
     */
    @Transactional
    public void deleteAsset(String projectId, String assetType, String assetId) {
        // 프로젝트 존재 여부 검증
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException(
                    "프로젝트를 찾을 수 없습니다. id=" + projectId, HttpStatus.NOT_FOUND);
        }

        // assetType 유효성 검증
        if (!"characters".equals(assetType) && !"backgrounds".equals(assetType)) {
            throw new BusinessException(
                    "지원하지 않는 자산 유형입니다. assetType=" + assetType
                    + " (허용값: characters, backgrounds)", HttpStatus.BAD_REQUEST);
        }

        // 자산 유형별 삭제 처리
        if ("characters".equals(assetType)) {
            characterService.delete(assetId);
            log.info("캐릭터 삭제 완료: projectId={}, characterId={}", projectId, assetId);
        } else {
            backgroundService.delete(assetId);
            log.info("배경 삭제 완료: projectId={}, backgroundId={}", projectId, assetId);
        }
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

    /**
     * 통합 자산 생성용 FAL.ai 프롬프트를 구성한다.
     *
     * <p>자산 유형(characters / backgrounds)에 따라 프롬프트 패턴을 다르게 적용한다.
     * ratio, quality 옵션이 있으면 프롬프트 끝에 추가한다.</p>
     *
     * @param name      자산 이름
     * @param assetType 자산 유형 ("characters" 또는 "backgrounds")
     * @param style     화풍 (예: REALISTIC, ANIME)
     * @param ratio     이미지 비율 (null 허용, 예: 16:9)
     * @param quality   이미지 품질 (null 허용, 예: Standard, High)
     * @return 완성된 FAL.ai 이미지 생성 프롬프트
     */
    private String buildAssetPrompt(String name, String assetType,
                                    String style, String ratio, String quality) {
        StringBuilder sb = new StringBuilder();

        if ("characters".equals(assetType)) {
            // 캐릭터: 동일 외형·표정 유지를 위한 프롬프트 패턴
            sb.append("character portrait of ").append(name)
              .append(", full body, consistent appearance, ")
              .append("keep the same character face and style as reference image");
        } else {
            // 배경: 배경 장면 생성 프롬프트 패턴
            sb.append("background scene of ").append(name)
              .append(", detailed environment, cinematic composition");
        }

        // 화풍 추가
        if (style != null && !style.isBlank()) {
            sb.append(", ").append(style.toLowerCase()).append(" style");
        }

        // 비율 추가
        if (ratio != null && !ratio.isBlank()) {
            sb.append(", aspect ratio ").append(ratio);
        }

        // 품질 추가
        if ("High".equalsIgnoreCase(quality)) {
            sb.append(", ultra high quality, 8k, highly detailed");
        } else {
            sb.append(", high quality");
        }

        return sb.toString();
    }
}
