package com.estaid.content.dto;

import java.util.List;

/** 씬 이미지 프롬프트 목록 응답. */
public record ImagePromptResponse(
        String plotId,
        Integer sceneNumber,
        List<String> prompts
) {
}
