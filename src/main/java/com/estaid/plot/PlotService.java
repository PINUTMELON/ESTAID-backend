package com.estaid.plot;

import com.estaid.character.Character;
import com.estaid.character.CharacterRepository;
import com.estaid.common.exception.BusinessException;
import com.estaid.common.service.ClaudeService;
import com.estaid.plot.dto.PlotCreateRequest;
import com.estaid.plot.dto.PlotResponse;
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
    private final ObjectMapper objectMapper;

    /**
     * 플롯 생성 (Claude API 씬 자동 생성 포함)
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

        // 3. Claude API 호출 → 씬 목록 생성
        int sceneCount = request.getSceneCount() != null ? request.getSceneCount() : 5;
        List<SceneDto> scenes = claudeService.generateScenes(
                request.getTitle(),
                request.getIdea(),
                sceneCount,
                request.getArtStyle(),
                character
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

        // 4. 씬 내용 수정
        target.setCharacters(request.getCharacters());
        target.setComposition(request.getComposition());
        target.setBackground(request.getBackground());
        target.setLighting(request.getLighting());
        target.setMainStory(request.getMainStory());
        target.setFirstFramePrompt(request.getFirstFramePrompt());
        target.setLastFramePrompt(request.getLastFramePrompt());

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
