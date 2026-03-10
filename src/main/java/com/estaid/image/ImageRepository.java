package com.estaid.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 이미지 Repository
 *
 * <p>Spring Data JPA가 런타임에 구현체를 자동 생성한다.
 * 기본 CRUD 메서드({@code save}, {@code findById}, {@code findAll}, {@code deleteById} 등)를
 * {@link JpaRepository}에서 상속한다.</p>
 */
public interface ImageRepository extends JpaRepository<Image, String> {

    /**
     * 특정 플롯에 속한 이미지 목록을 씬 순번 오름차순으로 조회한다.
     *
     * @param plotId 플롯 고유 식별자
     * @return 해당 플롯의 이미지 목록 (씬 순번 오름차순)
     */
    List<Image> findByPlot_PlotIdOrderBySceneNumberAsc(String plotId);

    /**
     * 특정 플롯의 특정 씬에 속한 이미지 목록을 조회한다.
     *
     * @param plotId      플롯 고유 식별자
     * @param sceneNumber 씬 순번
     * @return 해당 씬의 이미지 목록
     */
    List<Image> findByPlot_PlotIdAndSceneNumber(String plotId, Integer sceneNumber);
}
