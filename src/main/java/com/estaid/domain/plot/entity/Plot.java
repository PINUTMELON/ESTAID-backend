package com.estaid.domain.plot.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 플롯 엔티티
 * - 사용자가 입력한 아이디어를 바탕으로 AI가 생성한 씬 목록을 저장한다.
 * - 각 씬은 SceneItem 임베디드 객체 또는 별도 테이블로 관리할 수 있다.
 *   (현재는 JSON 문자열로 scenes 컬럼에 저장 - 단순화)
 *   TODO: 씬이 많아지거나 개별 조회가 필요하면 Scene 엔티티를 분리할 것
 */
@Entity
@Table(name = "plots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Plot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "plot_id", updatable = false, nullable = false)
    private String plotId;

    /** 플롯 제목 (AI가 자동 생성하거나 사용자가 입력) */
    @Column(nullable = false, length = 200)
    private String title;

    /** 사용자가 입력한 원본 아이디어 텍스트 */
    @Column(name = "idea", columnDefinition = "TEXT", nullable = false)
    private String idea;

    /** 화풍 (캐릭터 등록 시 설정한 artStyle과 일치시킨다) */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /** 참조 캐릭터 ID (선택적 - 이미지 생성 시 일관성 유지용) */
    @Column(name = "character_id", length = 36)
    private String characterId;

    /**
     * AI가 생성한 씬 목록 (JSON 직렬화하여 저장)
     * - 직렬화/역직렬화는 PlotService에서 담당한다.
     * - 추후 Scene 엔티티로 분리하면 이 컬럼은 제거한다.
     */
    @Column(name = "scenes_json", columnDefinition = "TEXT")
    private String scenesJson;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
