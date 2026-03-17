package com.estaid.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** backgrounds 테이블 매핑 엔티티. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "backgrounds")
public class BackgroundEntity {

    @Id
    @Column(name = "background_id", nullable = false, length = 36)
    private String backgroundId;

    @Column(name = "project_id", length = 36)
    private String projectId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "reference_image_url")
    private String referenceImageUrl;

    @Column(name = "art_style")
    private String artStyle;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
