package com.estaid.content.entity;

import com.estaid.common.util.SceneJsonParser;
import com.estaid.plot.dto.SceneDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * plots 테이블 매핑 엔티티 (조회 전용).
 *
 * <p>{@link #getParsedScenes}를 사용하면 scenesJson 역직렬화 결과가
 * {@code @Transient} 필드에 캐싱되어, 같은 인스턴스에서 반복 파싱을 방지한다.</p>
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "plots")
public class PlotEntity {

    @Id
    @Column(name = "plot_id", nullable = false, length = 36)
    private String plotId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "idea", nullable = false)
    private String idea;

    @Column(name = "art_style")
    private String artStyle;

    @Column(name = "character_id", length = 36)
    private String characterId;

    @Column(name = "background_id", length = 36)
    private String backgroundId;

    @Column(name = "scenes_json")
    private String scenesJson;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    /** scenesJson 역직렬화 캐시 (DB 컬럼 아님, 같은 인스턴스 내 반복 파싱 방지) */
    @Transient
    private transient List<SceneDto> cachedScenes;

    /**
     * scenesJson을 SceneDto 목록으로 역직렬화하되, 결과를 캐싱하여 반복 파싱을 방지한다.
     *
     * @param mapper Jackson ObjectMapper
     * @return 씬 DTO 목록 (빈 리스트 가능)
     */
    public List<SceneDto> getParsedScenes(ObjectMapper mapper) {
        if (cachedScenes == null) {
            cachedScenes = SceneJsonParser.parse(scenesJson, mapper);
        }
        return cachedScenes;
    }
}
