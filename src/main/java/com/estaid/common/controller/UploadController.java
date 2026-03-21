package com.estaid.common.controller;

import com.estaid.common.response.ApiResponse;
import com.estaid.common.service.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 파일 업로드 컨트롤러
 *
 * <p>프론트엔드에서 전송한 이미지 파일을 Supabase Storage에 업로드하고
 * 공개 URL을 반환한다.</p>
 *
 * <ul>
 *   <li>POST /api/upload/image - 이미지 파일 업로드 → 공개 URL 반환</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    /** Supabase Storage 업로드 서비스 */
    private final StorageService storageService;

    /**
     * 이미지 파일을 Supabase Storage에 업로드한다.
     *
     * <p>프론트엔드는 {@code multipart/form-data} 형식으로 파일을 전송한다.
     * 업로드 성공 시 공개 URL을 반환하며, 이 URL을 캐릭터의 {@code referenceImageUrl}로 사용한다.</p>
     *
     * <p>요청 예시 (form-data):</p>
     * <pre>
     *   POST /api/upload/image
     *   Content-Type: multipart/form-data
     *   file: [이미지 파일]
     * </pre>
     *
     * @param file 업로드할 이미지 파일 (form-data key: "file")
     * @return 200 OK + 업로드된 이미지의 공개 URL
     */
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadImage(
            @RequestParam("file") MultipartFile file) {
        String imageUrl = storageService.uploadImage(file);
        return ResponseEntity.ok(ApiResponse.ok("이미지가 업로드되었습니다.", Map.of("imageUrl", imageUrl)));
    }
}
