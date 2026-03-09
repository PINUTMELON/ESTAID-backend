package com.estaid.domain.video.repository;

import com.estaid.domain.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 영상 레포지토리
 */
public interface VideoRepository extends JpaRepository<Video, String> {

    /** 특정 플롯의 씬 단위 영상을 씬 순번 오름차순으로 조회 */
    List<Video> findByPlotIdAndVideoTypeOrderBySceneNumberAsc(String plotId, Video.VideoType videoType);
}
