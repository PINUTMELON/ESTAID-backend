package com.estaid.domain.plot.repository;

import com.estaid.domain.plot.entity.Plot;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 플롯 레포지토리
 */
public interface PlotRepository extends JpaRepository<Plot, String> {
}
