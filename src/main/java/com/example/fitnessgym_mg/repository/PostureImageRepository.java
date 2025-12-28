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
    
    /**
     * 画像IDからPostureGroupIDを取得
     * 認可チェック用の軽量なクエリ
     * 
     * @deprecated 認可用クエリはexists系のみを使用することを推奨。将来的に削除予定。
     * 
     * @param imageId 画像ID
     * @return PostureGroupID（存在する場合）
     */
    @Deprecated
    @Query("SELECT pi.postureGroup.id FROM PostureImage pi WHERE pi.id = :imageId")
    Optional<UUID> findPostureGroupIdByImageId(@Param("imageId") UUID imageId);
    
    /**
     * ユーザーが指定された姿勢画像にアクセス可能か確認（存在確認専用クエリ）
     * 
     * <p>認可用クエリはexists系のみを使用し、「取得」と「可否判定」を混ぜない。</p>
     * <p>姿勢画像へのアクセス権限はレッスンへのアクセス権限に依存するため、
     * 姿勢画像IDからレッスンIDを取得し、レッスンへのアクセス権限を確認する。</p>
     * 
     * <p>EXISTSを使用することで、COUNTベースのクエリよりも効率的に動作します。
     * 早期停止が可能になり、パフォーマンスが向上します。</p>
     * 
     * @param userId ユーザーID
     * @param imageId 画像ID
     * @return アクセス可能な場合 true
     */
    @Query("""
		SELECT CASE WHEN EXISTS (
			SELECT 1
			FROM PostureImage pi
			JOIN pi.postureGroup pg
			JOIN pg.lesson l
			JOIN l.customer c
			WHERE pi.id = :imageId
			  AND (
			    EXISTS (
			      SELECT 1 FROM User u
			      WHERE u.id = :userId
			        AND u.role = com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN
			    )
			    OR EXISTS (
			      SELECT 1 FROM User u
			      JOIN u.stores ms
			      JOIN c.stores cs
			      WHERE u.id = :userId
			        AND ms.id = cs.id
			    )
			    OR EXISTS (
			      SELECT 1 FROM UserCustomer uc
			      WHERE uc.id.userId = :userId
			        AND uc.id.customerId = c.id
			    )
			  )
		) THEN true ELSE false END
		""")
    boolean existsAccessiblePostureImage(
        @Param("userId") UUID userId,
        @Param("imageId") UUID imageId
    );
    
    /**
     * ユーザーが指定されたすべての姿勢画像にアクセス可能か確認（バッチ認可用クエリ）
     * 
     * <p>認可用クエリはexists系のみを使用し、「取得」と「可否判定」を混ぜない。</p>
     * <p>姿勢画像へのアクセス権限はレッスンへのアクセス権限に依存するため、
     * 姿勢画像IDからレッスンIDを取得し、レッスンへのアクセス権限を確認する。</p>
     * 
     * <p>このメソッドは、指定されたすべての画像IDがアクセス可能な場合にtrueを返します。
     * 一部でもアクセス不可の画像がある場合はfalseを返します。</p>
     * 
     * <p>Repository内でimageCountを計算することで、呼び出し側の誤りによるバグを防止します。
     * これにより、仕様変更やリファクタ時の壊れやすさを低減します。</p>
     * 
     * @param userId ユーザーID
     * @param imageIds 画像IDリスト
     * @return すべての画像がアクセス可能な場合 true
     */
    @Query("""
		SELECT CASE WHEN COUNT(DISTINCT pi.id) = (
			SELECT COUNT(i)
			FROM PostureImage i
			WHERE i.id IN :imageIds
		) THEN true ELSE false END
		FROM PostureImage pi
		JOIN pi.postureGroup pg
		JOIN pg.lesson l
		JOIN l.customer c
		WHERE pi.id IN :imageIds
		  AND (
		    EXISTS (
		      SELECT 1 FROM User u
		      WHERE u.id = :userId
		        AND u.role = com.example.fitnessgym_mg.entity.enums.UserRole.ADMIN
		    )
		    OR EXISTS (
		      SELECT 1 FROM User u
		      JOIN u.stores ms
		      JOIN c.stores cs
		      WHERE u.id = :userId
		        AND ms.id = cs.id
		    )
		    OR EXISTS (
		      SELECT 1 FROM UserCustomer uc
		      WHERE uc.id.userId = :userId
		        AND uc.id.customerId = c.id
		    )
		  )
		""")
    boolean existsAllAccessiblePostureImages(
        @Param("userId") UUID userId,
        @Param("imageIds") List<UUID> imageIds
    );
}

