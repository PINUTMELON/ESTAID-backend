package com.estaid.content.dto;

import java.util.List;

/** 씬 이미지 목록 응답. */
public record SceneImagesResponse(
        String plotId,
        Integer sceneNumber,
        List<SceneImageItemResponse> images
) {
}
