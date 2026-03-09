package com.estaid.domain.video.service;

import com.estaid.common.exception.BusinessException;
import com.estaid.domain.video.dto.VideoGenerateRequest;
import com.estaid.domain.video.dto.VideoMergeRequest;
import com.estaid.domain.video.dto.VideoResponse;
import com.estaid.domain.video.entity.Video;
import com.estaid.domain.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 영상 서비스 구현체
 *
 * [개발 가이드]
 * 영상 생성 AI API 연동 방법:
 * - 추천: Kling AI, Runway Gen-3, Pika Labs
 * - image-to-video: 첫 프레임 URL + 마지막 프레임 URL + 프롬프트 전달
 * - 영상 병합: FFmpeg 라이브러리 또는 영상 편집 API 사용
 *
 * TODO:
 *  1. 영상 생성 AI API WebClient 설정 추가
 *  2. generateVideo()에서 firstImage, lastImage URL 가져와 API 호출
 *  3. mergeVideos()에서 FFmpeg 또는 외부 API로 병합 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoServiceImpl implements VideoService {

    private final VideoRepository videoRepository;

    /**
     * 씬 단위 영상 생성
     * TODO: 이미지 레포지토리에서 firstImageId, lastImageId의 URL을 조회한 뒤
     *       영상 생성 AI API 호출 구현
     */
    @Transactional
    @Override
    public VideoResponse generateVideo(VideoGenerateRequest request) {
        log.info("영상 생성 요청 - plotId: {}, sceneNumber: {}", request.getPlotId(), request.getSceneNumber());

        Video video = Video.builder()
                .plotId(request.getPlotId())
                .sceneNumber(request.getSceneNumber())
                .firstImageId(request.getFirstImageId())
                .lastImageId(request.getLastImageId())
                .videoPrompt(request.getVideoPrompt())
                .duration(request.getDuration())
                .videoType(Video.VideoType.SCENE)
                .status(Video.GenerationStatus.PENDING)
                .build();

        Video saved = videoRepository.save(video);

        // TODO: 영상 생성 AI API 호출
        // TODO: 결과 URL로 상태 업데이트

        return VideoResponse.from(saved);
    }

    /**
     * 여러 영상 병합
     * TODO: videoIds의 videoUrl 목록을 순서대로 FFmpeg 또는 외부 API로 병합
     */
    @Transactional
    @Override
    public VideoResponse mergeVideos(VideoMergeRequest request) {
        log.info("영상 병합 요청 - 영상 수: {}", request.getVideoIds().size());

        Video mergedVideo = Video.builder()
                .videoType(Video.VideoType.MERGED)
                .status(Video.GenerationStatus.PENDING)
                .build();

        Video saved = videoRepository.save(mergedVideo);

        // TODO: 병합 로직 구현

        return VideoResponse.from(saved);
    }

    /** 영상 단건 조회 */
    @Transactional(readOnly = true)
    @Override
    public VideoResponse getVideo(String videoId) {
        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException("영상을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return VideoResponse.from(video);
    }

    /** 특정 플롯의 씬 영상 전체 조회 */
    @Transactional(readOnly = true)
    @Override
    public List<VideoResponse> getSceneVideosByPlot(String plotId) {
        return videoRepository.findByPlotIdAndVideoTypeOrderBySceneNumberAsc(plotId, Video.VideoType.SCENE)
                .stream()
                .map(VideoResponse::from)
                .collect(Collectors.toList());
    }
}
