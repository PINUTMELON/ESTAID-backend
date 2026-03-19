package com.estaid.content.repository;

import com.estaid.content.entity.ImageEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** 이미지 엔티티 저장소 (조회 전용). */
@Repository("contentImageRepository")
public interface ImageRepository extends JpaRepository<ImageEntity, String> {

    /** 씬 이미지 목록을 프레임 타입 순으로 조회한다. */
    List<ImageEntity> findByPlotIdAndSceneNumberOrderByFrameTypeAsc(String plotId, Integer sceneNumber);

    /** 플롯에 존재하는 씬 번호 목록을 중복 없이 조회한다. */
    @Query("""
            select distinct i.sceneNumber
            from ImageEntity i
            where i.plotId = :plotId
              and i.sceneNumber is not null
            order by i.sceneNumber asc
            """)
    List<Integer> findDistinctSceneNumbersByPlotId(@Param("plotId") String plotId);
}
