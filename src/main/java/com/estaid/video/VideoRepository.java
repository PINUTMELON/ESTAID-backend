package com.estaid.video;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 영상 Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD 메서드({@code save}, {@code findById}, {@code findAll}, {@code deleteById} 등)를
 * {@link JpaRepository}에서 상속한다.</p>
 */
public interface VideoRepository extends JpaRepository<Video, String> {

    /**
     * 특정 플롯에 속한 영상 목록을 씬 순번 오름차순으로 조회한다.
     *
     * @param plotId 플롯 고유 식별자
     * @return 해당 플롯의 영상 목록 (씬 순번 오름차순)
     */
    List<Video> findByPlot_PlotIdOrderBySceneNumberAsc(String plotId);

    /**
     * 특정 플롯의 특정 영상 종류에 해당하는 영상 목록을 조회한다.
     * 예: plotId의 SCENE 타입 영상 전체 조회 (병합 전 검증 용도)
     *
     * @param plotId    플롯 고유 식별자
     * @param videoType 영상 종류 ({@link Video.VideoType})
     * @return 해당 플롯·종류의 영상 목록
     */
    List<Video> findByPlot_PlotIdAndVideoType(String plotId, Video.VideoType videoType);

    /**
     * 특정 플롯의 특정 영상 종류 + 상태에 해당하는 영상 목록을 씬 순번 오름차순으로 조회한다.
     * 예: SCENE 타입이고 COMPLETED 상태인 영상만 조회 (병합 가능 여부 확인 용도)
     *
     * @param plotId    플롯 고유 식별자
     * @param videoType 영상 종류
     * @param status    생성 상태
     * @return 조건에 맞는 영상 목록 (씬 순번 오름차순)
     */
    List<Video> findByPlot_PlotIdAndVideoTypeAndStatusOrderBySceneNumberAsc(
            String plotId,
            Video.VideoType videoType,
            Video.GenerationStatus status
    );
}
