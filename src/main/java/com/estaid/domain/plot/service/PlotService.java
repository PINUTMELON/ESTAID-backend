package com.estaid.domain.plot.service;

import com.estaid.domain.plot.dto.PlotCreateRequest;
import com.estaid.domain.plot.dto.PlotResponse;

/**
 * 플롯 서비스 인터페이스
 */
public interface PlotService {

    /**
     * 플롯 생성
     * - 사용자 아이디어를 Claude API에 전달하여 씬 목록을 생성한다.
     */
    PlotResponse createPlot(PlotCreateRequest request);

    /** 플롯 단건 조회 */
    PlotResponse getPlot(String plotId);
}
