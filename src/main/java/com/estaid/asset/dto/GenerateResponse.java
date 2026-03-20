package com.estaid.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 이미지 임시 생성 응답 DTO
 *
 * <p>FAL.ai가 생성한 이미지 URL을 반환한다.
 * DB에 저장된 것이 아니므로 "프로젝트에 사용하기"를 누르면
 * {@code POST /api/projects/{id}/assets}로 별도 저장해야 한다.</p>
 */
@Getter
@AllArgsConstructor
public class GenerateResponse {

    /**
     * FAL.ai가 생성한 이미지 URL
     * 7일 후 자동 만료되므로 저장이 필요하면 별도 스토리지로 이전해야 한다.
     */
    private String imageUrl;
}
