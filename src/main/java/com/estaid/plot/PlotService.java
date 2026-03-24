package com.estaid.plot;

import com.estaid.character.Character;
import com.estaid.character.CharacterRepository;
import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.ClaudeService;
import com.estaid.common.service.FalAiService;
import com.estaid.plot.dto.FrameRegenerateRequest;
import com.estaid.plot.dto.FrameRegenerateResponse;
import com.estaid.plot.dto.PlotCreateRequest;
import com.estaid.plot.dto.PlotGenerateRequest;
import com.estaid.plot.dto.PlotResponse;
import com.estaid.plot.dto.SceneBatchSaveRequest;
import com.estaid.plot.dto.SceneDto;
import com.estaid.plot.dto.SceneUpdateRequest;
import com.estaid.project.Project;
import com.estaid.project.ProjectRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 플롯 비즈니스 로직 서비스
 *
 * <p>플롯 생성 시 Claude API를 호출하여 씬 목록을 자동 생성한다.
 * 생성된 씬은 JSON 배열로 직렬화하여 {@code plots.scenes_json} 컬럼에 저장한다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #create}      - 플롯 생성 + Claude API 씬 자동 생성</li>
 *   <li>{@link #updateScene} - 특정 씬 내용 사용자 수정</li>
 *   <li>{@link #findAllByProject} - 프로젝트 내 플롯 목록 조회</li>
 *   <li>{@link #findById}    - 플롯 단건 조회</li>
 *   <li>{@link #delete}      - 플롯 삭제</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotService {

    private final PlotRepository plotRepository;
    private final ProjectRepository projectRepository;
    private final CharacterRepository characterRepository;
    private final ClaudeService claudeService;
    private final FalAiService falAiService;
    private final ObjectMapper objectMapper;

    /**
     * 플롯 생성 (기존 API, 하위 호환 유지)
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. projectId로 프로젝트 조회 (없으면 404)
     *   2. characterId로 캐릭터 조회 (없으면 404, null이면 스킵)
     *   3. Claude API 호출 → sceneCount개의 씬 JSON 생성
     *   4. 씬 목록을 JSON 직렬화하여 Plot 엔티티 저장
     *   5. PlotResponse 반환
     * </pre>
     *
     * @param request 플롯 생성 요청 DTO
     * @return 생성된 플롯 응답 DTO (씬 목록 포함)
     * @throws BusinessException 프로젝트·캐릭터 미존재(404), Claude API 실패(502)
     */
    @Transactional
    public PlotResponse create(PlotCreateRequest request) {
        // 1. 프로젝트 조회
        Project project = getProjectOrThrow(request.getProjectId());

        // 2. 캐릭터 조회 (선택)
        Character character = null;
        if (request.getCharacterId() != null && !request.getCharacterId().isBlank()) {
            character = getCharacterOrThrow(request.getCharacterId());
        }

        // 3. Claude API 호출 → 씬 목록 생성 (캐릭터 정보 포함)
        int sceneCount = request.getSceneCount() != null ? request.getSceneCount() : 5;
        String characterName = character != null ? character.getName() : null;
        String characterDescription = character != null ? character.getDescription() : null;
        List<SceneDto> scenes = claudeService.generateScenes(
                request.getIdea(),
                sceneCount,
                null,
                characterName,
                characterDescription
        );

        // 4. 씬 목록 JSON 직렬화
        String scenesJson = serializeScenes(scenes);

        // 5. Plot 엔티티 저장
        Plot plot = Plot.builder()
                .project(project)
                .title(request.getTitle())
                .idea(request.getIdea())
                .artStyle(request.getArtStyle())
                .character(character)
                .scenesJson(scenesJson)
                .build();

        Plot saved = plotRepository.save(plot);
        log.info("플롯 생성 완료: plotId={}, title={}, sceneCount={}", saved.getPlotId(), saved.getTitle(), scenes.size());
        return PlotResponse.from(saved, scenes);
    }

    /**
     * AI 스토리보드 자동 생성 (신규 API)
     *
     * <p>프론트 요청 기반의 신규 엔드포인트용 메서드.
     * 한 프로젝트당 1번만 생성 가능하며, 이미 플롯이 존재하면 409 Conflict를 반환한다.</p>
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. projectId로 프로젝트 조회 (없으면 404)
     *   2. 해당 프로젝트에 이미 플롯이 있으면 409 Conflict
     *   3. Claude API 호출 → sceneCount개의 씬 JSON 생성
     *      (composition Enum, majorStory, backgroundDetail 포함)
     *   4. 씬 목록 JSON 직렬화 후 Plot 저장
     *   5. PlotResponse 반환 (firstFrameImageUrl/lastFrameImageUrl은 null)
     * </pre>
     *
     * @param request 스토리보드 생성 요청 DTO (projectId, storyDescription, sceneCount, ratio)
     * @return 생성된 플롯 응답 DTO (씬 목록 포함, 프레임 이미지 URL은 null)
     * @throws BusinessException 프로젝트 미존재(404), 이미 플롯 존재(409), Claude API 실패(502)
     */
    @Transactional
    public PlotResponse generate(PlotGenerateRequest request) {
        // 1. 프로젝트 조회
        Project project = getProjectOrThrow(request.getProjectId());

        // 2. 한 프로젝트당 1번 제한: 이미 플롯이 있으면 409 반환
        List<Plot> existing = plotRepository.findByProject_ProjectId(request.getProjectId());
        if (!existing.isEmpty()) {
            throw new BusinessException(
                    "이미 스토리보드가 생성된 프로젝트입니다. 프로젝트당 1번만 생성 가능합니다.",
                    HttpStatus.CONFLICT);
        }

        // 3. 프로젝트의 캐릭터 조회 (있으면 씬 프롬프트에 외형 정보 반영)
        List<Character> characters = characterRepository.findByProject_ProjectId(request.getProjectId());
        Character character = characters.isEmpty() ? null : characters.get(0);
        String characterName = character != null ? character.getName() : null;
        String characterDescription = character != null ? character.getDescription() : null;

        // 4. Claude API 호출 → 씬 목록 생성 (캐릭터 정보 포함)
        int sceneCount = request.getSceneCount() != null ? request.getSceneCount() : 4;
        List<SceneDto> scenes = claudeService.generateScenes(
                request.getStoryDescription(),
                sceneCount,
                request.getRatio(),
                characterName,
                characterDescription
        );

        // 5. 씬 목록 JSON 직렬화
        String scenesJson = serializeScenes(scenes);

        // 6. Plot 엔티티 저장 (title은 storyDescription의 앞 50자로 대체, 캐릭터 연결)
        String title = request.getStoryDescription().length() > 50
                ? request.getStoryDescription().substring(0, 50)
                : request.getStoryDescription();

        Plot plot = Plot.builder()
                .project(project)
                .title(title)
                .idea(request.getStoryDescription())
                .character(character)
                .scenesJson(scenesJson)
                .build();

        Plot saved = plotRepository.save(plot);
        log.info("스토리보드 자동 생성 완료: plotId={}, projectId={}, sceneCount={}",
                saved.getPlotId(), request.getProjectId(), scenes.size());
        return PlotResponse.from(saved, scenes);
    }

    /**
     * 전체 씬 배치 저장 (다음 버튼 클릭 시 호출)
     *
     * <p>사용자가 씬 만들기 페이지에서 수정한 전체 씬 목록을 한 번에 저장한다.
     * 기존 scenesJson을 요청 데이터로 완전히 교체한다.</p>
     *
     * @param projectId 프로젝트 UUID
     * @param request   씬 배치 저장 요청 DTO (씬 목록)
     * @throws BusinessException 프로젝트 미존재(404), 플롯 미존재(404)
     */
    @Transactional
    public void saveScenesBatch(String projectId, SceneBatchSaveRequest request) {
        // 1. 해당 프로젝트의 플롯 조회
        List<Plot> plots = plotRepository.findByProject_ProjectId(projectId);
        if (plots.isEmpty()) {
            throw new BusinessException(
                    "플롯을 찾을 수 없습니다. projectId=" + projectId, HttpStatus.NOT_FOUND);
        }

        // 2. 첫 번째 플롯(프로젝트당 1개)의 scenesJson을 요청 데이터로 교체
        Plot plot = plots.get(0);
        String scenesJson = serializeScenes(request.getScenes());
        plot.setScenesJson(scenesJson);

        log.info("씬 배치 저장 완료: plotId={}, projectId={}, sceneCount={}",
                plot.getPlotId(), projectId, request.getScenes().size());
    }

    /**
     * 프레임 이미지 재생성
     *
     * <p>씬의 첫 프레임 또는 마지막 프레임 이미지를 FAL.ai로 재생성한다.
     * 재생성 후 SceneDto의 firstFrameImageUrl 또는 lastFrameImageUrl을 업데이트한다.</p>
     *
     * @param request 프레임 재생성 요청 DTO (projectId, sceneNumber, firstOrLast, prompt)
     * @return 재생성된 이미지 URL
     * @throws BusinessException 프로젝트·플롯·씬 미존재(404), FAL.ai 실패(502)
     */
    @Transactional
    public FrameRegenerateResponse regenerateFrame(FrameRegenerateRequest request) {
        // 1. 해당 프로젝트의 플롯 조회
        List<Plot> plots = plotRepository.findByProject_ProjectId(request.getProjectId());
        if (plots.isEmpty()) {
            throw new BusinessException(
                    "플롯을 찾을 수 없습니다. projectId=" + request.getProjectId(), HttpStatus.NOT_FOUND);
        }

        Plot plot = plots.get(0);

        // 2. 씬 목록에서 해당 씬 번호 찾기
        List<SceneDto> scenes = deserializeScenes(plot.getScenesJson());
        SceneDto target = scenes.stream()
                .filter(s -> s.getSceneNumber() == request.getSceneNumber())
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "씬 번호를 찾을 수 없습니다. sceneNumber=" + request.getSceneNumber(),
                        HttpStatus.NOT_FOUND));

        // 3. FAL.ai FLUX Pro text-to-image로 프레임 이미지 생성 (프롬프트만 사용)
        //    FLUX Kontext는 image_url 필수이므로, text-to-image 전용 메서드 사용
        String imageUrl = falAiService.generateTextImageSync(request.getPrompt());

        // 4. 해당 프레임 URL 업데이트 후 scenesJson 저장
        boolean isFirst = "FIRST".equalsIgnoreCase(request.getFirstOrLast());
        if (isFirst) {
            target.setFirstFrameImageUrl(imageUrl);
            target.setFirstFramePrompt(request.getPrompt()); // 프롬프트도 업데이트
        } else {
            target.setLastFrameImageUrl(imageUrl);
            target.setLastFramePrompt(request.getPrompt()); // 프롬프트도 업데이트
        }

        plot.setScenesJson(serializeScenes(scenes));

        // 첫 씬 FIRST 프레임 재생성 시 프로젝트 썸네일도 함께 업데이트
        if (request.getSceneNumber() == 1 && isFirst) {
            Project proj = plot.getProject();
            proj.setBackgroundImageUrl(imageUrl);
            projectRepository.save(proj);
            log.info("프로젝트 썸네일 업데이트: projectId={}, imageUrl={}", proj.getProjectId(), imageUrl);
        }

        log.info("프레임 재생성 완료: plotId={}, sceneNumber={}, firstOrLast={}, imageUrl={}",
                plot.getPlotId(), request.getSceneNumber(), request.getFirstOrLast(), imageUrl);

        return new FrameRegenerateResponse(imageUrl);
    }

    /**
     * 특정 씬 내용 수정 (사용자 직접 편집)
     *
     * <p>처리 흐름:</p>
     * <pre>
     *   1. Plot 조회 (없으면 404)
     *   2. scenes_json 역직렬화 → List&lt;SceneDto&gt;
     *   3. sceneNumber에 해당하는 씬 찾기 (없으면 404)
     *   4. 요청 필드로 씬 내용 교체
     *   5. 재직렬화 후 저장
     * </pre>
     *
     * @param plotId      수정할 플롯 UUID
     * @param sceneNumber 수정할 씬 번호 (1부터 시작)
     * @param request     씬 수정 요청 DTO
     * @return 수정된 플롯 전체 응답 DTO
     * @throws BusinessException 플롯 미존재(404), 씬 번호 미존재(404)
     */
    @Transactional
    public PlotResponse updateScene(String plotId, int sceneNumber, SceneUpdateRequest request) {
        // 1. 플롯 조회
        Plot plot = getPlotOrThrow(plotId);

        // 2. 씬 목록 역직렬화
        List<SceneDto> scenes = deserializeScenes(plot.getScenesJson());

        // 3. 해당 씬 번호 찾기
        SceneDto target = scenes.stream()
                .filter(s -> s.getSceneNumber() == sceneNumber)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        "씬 번호를 찾을 수 없습니다. sceneNumber=" + sceneNumber, HttpStatus.NOT_FOUND));

        // 4. 씬 내용 수정 (null인 필드도 그대로 반영되므로 클라이언트가 기존 값 포함하여 전송해야 함)
        target.setCharacters(request.getCharacters());
        target.setComposition(request.getComposition());
        target.setBackground(request.getBackground());
        target.setBackgroundDetail(request.getBackgroundDetail());
        target.setLighting(request.getLighting());
        target.setMajorStory(request.getMajorStory());
        target.setFirstFramePrompt(request.getFirstFramePrompt());
        target.setFirstFrameImageUrl(request.getFirstFrameImageUrl());
        target.setLastFramePrompt(request.getLastFramePrompt());
        target.setLastFrameImageUrl(request.getLastFrameImageUrl());

        // 5. 재직렬화 후 저장
        plot.setScenesJson(serializeScenes(scenes));
        log.info("씬 수정 완료: plotId={}, sceneNumber={}", plotId, sceneNumber);
        return PlotResponse.from(plot, scenes);
    }

    /**
     * 특정 프로젝트의 플롯 목록 조회
     *
     * @param projectId 프로젝트 UUID
     * @return 플롯 응답 DTO 목록 (씬 목록 포함)
     * @throws BusinessException 프로젝트 미존재(404)
     */
    @Transactional(readOnly = true)
    public List<PlotResponse> findAllByProject(String projectId) {
        getProjectOrThrow(projectId); // 프로젝트 존재 검증
        return plotRepository.findByProject_ProjectId(projectId)
                .stream()
                .map(plot -> PlotResponse.from(plot, deserializeScenes(plot.getScenesJson())))
                .toList();
    }

    /**
     * 플롯 단건 조회
     *
     * @param plotId 조회할 플롯 UUID
     * @return 플롯 응답 DTO (씬 목록 포함)
     * @throws BusinessException 플롯 미존재(404)
     */
    @Transactional(readOnly = true)
    public PlotResponse findById(String plotId) {
        Plot plot = getPlotOrThrow(plotId);
        return PlotResponse.from(plot, deserializeScenes(plot.getScenesJson()));
    }

    /**
     * 플롯 삭제
     *
     * <p>DB의 ON DELETE CASCADE로 연결된 이미지·영상도 함께 삭제된다.</p>
     *
     * @param plotId 삭제할 플롯 UUID
     * @throws BusinessException 플롯 미존재(404)
     */
    @Transactional
    public void delete(String plotId) {
        Plot plot = getPlotOrThrow(plotId);
        plotRepository.delete(plot);
        log.info("플롯 삭제 완료: plotId={}", plotId);
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * 씬 목록을 JSON 문자열로 직렬화한다.
     *
     * @param scenes 씬 목록
     * @return JSON 배열 문자열
     * @throws BusinessException 직렬화 실패 시
     */
    private String serializeScenes(List<SceneDto> scenes) {
        try {
            return objectMapper.writeValueAsString(scenes);
        } catch (Exception e) {
            log.error("씬 직렬화 실패: {}", e.getMessage());
            throw new BusinessException("씬 데이터 저장에 실패했습니다.");
        }
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

    /** 플롯 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Plot getPlotOrThrow(String plotId) {
        return plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException(
                        "플롯을 찾을 수 없습니다. id=" + plotId, HttpStatus.NOT_FOUND));
    }

    /** 프로젝트 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Project getProjectOrThrow(String projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException(
                        "프로젝트를 찾을 수 없습니다. id=" + projectId, HttpStatus.NOT_FOUND));
    }

    /** 캐릭터 조회 공통 메서드 - 없으면 404 예외 발생 */
    private Character getCharacterOrThrow(String characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new BusinessException(
                        "캐릭터를 찾을 수 없습니다. id=" + characterId, HttpStatus.NOT_FOUND));
    }
}
