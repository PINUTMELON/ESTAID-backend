package com.estaid.domain.image.repository;

import com.estaid.domain.image.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 이미지 레포지토리
 */
public interface ImageRepository extends JpaRepository<Image, String> {

    /** 특정 플롯의 모든 이미지를 씬 순번 오름차순으로 조회 */
    List<Image> findByPlotIdOrderBySceneNumberAsc(String plotId);
}
