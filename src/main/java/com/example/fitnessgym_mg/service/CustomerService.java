package com.example.fitnessgym_mg.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional // トランザクション設定をクラスレベルに設定
public class CustomerService {

	private final CustomerRepository customerRepository;

	// --- keyword + sort の総合検索 ---
	@Transactional(readOnly = true)
	public List<CustomerResponse> searchCustomers(String keyword, String sort) {

		// JpaSpecificationExecutor の代わりに、ユーザー一覧に倣って全件取得後に Stream で処理
		List<Customer> customers = (List<Customer>) customerRepository.findAll();

		// --- キーワード（name + kana） ---
		if (keyword != null && !keyword.isEmpty()) {
			String lower = keyword.toLowerCase();

			customers = customers.stream()
					.filter(c -> c.getName().toLowerCase().contains(lower) ||
							c.getKana().toLowerCase().contains(lower))
					.toList();
		}

		// --- 並び順 ---
		switch (sort) {

		case "kana": // 50音順（kana を基準）
			customers = customers.stream()
					.sorted(Comparator.comparing(Customer::getKana))
					.toList();
			break;

		case "created": // 新着順 (created_at を基準)
		default:
			customers = customers.stream()
					.sorted(Comparator.comparing(Customer::getCreatedAt).reversed())
					.toList();
			break;
		}

		return customers.stream()
				.map(CustomerResponse::fromEntity)
				.toList();
	}

	// --- カスタマー作成 ---
	public void create(CustomerRequest req) {
		Customer customer = new Customer();

		// 必須項目
		customer.setKana(req.getKana());
		customer.setName(req.getName());
		customer.setGender(req.getGender());
		customer.setBirthday(req.getBirthday());
		customer.setHeight(req.getHeight());
		customer.setEmail(req.getEmail());
		customer.setPhone(req.getPhone());
		customer.setAddress(req.getAddress());

		// 任意項目
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());
		// first_posture_group_id は新規作成時は任意 (null許容)
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

		// システム設定
		customer.setActive(true); // is_active は新規作成時は有効 (true)

		customerRepository.save(customer);
	}

	// --- カスタマー更新 ---
	// 編集モーダルでは初回姿勢画像(first_posture_group_id)も必須としたい
	public void update(UUID id, CustomerRequest req) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Customer not found"));

		// 必須項目を更新
		customer.setKana(req.getKana());
		customer.setName(req.getName());
		customer.setGender(req.getGender());
		customer.setBirthday(req.getBirthday());
		customer.setHeight(req.getHeight());
		customer.setEmail(req.getEmail());
		customer.setPhone(req.getPhone());
		customer.setAddress(req.getAddress());

		// 任意項目を更新
		customer.setMedical(req.getMedical());
		customer.setTaboo(req.getTaboo());
		customer.setMemo(req.getMemo());

		// システム設定
		customer.setActive(req.isActive()); // 有効/無効状態も更新できる想定

		// 編集時の初回姿勢画像必須チェック（ビジネスロジック）
		if (req.getFirstPostureGroupId() == null) {
			throw new IllegalArgumentException("初回姿勢画像 (firstPostureGroupId) は編集時に必須です。");
		}
		customer.setFirstPostureGroupId(req.getFirstPostureGroupId());

		customerRepository.save(customer);
	}

	// --- 有効/無効切替 ---
	public void toggleActive(UUID id) {
		Customer customer = customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Customer not found"));

		customer.setActive(!customer.isActive());
		customerRepository.save(customer);
	}

	// --- 削除 ---
	public void delete(UUID id) {
		customerRepository.deleteById(id);
	}

	// IDでエンティティを取得する（編集モーダル初期表示用など）
	@Transactional(readOnly = true)
	public Customer findById(UUID id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Customer not found"));
	}
}