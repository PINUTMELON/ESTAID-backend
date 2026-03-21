package com.estaid.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProjectRequest {

    @NotBlank(message = "프로젝트 제목은 필수입니다.")
    @Size(max = 200, message = "프로젝트 제목은 200자 이하로 입력해주세요.")
    private String title;

    private String backgroundImageUrl;
}
