package com.estaid.content.controller;

import com.estaid.auth.service.AuthenticatedUserService;
import com.estaid.common.response.ApiResponse;
import com.estaid.content.dto.GalleryItemResponse;
import com.estaid.content.service.ContentQueryService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/gallery")
@RequiredArgsConstructor
public class GalleryQueryController {

    private final ContentQueryService contentQueryService;
    private final AuthenticatedUserService authenticatedUserService;

    @GetMapping
    public ApiResponse<List<GalleryItemResponse>> getGallery(HttpServletRequest request) {
        String userId = authenticatedUserService.requireCurrentUserId(request);
        return ApiResponse.ok(contentQueryService.getGallery(userId));
    }
}
