package com.estaid.content.repository;

import com.estaid.content.entity.PlotEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** 플롯 엔티티 저장소 (조회 전용). */
@Repository("contentPlotRepository")
public interface PlotRepository extends JpaRepository<PlotEntity, String> {
    /** 프로젝트에 속한 플롯을 생성 순으로 조회한다. */
    List<PlotEntity> findByProjectIdOrderByCreatedAtAsc(String projectId);

    /** 여러 프로젝트에 속한 플롯을 한번에 조회한다 (N+1 방지용 배치 쿼리). */
    List<PlotEntity> findByProjectIdInOrderByCreatedAtAsc(List<String> projectIds);
}
