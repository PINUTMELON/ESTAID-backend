package com.estaid.content.controller;

import com.estaid.auth.service.AuthenticatedUserService;
import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.VideoUrlResponse;
import com.estaid.content.service.ContentQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoDetailController {

    private final ContentQueryService contentQueryService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping("/random")
    public ApiResponse<VideoUrlResponse> getRandomVideo(HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getRandomOtherUserVideo(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<VideoUrlResponse> getVideo(@PathVariable("id") String videoId) {
        return ApiResponse.ok(contentQueryService.getVideoUrl(videoId));
    }
}
