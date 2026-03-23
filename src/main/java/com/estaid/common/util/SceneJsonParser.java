package com.estaid.common.util;

import com.estaid.plot.dto.SceneDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * scenesJson(JSON 문자열) → List&lt;SceneDto&gt; 변환 유틸리티
 *
 * <p>4개 서비스(ContentQueryService, PlotService, ImageService, VideoService)에서
 * 중복되던 역직렬화 로직을 통합한다.</p>
 */
@Slf4j
public final class SceneJsonParser {

    /** Jackson TypeReference (인스턴스 재사용) */
    private static final TypeReference<List<SceneDto>> TYPE_REF = new TypeReference<>() {};

    private SceneJsonParser() {
        // 유틸리티 클래스 — 인스턴스 생성 방지
    }

    /**
     * scenesJson을 SceneDto 목록으로 역직렬화한다.
     *
     * <p>null이거나 빈 문자열이면 빈 리스트를 반환한다.
     * 역직렬화 실패 시 경고 로그를 남기고 빈 리스트를 반환한다.</p>
     *
     * @param scenesJson JSON 배열 문자열 (null 허용)
     * @param mapper     Jackson ObjectMapper
     * @return 씬 DTO 목록 (빈 리스트 가능, null 불가)
     */
    public static List<SceneDto> parse(String scenesJson, ObjectMapper mapper) {
        if (scenesJson == null || scenesJson.isBlank()) {
            return List.of();
        }
        try {
            return mapper.readValue(scenesJson, TYPE_REF);
        } catch (Exception e) {
            log.warn("scenesJson 파싱 실패 (빈 리스트 반환): {}", e.getMessage());
            return List.of();
        }
    }
}
