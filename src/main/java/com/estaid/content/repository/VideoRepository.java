package com.estaid.content.repository;

import com.estaid.content.entity.VideoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 영상 엔티티 저장소 (조회 전용). */
@Repository("contentVideoRepository")
public interface VideoRepository extends JpaRepository<VideoEntity, String> {

    /** 씬의 최신 영상 한 건을 조회한다. */
    Optional<VideoEntity> findTopByPlotIdAndSceneNumberAndVideoTypeOrderByCreatedAtDesc(
            String plotId, Integer sceneNumber, String videoType);

    @Query(value = """
            select v.*
            from videos v
            join plots p on p.plot_id = v.plot_id
            join projects pr on pr.project_id = p.project_id
            where pr.user_id <> :currentUserId
              and v.video_type = :videoType
              and v.video_url is not null
              and trim(v.video_url) <> ''
            order by random()
            limit 1
            """, nativeQuery = true)
    Optional<VideoEntity> findRandomByVideoTypeAndProjectUserIdNot(
            @Param("videoType") String videoType,
            @Param("currentUserId") String currentUserId);

    /** 플롯에 존재하는 씬 번호 목록을 영상 기준으로 조회한다. */
    @Query("""
            select distinct v.sceneNumber
            from VideoEntity v
            where v.plotId = :plotId
              and v.videoType = :videoType
              and v.sceneNumber is not null
            order by v.sceneNumber asc
            """)
    List<Integer> findDistinctSceneNumbersByPlotIdAndVideoType(
            @Param("plotId") String plotId,
            @Param("videoType") String videoType);

    /** 여러 플롯의 씬 영상을 한번에 조회한다 (N+1 방지용 배치 쿼리). */
    @Query("""
            select v from VideoEntity v
            where v.plotId in :plotIds
              and v.videoType = :videoType
            order by v.plotId, v.sceneNumber, v.createdAt desc
            """)
    List<VideoEntity> findByPlotIdsAndVideoType(
            @Param("plotIds") List<String> plotIds,
            @Param("videoType") String videoType);
}
