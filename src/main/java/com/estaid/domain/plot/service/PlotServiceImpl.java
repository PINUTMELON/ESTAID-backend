package com.estaid.domain.plot.service;

import com.estaid.common.exception.BusinessException;
import com.estaid.domain.plot.dto.PlotCreateRequest;
import com.estaid.domain.plot.dto.PlotResponse;
import com.estaid.domain.plot.dto.SceneDto;
import com.estaid.domain.plot.entity.Plot;
import com.estaid.domain.plot.repository.PlotRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * 플롯 서비스 구현체
 *
 * [개발 가이드]
 * 1. claudeWebClient를 사용하여 Claude API에 프롬프트를 전송한다.
 * 2. Claude 응답(JSON)을 파싱하여 List<SceneDto>로 변환한다.
 * 3. Plot 엔티티로 저장한다.
 *
 * Claude API 요청 형식:
 * POST https://api.anthropic.com/v1/messages
 * {
 *   "model": "claude-opus-4-6",
 *   "max_tokens": 4096,
 *   "messages": [
 *     { "role": "user", "content": "..." }
 *   ]
 * }
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlotServiceImpl implements PlotService {

    @Qualifier("claudeWebClient")
    private final WebClient claudeWebClient;

    private final PlotRepository plotRepository;
    private final ObjectMapper objectMapper;

    @Value("${claude.api.model}")
    private String claudeModel;

    /**
     * 플롯 생성
     * TODO:
     *  1. buildPlotPrompt()로 프롬프트 생성
     *  2. claudeWebClient로 Claude API 호출
     *  3. 응답 JSON 파싱 → List<SceneDto>
     *  4. Plot 엔티티 저장
     *  5. PlotResponse 반환
     */
    @Transactional
    @Override
    public PlotResponse createPlot(PlotCreateRequest request) {
        log.info("플롯 생성 요청 - 아이디어: {}, 씬 수: {}", request.getIdea(), request.getSceneCount());

        // TODO: Claude API 호출 구현
        // String prompt = buildPlotPrompt(request);
        // List<SceneDto> scenes = callClaudeForPlot(prompt);

        throw new BusinessException("플롯 생성 기능은 아직 구현되지 않았습니다.", HttpStatus.NOT_IMPLEMENTED);
    }

    /**
     * 플롯 단건 조회
     */
    @Transactional(readOnly = true)
    @Override
    public PlotResponse getPlot(String plotId) {
        log.info("플롯 조회: {}", plotId);

        Plot plot = plotRepository.findById(plotId)
                .orElseThrow(() -> new BusinessException("플롯을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        // scenesJson → List<SceneDto> 역직렬화
        List<SceneDto> scenes = parseScenesJson(plot.getScenesJson());

        return PlotResponse.builder()
                .plotId(plot.getPlotId())
                .title(plot.getTitle())
                .idea(plot.getIdea())
                .artStyle(plot.getArtStyle())
                .scenes(scenes)
                .createdAt(plot.getCreatedAt())
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helper methods
    // -------------------------------------------------------------------------

    /**
     * Claude API에 전달할 플롯 생성 프롬프트를 구성한다.
     *
     * @param request 플롯 생성 요청 정보
     * @return 완성된 프롬프트 문자열
     *
     * TODO: 프롬프트 내용을 작성할 것.
     *       응답은 반드시 아래 JSON 배열 형식을 따르도록 지시한다:
     *       [
     *         {
     *           "sceneNumber": 1,
     *           "title": "씬 제목",
     *           "characters": "등장인물",
     *           "composition": "카메라 구도",
     *           "background": "배경",
     *           "lighting": "조명",
     *           "atmosphere": "분위기",
     *           "mainStory": "주요 스토리",
     *           "cameraMovement": "카메라 움직임",
     *           "firstFramePrompt": "첫 프레임 프롬프트 (영어)",
     *           "lastFramePrompt": "마지막 프레임 프롬프트 (영어)",
     *           "videoPrompt": "영상 생성 프롬프트 (영어)"
     *         }
     *       ]
     */
    private String buildPlotPrompt(PlotCreateRequest request) {
        // TODO: 구현 필요
        return "";
    }

    /**
     * Claude API를 호출하여 씬 목록을 생성한다.
     *
     * @param prompt 플롯 생성 프롬프트
     * @return 생성된 씬 목록
     *
     * TODO: claudeWebClient.post()로 API 호출 구현
     */
    private List<SceneDto> callClaudeForPlot(String prompt) {
        // TODO: 구현 필요
        return List.of();
    }

    /**
     * JSON 문자열을 List<SceneDto>로 역직렬화한다.
     */
    private List<SceneDto> parseScenesJson(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<SceneDto>>() {});
        } catch (Exception e) {
            log.error("씬 JSON 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }
}
