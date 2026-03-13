package com.estaid.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 생성/수정 요청 DTO
 *
 * <p>클라이언트가 프로젝트를 생성하거나 수정할 때 전달하는 데이터.</p>
 *
 * <ul>
 *   <li>생성: POST /api/projects</li>
 *   <li>수정: PUT /api/projects/{projectId}</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
public class ProjectRequest {

    /**
     * 프로젝트 제목 (필수, 최대 200자)
     * 예: "괴물과 싸우는 영상", "봄날 벚꽃 로맨스"
     */
    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    @Size(max = 200, message = "프로젝트 제목은 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 배경 이미지 URL (선택)
     * 사용자가 업로드한 배경 이미지의 URL.
     */
    private String backgroundImageUrl;

    /**
     * AI 영상 생성 기본 설정 (선택, JSON 문자열)
     * 예: {"resolution":"720p","aspectRatio":"16:9","fps":24,"duration":5}
     */
    private String settingsJson;
}
