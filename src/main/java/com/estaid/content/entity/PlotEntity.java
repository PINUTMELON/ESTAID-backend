package com.estaid.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** plots 테이블 매핑 엔티티. */
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
}
