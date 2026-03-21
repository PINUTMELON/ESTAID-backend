package com.estaid.content.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** projects 테이블 매핑 엔티티. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "projects")
public class ProjectEntity {

    @Id
    @Column(name = "project_id", nullable = false, length = 36)
    private String projectId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "background_image_url")
    private String backgroundImageUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
