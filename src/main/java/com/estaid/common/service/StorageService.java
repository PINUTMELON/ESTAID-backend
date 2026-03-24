package com.estaid.common.service;

import com.estaid.common.exception.BusinessException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Supabase Storage 파일 업로드 서비스
 *
 * <p>프론트엔드에서 전송한 이미지 파일을 Supabase Storage에 업로드하고
 * 공개 접근 가능한 URL을 반환한다.</p>
 *
 * <p>업로드 흐름:</p>
 * <pre>
 *   MultipartFile 수신
 *   → 고유 파일명 생성 (UUID + 원본 확장자)
 *   → Supabase Storage REST API로 업로드
 *   → 공개 URL 반환
 * </pre>
 */
@Slf4j
@Service
public class StorageService {

    /** Supabase 프로젝트 URL (예: https://xxx.supabase.co) */
    @Value("${supabase.url}")
    private String supabaseUrl;

    /** Supabase Service Role Key (스토리지 업로드 권한) */
    @Value("${supabase.service-role-key}")
    private String serviceRoleKey;

    /** 업로드할 버킷 이름 */
    @Value("${supabase.storage.bucket}")
    private String bucket;

    /** Supabase Storage 호출용 WebClient */
    private WebClient storageWebClient;

    /**
     * 빈 초기화 후 WebClient 구성
     */
    @PostConstruct
    private void initWebClient() {
        this.storageWebClient = WebClient.builder()
                .baseUrl(supabaseUrl + "/storage/v1/object")
                .defaultHeader("Authorization", "Bearer " + serviceRoleKey)
                .build();
        log.info("Supabase Storage WebClient 초기화 완료: bucket={}", bucket);
    }

    /**
     * 이미지 파일을 Supabase Storage에 업로드하고 공개 URL을 반환한다.
     *
     * @param file 업로드할 이미지 파일 (MultipartFile)
     * @return 업로드된 이미지의 공개 URL
     * @throws BusinessException 업로드 실패 또는 지원하지 않는 파일 형식 시
     */
    public String uploadImage(MultipartFile file) {
        // 파일 검증
        validateImageFile(file);

        // 고유 파일명 생성 (UUID + 원본 확장자)
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storagePath = "uploads/" + UUID.randomUUID() + extension;

        log.info("Supabase Storage 업로드 시작: path={}, size={}bytes", storagePath, file.getSize());

        try {
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null
                    ? file.getContentType()
                    : MediaType.APPLICATION_OCTET_STREAM_VALUE;

            // Supabase Storage REST API 호출
            storageWebClient.post()
                    .uri("/" + bucket + "/" + storagePath)
                    .contentType(MediaType.parseMediaType(contentType))
                    .bodyValue(fileBytes)
                    .retrieve()
                    .onStatus(status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(
                                            new RuntimeException("Supabase Storage 업로드 오류: " + body))))
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            // 공개 URL 생성
            String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + storagePath;
            log.info("Supabase Storage 업로드 완료: url={}", publicUrl);
            return publicUrl;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Supabase Storage 업로드 실패: {}", e.getMessage());
            throw new BusinessException("이미지 업로드에 실패했습니다: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    // ─────────────────────────────────────────
    // private 헬퍼
    // ─────────────────────────────────────────

    /**
     * 이미지 파일 유효성을 검증한다.
     * 빈 파일이거나 이미지가 아닌 경우 예외를 발생시킨다.
     *
     * @param file 검증할 파일
     * @throws BusinessException 유효하지 않은 파일 시
     */
    /** FAL.ai가 지원하는 이미지 Content-Type 목록 (AVIF, HEIC 등 미지원 포맷 차단) */
    private static final java.util.Set<String> SUPPORTED_IMAGE_TYPES = java.util.Set.of(
            "image/png", "image/jpeg", "image/jpg", "image/webp", "image/gif"
    );

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("업로드할 파일이 없습니다.", HttpStatus.BAD_REQUEST);
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("이미지 파일만 업로드 가능합니다. contentType=" + contentType, HttpStatus.BAD_REQUEST);
        }
        // FAL.ai 미지원 포맷 차단 (AVIF, HEIC 등)
        if (!SUPPORTED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(
                    "지원하지 않는 이미지 형식입니다. PNG, JPG, WebP 형식만 사용 가능합니다. (현재: " + contentType + ")",
                    HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 파일명에서 확장자를 추출한다.
     *
     * @param filename 원본 파일명
     * @return 확장자 (예: ".jpg") - 없으면 ".jpg" 기본값
     */
    private String getExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf("."));
        }
        return ".jpg";
    }
}
