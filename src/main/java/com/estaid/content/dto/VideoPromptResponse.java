package com.estaid.content.dto;

/** 씬 영상 프롬프트 응답. */
public record VideoPromptResponse(
        String plotId,
        Integer sceneNumber,
        String prompt
) {
}
