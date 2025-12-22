package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.PostureImage;
import com.example.fitnessgym_mg.entity.enums.PostureImagePosition;

/**
 * 姿勢画像エンティティ用リポジトリ
 * 姿勢画像の検索、姿勢グループ別取得などの機能を提供
 */
@Repository
public interface PostureImageRepository extends JpaRepository<PostureImage, UUID> {

	/**
	 * 姿勢グループIDに紐づく姿勢画像を撮影日時の昇順で取得
	 * 
	 * @param postureGroupId 姿勢グループID
	 * @return 姿勢画像のリスト（撮影日時の昇順）
	 */
    @Query("SELECT pi FROM PostureImage pi WHERE pi.postureGroup.id = :postureGroupId ORDER BY pi.takenAt ASC")
    List<PostureImage> findByPostureGroupIdOrderByTakenAtAsc(@Param("postureGroupId") UUID postureGroupId);
    
	/**
	 * 姿勢グループIDと位置で姿勢画像を検索
	 * 
	 * @param postureGroupId 姿勢グループID
	 * @param position 画像位置（FRONT, SIDE, BACK）
	 * @return 姿勢画像（存在する場合）
	 */
    @Query("SELECT pi FROM PostureImage pi WHERE pi.postureGroup.id = :postureGroupId AND pi.position = :position")
    Optional<PostureImage> findByPostureGroupIdAndPosition(@Param("postureGroupId") UUID postureGroupId, @Param("position") PostureImagePosition position);
    
	/**
	 * IDのリストで姿勢画像を一括取得
	 * 
	 * @param ids 姿勢画像IDのリスト
	 * @return 姿勢画像のリスト
	 */
    List<PostureImage> findAllByIdIn(List<UUID> ids);
}

