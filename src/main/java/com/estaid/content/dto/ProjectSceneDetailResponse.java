package com.estaid.content.dto;

import java.util.List;

public record ProjectSceneDetailResponse(
        String plotId,
        String plotTitle,
        Integer sceneNumber,
        List<SceneImageItemResponse> images,
        SceneVideoResponse video
) {
}
