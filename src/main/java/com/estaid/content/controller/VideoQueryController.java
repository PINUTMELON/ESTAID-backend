package com.estaid.content.controller;

import com.estaid.content.service.ContentQueryService;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 영상 재생 URL로 리다이렉트하는 API를 제공한다. */
@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoQueryController {

    private final ContentQueryService contentQueryService;

    /** 저장된 재생 URL로 302 리다이렉트한다. */
    @GetMapping("/{id}")
    public ResponseEntity<Void> playVideo(@PathVariable("id") String videoId) {
        URI playbackUri = contentQueryService.getVideoPlaybackUri(videoId);
        return ResponseEntity.status(HttpStatus.FOUND).location(playbackUri).build();
    }
}
