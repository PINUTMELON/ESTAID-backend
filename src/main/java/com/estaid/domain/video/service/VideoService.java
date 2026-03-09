package com.estaid.domain.video.service;

import com.estaid.domain.video.dto.VideoGenerateRequest;
import com.estaid.domain.video.dto.VideoMergeRequest;
import com.estaid.domain.video.dto.VideoResponse;

import java.util.List;

/**
 * 영상 서비스 인터페이스
 */
public interface VideoService {

    /** 씬 단위 영상 생성 */
    VideoResponse generateVideo(VideoGenerateRequest request);

    /** 여러 영상 병합 */
    VideoResponse mergeVideos(VideoMergeRequest request);

    /** 영상 단건 조회 */
    VideoResponse getVideo(String videoId);

    /** 특정 플롯의 씬 영상 전체 조회 */
    List<VideoResponse> getSceneVideosByPlot(String plotId);
}
