package com.estaid.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** images 테이블 매핑 엔티티. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "images")
public class ImageEntity {

    @Id
    @Column(name = "image_id", nullable = false, length = 36)
    private String imageId;

    @Column(name = "plot_id", nullable = false, length = 36)
    private String plotId;

    @Column(name = "scene_number", nullable = false)
    private Integer sceneNumber;

    @Column(name = "frame_type", nullable = false)
    private String frameType;

    @Column(name = "prompt")
    private String prompt;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
