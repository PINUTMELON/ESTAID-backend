package com.estaid.content.dto;

/** 씬 이미지 단건 응답. */
public record SceneImageItemResponse(
        String imageId,
        Integer sceneNumber,
        String frameType,
        String prompt,
        String imageUrl,
        String status
) {
}
