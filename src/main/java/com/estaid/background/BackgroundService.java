package com.estaid.background;

import com.estaid.common.exception.BusinessException;
import com.estaid.content.entity.BackgroundEntity;
import com.estaid.content.repository.BackgroundRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배경 자산 비즈니스 로직 서비스
 *
 * <p>배경 자산(BackgroundEntity) 생성·삭제를 담당한다.
 * 캐릭터({@code CharacterService})와 동일한 패턴으로 구현된다.</p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>{@link #create}  - 배경 엔티티 생성 및 저장</li>
 *   <li>{@link #delete}  - 배경 엔티티 삭제</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackgroundService {

    /** 배경 엔티티 Repository */
    private final BackgroundRepository backgroundRepository;

    /**
     * 배경 자산을 생성하고 DB에 저장한다.
     *
     * @param projectId         소속 프로젝트 UUID
     * @param name              배경 이름
     * @param description       배경 설명
     * @param referenceImageUrl Supabase Storage에 업로드된 참조 이미지 URL
     * @param artStyle          화풍 (예: REALISTIC, ANIME)
     * @return 저장된 배경 엔티티
     */
    @Transactional
    public BackgroundEntity create(String projectId, String name, String description,
                                   String referenceImageUrl, String artStyle) {
        // 배경 엔티티 생성 및 저장
        BackgroundEntity background = BackgroundEntity.builder()
                .projectId(projectId)
                .name(name)
                .description(description)
                .referenceImageUrl(referenceImageUrl)
                .artStyle(artStyle)
                .build();

        BackgroundEntity saved = backgroundRepository.save(background);
        log.info("배경 생성 완료: backgroundId={}, name={}, projectId={}",
                saved.getBackgroundId(), saved.getName(), projectId);
        return saved;
    }

    /**
     * 배경 자산을 삭제한다.
     *
     * <p>DB의 plots.background_id는 FK이므로, 해당 배경을 참조하는 플롯이 있다면
     * DB 제약에 따라 처리된다. (schema.sql의 ON DELETE 정책 확인 필요)</p>
     *
     * @param backgroundId 삭제할 배경 UUID
     * @throws BusinessException 배경이 존재하지 않을 때 (404)
     */
    @Transactional
    public void delete(String backgroundId) {
        BackgroundEntity background = backgroundRepository.findById(backgroundId)
                .orElseThrow(() -> new BusinessException(
                        "배경을 찾을 수 없습니다. id=" + backgroundId, HttpStatus.NOT_FOUND));
        backgroundRepository.delete(background);
        log.info("배경 삭제 완료: backgroundId={}", backgroundId);
    }
}
