package com.estaid.asset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Asset Repository
 *
 * <p>프로젝트에 저장된 캐릭터·배경 이미지 자산을 조회한다.</p>
 */
public interface AssetRepository extends JpaRepository<Asset, String> {

    /**
     * 프로젝트의 전체 Asset 목록을 생성 순으로 조회한다.
     *
     * @param projectId 프로젝트 UUID
     * @return Asset 목록 (생성 시각 오름차순)
     */
    List<Asset> findByProject_ProjectIdOrderByCreatedAtAsc(String projectId);

    /**
     * 프로젝트의 특정 타입 Asset 목록을 생성 순으로 조회한다.
     *
     * @param projectId 프로젝트 UUID
     * @param type      Asset 종류 (CHARACTER / BACKGROUND)
     * @return Asset 목록 (생성 시각 오름차순)
     */
    List<Asset> findByProject_ProjectIdAndTypeOrderByCreatedAtAsc(String projectId, Asset.AssetType type);
}
