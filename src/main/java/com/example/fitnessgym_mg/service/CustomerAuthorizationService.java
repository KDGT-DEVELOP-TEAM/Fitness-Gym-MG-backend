package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.CustomerRepositoryCustom;
import com.example.fitnessgym_mg.repository.LessonRepository;
import com.example.fitnessgym_mg.repository.UserCustomerRepository;
import com.example.fitnessgym_mg.service.policy.RolePolicy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 顧客へのアクセス権限チェックを一元管理するサービス
 * 認可ロジックをコントローラーから分離し、再利用可能にする
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>boolean版メソッド（canAccessXxx）: SpEL専用。AuthorizationFacadeに実装。</li>
 *   <li>例外版メソッド（checkCanAccessXxxOrThrow）: Service層での使用を推奨。AuthorizationFacadeに実装。</li>
 * </ul>
 * 
 * <p>このServiceは認可ロジックの内部実装のみを担当し、外部からはAuthorizationFacade経由でアクセスすること。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerAuthorizationService {

	private final CustomerRepository customerRepository;
	private final UserCustomerRepository userCustomerRepository;
	private final LessonRepository lessonRepository;
	private final RolePolicy rolePolicy;

	/**
	 * 現在のユーザーが指定された顧客にアクセス可能か確認（内部実装）
	 * 
	 * <p>このメソッドは内部実装用。外部からはAuthorizationFacade経由でアクセスすること。</p>
	 * 
	 * <p>設計方針: 存在確認と権限確認を分離しない。
	 * 各ロール別ロジック内で必要な情報が取れなければfalseを返す。
	 * 「見えないものは存在しない扱い」を実現し、情報漏洩リスクを低減する。</p>
	 * 
	 * @param currentUser 現在のユーザー
	 * @param customerId 顧客ID
	 * @return アクセス可能な場合 true
	 */
	boolean canAccessCustomerInternal(User currentUser, UUID customerId) {
		// 不正な引数は「アクセス不可」として扱う（例外は投げない設計）
		if (currentUser == null) {
			log.warn("Authorization check failed: currentUser is null. customerId={}", customerId);
			return false;
		}

		if (customerId == null) {
			log.warn("Authorization check failed: customerId is null. userId={}, customerId=null", currentUser.getId());
			return false;
		}

		log.debug("Authorization check: userId={}, role={}, customerId={}", currentUser.getId(), currentUser.getRole(), customerId);

		// スーパーユーザー（ADMIN）は全顧客にアクセス可能
		if (rolePolicy.isSuperUser(currentUser)) {
			log.debug("Authorization check: SuperUser access granted");
			return true;
		}

		// MANAGER: 自分の店舗に所属する顧客のみアクセス可能
		// 顧客が存在しない、または店舗に所属していない場合はfalseを返す
		if (currentUser.getRole() == UserRole.MANAGER) {
			boolean result = canManagerAccessCustomer(currentUser, customerId);
			log.debug("Authorization check: MANAGER access={} for customerId={}", result, customerId);
			return result;
		}

		// TRAINER: 担当している顧客のみアクセス可能
		// 顧客が存在しない、または割り当てられていない場合はfalseを返す
		if (currentUser.getRole() == UserRole.TRAINER) {
			boolean result = isTrainerAssignedToCustomer(currentUser.getId(), customerId);
			log.debug("Authorization check: TRAINER access={} for trainerId={}, customerId={}", result, currentUser.getId(), customerId);
			return result;
		}

		// その他のロール（現時点ではCUSTOMERロールは未対応）
		// 将来的にCUSTOMERロールが追加された場合は、currentUser.getId().equals(customerId) のチェックを実装予定
		log.warn("Authorization check failed: Unknown role={}", currentUser.getRole());
		return false;
	}

	/**
	 * マネージャーが顧客にアクセス可能か確認
	 * 
	 * <p>N+1問題を回避するため、RepositoryレベルのEXISTSクエリを使用。</p>
	 * 
	 * @param manager マネージャー
	 * @param customerId 顧客ID
	 * @return アクセス可能な場合 true
	 */
	private boolean canManagerAccessCustomer(User manager, UUID customerId) {
		// RepositoryレベルのEXISTSクエリで、マネージャーと顧客が同じ店舗に所属しているか確認
		// @SQLRestrictionを回避するため、ネイティブSQLクエリを使用するexistsManagerCustomerInSameStoreNative()を使用
		// CustomerRepositoryをCustomerRepositoryCustomにキャストして直接呼び出す
		try {
			if (customerRepository instanceof CustomerRepositoryCustom) {
				boolean result = ((CustomerRepositoryCustom) customerRepository)
						.existsManagerCustomerInSameStoreNative(manager.getId(), customerId);
				log.debug("canManagerAccessCustomer: managerId={}, customerId={}, result={}", 
						manager.getId(), customerId, result);
				return result;
			} else {
				log.error("CustomerRepository does not implement CustomerRepositoryCustom");
				return false;
			}
		} catch (Exception e) {
			log.error("canManagerAccessCustomer failed: managerId={}, customerId={}", 
					manager.getId(), customerId, e);
			return false;
		}
	}

	/**
	 * トレーナーが顧客に割り当てられているか確認（存在確認専用クエリ）
	 * 
	 * <p>Service層で複合キー構造を知らないようにするため、Repositoryのメソッドを使用。</p>
	 * <p>以下の2つの条件のいずれかを満たす場合、トレーナーは顧客にアクセス可能:</p>
	 * <ul>
	 *   <li>user_customersテーブルにレコードが存在する（明示的な割り当て）</li>
	 *   <li>lessonsテーブルで、そのトレーナーがその顧客の次回レッスン希望日程の担当として設定されている（nextUser.id = trainerId）</li>
	 * </ul>
	 * 
	 * @param trainerId トレーナーID
	 * @param customerId 顧客ID
	 * @return 割り当てられている、または次回レッスン希望日程の担当として設定されている場合 true
	 */
	private boolean isTrainerAssignedToCustomer(UUID trainerId, UUID customerId) {
		if (trainerId == null) {
			log.warn("isTrainerAssignedToCustomer: trainerId is null");
			return false;
		}
		
		if (customerId == null) {
			log.warn("isTrainerAssignedToCustomer: customerId is null, trainerId={}", trainerId);
			return false;
		}
		
		try {
			// 1. user_customersテーブルで明示的な割り当てを確認
			boolean hasUserCustomerRecord = userCustomerRepository.existsByUserIdAndCustomerId(trainerId, customerId);
			
			if (hasUserCustomerRecord) {
				return true;
			}
			
			// 2. lessonsテーブルで次回レッスン希望日程の担当として設定されているかを確認
			boolean hasNextLessonRecord = lessonRepository.existsNextLessonByTrainerIdAndCustomerId(trainerId, customerId);
			
			if (hasNextLessonRecord) {
				return true;
			}
			
			log.warn("isTrainerAssignedToCustomer: No assignment or next lesson found - trainerId={}, customerId={}. Check if UserCustomer record or Lesson record with nextUser exists in database.", trainerId, customerId);
			return false;
		} catch (Exception e) {
			log.error("isTrainerAssignedToCustomer: Exception occurred - trainerId={}, customerId={}", trainerId, customerId, e);
			return false;
		}
	}

}
