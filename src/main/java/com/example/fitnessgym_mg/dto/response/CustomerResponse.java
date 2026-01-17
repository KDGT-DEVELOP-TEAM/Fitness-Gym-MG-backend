package com.example.fitnessgym_mg.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.enums.Gender;

import lombok.Data;
import lombok.ToString;

/**
 * 顧客レスポンスDTO
 * 顧客情報をAPIレスポンスとして返す際に使用
 * 年齢は自動計算される
 * 
 * <p>セキュリティ: 個人情報（email、phone）と機密情報（medical、taboo、memo）はログ出力から除外します。</p>
 */
@Data
@ToString(exclude = {"email", "phone", "medical", "taboo", "memo"}) // セキュリティ: 個人情報と機密情報をログに出力しない
public class CustomerResponse {

	private UUID id;
	private String name;
	private String kana;
	private boolean active;
	private String email;
	private String phone;
	private int age; // 年齢
	private LocalDateTime createdAt;
	
	// プロフィール画面用の追加フィールド
	private Gender gender;
	/**
	 * 生年月日
	 * 
	 * <p>Entity（{@link com.example.fitnessgym_mg.entity.Customer#birthday}）の`birthday`フィールドに対応します。</p>
	 * <p>命名の不一致について: HTMLフォームとの互換性のため、DTOでは`birthdate`という名前を使用しています。</p>
	 * <p>意味は同じですが、フロントエンドの実装都合により異なる名前を使用しています。</p>
	 * <p>将来的には統一を検討する余地がありますが、現状はコメントで明確化されているため問題ありません。</p>
	 */
	private LocalDate birthdate;
	private String address;
	private BigDecimal height;
	/**
	 * 最新レッスンの体重（BMI計算用）
	 * 
	 * <p>注意: このフィールドは`fromEntity()`では設定されません。</p>
	 * <p>nullの可能性があります。フロントエンド側でnullチェックを実施してください。</p>
	 * <p>設定される場合: {@link com.example.fitnessgym_mg.service.CustomerService#getCustomerById(UUID)}で取得する場合のみ設定されます。</p>
	 * <p>設定されない場合: {@link com.example.fitnessgym_mg.service.CustomerService#searchCustomers(String, CustomerSort, UUID, Pageable)}や
	 * {@link com.example.fitnessgym_mg.service.CustomerService#getMyCustomers()}で取得する場合はnullのままです。</p>
	 */
	private BigDecimal latestWeight;
	private UUID firstPostureGroupId; // 初回姿勢画像ID
	
	/**
	 * 店舗ID
	 * 顧客が紐づく店舗のID（顧客は1つの店舗にのみ紐づく）
	 */
	private UUID storeId;
	
	/**
	 * 店舗名
	 * 顧客が紐づく店舗の名前（顧客は1つの店舗にのみ紐づく）
	 */
	private String storeName;
	
	/**
	 * 医療・既往歴（任意）
	 * 
	 * <p>Entity（{@link com.example.fitnessgym_mg.entity.Customer#medical}）の`medical`フィールドに対応します。</p>
	 * <p>null許容フィールドです。顧客の医療歴や既往歴を記録するための任意項目です。</p>
	 */
	private String medical;
	
	/**
	 * 禁忌事項（任意）
	 * 
	 * <p>Entity（{@link com.example.fitnessgym_mg.entity.Customer#taboo}）の`taboo`フィールドに対応します。</p>
	 * <p>null許容フィールドです。トレーニング時の禁忌事項を記録するための任意項目です。</p>
	 */
	private String taboo;
	
	/**
	 * メモ（任意）
	 * 
	 * <p>Entity（{@link com.example.fitnessgym_mg.entity.Customer#memo}）の`memo`フィールドに対応します。</p>
	 * <p>null許容フィールドです。顧客に関する自由記入のメモを記録するための任意項目です。</p>
	 */
	private String memo;

	/**
	 * Customer エンティティから CustomerResponse DTO に変換する
	 */
	public static CustomerResponse fromEntity(Customer c) {
		if (c == null) {
			return null;
		}
		
		// 年齢計算（nullチェック追加）
		int age = 0;
		if (c.getBirthday() != null) {
			age = Period.between(c.getBirthday(), LocalDate.now()).getYears();
		}

		CustomerResponse r = new CustomerResponse();
		r.setId(c.getId());
		r.setName(c.getName());
		r.setKana(c.getKana());
		r.setActive(c.isActive());
		r.setEmail(c.getEmail());
		r.setPhone(c.getPhone());
		r.setAge(age);
		r.setCreatedAt(c.getCreatedAt());
		r.setGender(c.getGender());
		r.setBirthdate(c.getBirthday()); // birthdayをbirthdateとして設定
		r.setAddress(c.getAddress());
		r.setHeight(c.getHeight());
		r.setFirstPostureGroupId(c.getFirstPostureGroupId());
		r.setMedical(c.getMedical());
		r.setTaboo(c.getTaboo());
		r.setMemo(c.getMemo());
		
		// 店舗情報を取得（顧客は1つの店舗にのみ紐づくため、最初の店舗を取得）
		if (c.getStores() != null && !c.getStores().isEmpty()) {
			com.example.fitnessgym_mg.entity.Store firstStore = c.getStores().iterator().next();
			r.setStoreId(firstStore.getId());
			r.setStoreName(firstStore.getName());
		}
		
		// latestWeightは別途設定が必要（レッスンから取得）
		return r;
	}
}