package com.estaid.plot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * 전체 씬 배치 저장 요청 DTO
 *
 * <p>씬 만들기 페이지에서 사용자가 수정한 전체 씬 목록을 한 번에 저장한다.
 * "다음" 버튼 클릭 시 호출되며, 기존 scenesJson을 전체 교체한다.</p>
 *
 * <p>엔드포인트: {@code PUT /api/projects/{projectId}/plots/scenes}</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SceneBatchSaveRequest {

    /**
     * 저장할 씬 목록 (필수)
     * sceneNumber 기준으로 기존 씬을 교체한다.
     * 2.1 AI 스토리보드 생성 응답(SceneDto)과 동일한 구조다.
     */
    @NotNull(message = "씬 목록은 필수입니다.")
    @Valid
    private List<SceneDto> scenes;
}
