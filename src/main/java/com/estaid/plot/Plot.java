package com.estaid.plot;

import com.estaid.character.Character;
import com.estaid.project.Project;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * 플롯 엔티티
 *
 * <p>사용자가 입력한 스토리 아이디어를 바탕으로 Claude AI가 생성한 씬 목록을 저장한다.
 * 씬 목록(scenes_json)은 JSON 문자열로 직렬화하여 단일 컬럼에 저장한다.</p>
 *
 * <p>플롯 생성 흐름:</p>
 * <pre>
 *   사용자 아이디어 입력 (title, idea, sceneCount)
 *        → Claude API 호출 → N개의 씬 JSON 생성
 *        → scenes_json 컬럼에 저장
 *        → 사용자가 표에서 씬 내용 직접 수정 가능
 * </pre>
 *
 * <p>각 씬(SceneDto) 구성: sceneNumber, title, characters, composition,
 * background, lighting, mainStory, firstFramePrompt, lastFramePrompt</p>
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
     * 소속 프로젝트
     * 프로젝트 삭제 시 플롯도 함께 삭제된다 (ON DELETE CASCADE).
     * LAZY 로딩: 플롯 조회 시 프로젝트 정보는 실제 접근 시점에 로드된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /**
     * 플롯 제목 (AI 자동 생성 또는 사용자 입력)
     * 예: "괴물과의 첫 대결", "벚꽃 공원에서의 만남"
     */
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    /**
     * 사용자가 입력한 원본 스토리 아이디어
     * Claude API 호출 시 씬 생성의 기반이 되는 텍스트다.
     * 예: "주인공이 도심 폐허에서 거대 괴물과 맞닥뜨려 싸우는 장면"
     */
    @Column(name = "idea", columnDefinition = "TEXT", nullable = false)
    private String idea;

    /**
     * 화풍 설정 (캐릭터의 artStyle과 일치시킨다)
     * 이미지/영상 생성 프롬프트에 일관되게 적용된다.
     * 예: anime, realistic, webtoon
     */
    @Column(name = "art_style", length = 50)
    private String artStyle;

    /**
     * 참조 캐릭터 (선택적 - 이미지 생성 시 외형 일관성 유지용)
     * 캐릭터 삭제 시 NULL로 설정됨 (ON DELETE SET NULL).
     * LAZY 로딩: 플롯 조회 시 캐릭터 정보는 실제 접근 시점에 로드된다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id")
    private Character character;

    /**
     * AI가 생성한 씬 목록 (JSON 배열 형태로 직렬화하여 저장)
     *
     * <p>저장 형식 예시:</p>
     * <pre>
     * [
     *   {
     *     "sceneNumber": 1,
     *     "title": "괴물의 등장",
     *     "characters": "주인공, 괴물",
     *     "composition": "wide cinematic shot",
     *     "background": "폐허가 된 도심, 먼지와 잔해",
     *     "lighting": "dark storm clouds, dramatic lightning",
     *     "mainStory": "안개 속에서 거대 괴물의 실루엣이 드러나며 주인공과 대치한다",
     *     "firstFramePrompt": "A lone hero holding a sword...",
     *     "lastFramePrompt": "The giant monster fully emerges..."
     *   }
     * ]
     * </pre>
     *
     * ObjectMapper를 통해 {@code List<SceneDto>}로 역직렬화하여 사용한다.
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
