package com.estaid.plot;

import com.estaid.character.Character;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 플롯 엔티티
 *
 * <p>사용자가 입력한 아이디어를 바탕으로 AI가 생성한 스토리 플롯을 저장한다.
 * 씬 목록(scenes_json)은 JSON 문자열로 직렬화하여 단일 컬럼에 저장한다.</p>
 *
 * <p>DB 테이블: {@code plots}</p>
 */
@Entity
@Table(name = "plots")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Plot {

    /**
     * 플롯 고유 식별자 (UUID, VARCHAR 36자리)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plot_id", length = 36)
    private String plotId;

    /**
     * 플롯 제목 (AI 자동 생성 또는 사용자 입력)
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /**
     * 사용자가 입력한 원본 아이디어 텍스트
     */
    @Column(name = "idea", columnDefinition = "TEXT", nullable = false)
    private String idea;

    /**
     * 화풍 (캐릭터 등록 시 설정한 art_style과 일치시킨다)
     */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /**
     * 참조 캐릭터 (선택적 - 이미지 생성 시 외형 일관성 유지용)
     * 캐릭터 삭제 시 NULL로 설정됨 (ON DELETE SET NULL)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    /**
     * AI가 생성한 씬 목록 (JSON 배열 형태로 직렬화하여 저장)
     * 예: [{"sceneNumber":1,"description":"...","firstFramePrompt":"..."}]
     */
    @Column(name = "scenes_json", columnDefinition = "TEXT")
    private String scenesJson;

    /**
     * 레코드 생성 시각 (최초 저장 시 자동 설정, 이후 변경 불가)
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    /**
     * 엔티티 최초 저장 전 호출 - 생성 시각 초기화
     */
    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
