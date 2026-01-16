package com.example.fitnessgym_mg.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.CustomerRepositoryCustom;
import com.example.fitnessgym_mg.repository.LessonRepository;
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

		// MANAGER: 全店舗の顧客にアクセス可能（トレーナーと同様のロジック）
		// 顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能
		if (currentUser.getRole() == UserRole.MANAGER) {
			boolean result = canManagerAccessCustomer(currentUser, customerId);
			log.debug("Authorization check: MANAGER access={} for customerId={}", result, customerId);
			return result;
		}

		// TRAINER: 所属店舗のすべての顧客にアクセス可能
		// トレーナーと顧客が同じ店舗に所属しているかチェック
		if (currentUser.getRole() == UserRole.TRAINER) {
			boolean result = canTrainerAccessCustomerByStore(currentUser, customerId);
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
	 * <p>マネージャーは全店舗の顧客にアクセス可能（トレーナーと同様のロジック）</p>
	 * <p>顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能</p>
	 * 
	 * @param manager マネージャー
	 * @param customerId 顧客ID
	 * @return アクセス可能な場合 true
	 */
	private boolean canManagerAccessCustomer(User manager, UUID customerId) {
		// マネージャーは全店舗の顧客にアクセス可能（トレーナーと同様のロジック）
		// 顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能
		try {
			if (customerRepository instanceof CustomerRepositoryCustom) {
				// 顧客が存在し、論理削除されていない、かつ有効かを確認
				java.util.Optional<Customer> customerOpt = ((CustomerRepositoryCustom) customerRepository)
						.findByIdWithStoresNative(customerId);
				
				if (customerOpt.isEmpty()) {
					log.debug("canManagerAccessCustomer: customer not found - managerId={}, customerId={}", 
							manager.getId(), customerId);
					return false;
				}
				
				Customer customer = customerOpt.get();
				// 顧客が有効で、論理削除されていない場合にアクセス可能
				boolean result = customer.isActive() && !customer.isDeleted();
				log.debug("canManagerAccessCustomer: managerId={}, customerId={}, active={}, deleted={}, result={}", 
						manager.getId(), customerId, customer.isActive(), customer.isDeleted(), result);
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
	 * トレーナーが店舗ベースで顧客にアクセス可能か確認
	 * トレーナーにはuser_storesテーブルにレコードがないため、店舗チェックをスキップ
	 * トレーナーは全顧客にアクセス可能（顧客一覧と同様のロジック）
	 * 
	 * @param trainer トレーナー
	 * @param customerId 顧客ID
	 * @return アクセス可能な場合 true
	 */
	private boolean canTrainerAccessCustomerByStore(User trainer, UUID customerId) {
		// トレーナーにはuser_storesテーブルにレコードがないため、店舗チェックをスキップ
		// トレーナーは全顧客にアクセス可能（顧客一覧と同様のロジック）
		// 顧客が存在し、論理削除されていない、かつ有効な場合にアクセス可能
		try {
			if (customerRepository instanceof CustomerRepositoryCustom) {
				// 顧客が存在し、論理削除されていない、かつ有効かを確認
				java.util.Optional<Customer> customerOpt = ((CustomerRepositoryCustom) customerRepository)
						.findByIdWithStoresNative(customerId);
				
				if (customerOpt.isEmpty()) {
					log.debug("canTrainerAccessCustomerByStore: customer not found - trainerId={}, customerId={}", 
							trainer.getId(), customerId);
					return false;
				}
				
				Customer customer = customerOpt.get();
				// 顧客が有効で、論理削除されていない場合にアクセス可能
				boolean result = customer.isActive() && !customer.isDeleted();
				log.debug("canTrainerAccessCustomerByStore: trainerId={}, customerId={}, active={}, deleted={}, result={}", 
						trainer.getId(), customerId, customer.isActive(), customer.isDeleted(), result);
				return result;
			} else {
				log.error("CustomerRepository does not implement CustomerRepositoryCustom");
				return false;
			}
		} catch (Exception e) {
			log.error("canTrainerAccessCustomerByStore failed: trainerId={}, customerId={}", 
					trainer.getId(), customerId, e);
			return false;
		}
	}

}
