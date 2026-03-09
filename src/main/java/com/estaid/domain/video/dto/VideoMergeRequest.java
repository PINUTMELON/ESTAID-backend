package com.estaid.domain.video.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

import java.util.List;

/**
 * 영상 병합 요청 DTO
 * - 여러 씬 영상을 순서대로 병합하여 하나의 최종 영상을 만든다.
 */
@Getter
public class VideoMergeRequest {

    /** 병합할 영상 ID 목록 (순서대로 병합됨) */
    @NotEmpty(message = "병합할 영상 ID 목록은 필수입니다.")
    private List<String> videoIds;

    /** 최종 영상 제목 */
    @NotBlank(message = "영상 제목은 필수입니다.")
    private String outputTitle;
}
