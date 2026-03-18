package com.estaid.plot.dto;

import com.estaid.plot.Plot;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 플롯 응답 DTO
 *
 * <p>플롯 조회·생성·수정 API의 응답으로 사용된다.
 * {@code scenes} 필드에 씬 목록이 담겨 프론트엔드에서 표(테이블) 형태로 렌더링된다.</p>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlotResponse {

    /** 플롯 고유 식별자 (UUID) */
    private String plotId;

    /** 소속 프로젝트 UUID */
    private String projectId;

    /** 플롯 제목 */
    private String title;

    /** 사용자가 입력한 원본 스토리 아이디어 */
    private String idea;

    /** 화풍 설정 (anime, realistic, webtoon 등) */
    private String artStyle;

    /** 참조 캐릭터 UUID (없으면 null) */
    private String characterId;

    /**
     * Claude AI가 생성한 씬 목록 (표 형태로 프론트에서 렌더링)
     * 각 씬은 sceneNumber, title, characters, composition, background,
     * lighting, mainStory, firstFramePrompt, lastFramePrompt 포함
     */
    private List<SceneDto> scenes;

    /** 플롯 생성 시각 */
    private OffsetDateTime createdAt;

    /**
     * Plot 엔티티 + 씬 목록으로 응답 DTO 생성
     *
     * @param plot   플롯 엔티티
     * @param scenes 역직렬화된 씬 목록
     * @return PlotResponse
     */
    public static PlotResponse from(Plot plot, List<SceneDto> scenes) {
        return PlotResponse.builder()
                .plotId(plot.getPlotId())
                .projectId(plot.getProject() != null ? plot.getProject().getProjectId() : null)
                .title(plot.getTitle())
                .idea(plot.getIdea())
                .artStyle(plot.getArtStyle())
                .characterId(plot.getCharacter() != null ? plot.getCharacter().getCharacterId() : null)
                .scenes(scenes)
                .createdAt(plot.getCreatedAt())
                .build();
    }
}
