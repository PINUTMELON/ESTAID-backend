package com.estaid.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** videos 테이블 매핑 엔티티. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "videos")
public class VideoEntity {

    @Id
    @Column(name = "video_id", nullable = false, length = 36)
    private String videoId;

    @Column(name = "plot_id", length = 36)
    private String plotId;

    @Column(name = "scene_number")
    private Integer sceneNumber;

    @Column(name = "video_prompt")
    private String videoPrompt;

    @Column(name = "first_image_id", length = 36)
    private String firstImageId;

    @Column(name = "last_image_id", length = 36)
    private String lastImageId;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "duration")
    private Integer duration;

    @Column(name = "video_type", nullable = false)
    private String videoType;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}
