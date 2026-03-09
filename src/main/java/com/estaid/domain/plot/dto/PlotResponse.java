package com.estaid.domain.plot.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 플롯 응답 DTO
 */
@Getter
@Builder
public class PlotResponse {

    private String plotId;
    private String title;
    private String idea;
    private String artStyle;
    private List<SceneDto> scenes;
    private LocalDateTime createdAt;
}
