package com.estaid.domain.plot.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 씬(장면) DTO
 * - AI가 생성한 각 장면의 정보를 담는다.
 * - PlotResponse 안에 List<SceneDto>로 포함된다.
 */
@Getter
@Builder
public class SceneDto {

    /** 씬 순번 (1부터 시작) */
    private int sceneNumber;

    /** 씬 제목 (예: 괴물의 등장) */
    private String title;

    /** 등장인물 (예: 주인공, 보조캐릭터) */
    private String characters;

    /** 카메라 구도 (예: 와이드 샷, 클로즈업) */
    private String composition;

    /** 배경 (예: 폐허가 된 고대 도시) */
    private String background;

    /** 조명 (예: 어두운 하늘, 번개가 치는 강한 대비) */
    private String lighting;

    /** 분위기 (예: 긴장감, 웅장함) */
    private String atmosphere;

    /** 해당 씬의 주요 스토리 */
    private String mainStory;

    /** 카메라 움직임 (예: 천천히 줌인, 팬 샷) */
    private String cameraMovement;

    /** 첫 프레임 이미지 생성 프롬프트 (영어) */
    private String firstFramePrompt;

    /** 마지막 프레임 이미지 생성 프롬프트 (영어) */
    private String lastFramePrompt;

    /** 영상 생성 프롬프트 (첫 → 마지막 프레임 움직임 묘사, 영어) */
    private String videoPrompt;
}
