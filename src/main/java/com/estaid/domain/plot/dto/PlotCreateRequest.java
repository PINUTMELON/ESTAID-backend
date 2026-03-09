package com.estaid.domain.plot.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * 플롯 생성 요청 DTO
 */
@Getter
public class PlotCreateRequest {

    /** 사용자가 입력한 아이디어 (예: "주인공이 괴물과 싸우는 장면") */
    @NotBlank(message = "아이디어를 입력해주세요.")
    private String idea;

    /** 생성할 씬 수 (5~10개 권장) */
    @Min(value = 1, message = "씬은 최소 1개 이상이어야 합니다.")
    @Max(value = 10, message = "씬은 최대 10개까지 생성 가능합니다.")
    private int sceneCount = 5;

    /** 화풍 (예: anime, realistic, webtoon) */
    private String artStyle;

    /** 참조 캐릭터 ID (선택) - 캐릭터 외형 일관성 유지에 사용 */
    private String characterId;
}
