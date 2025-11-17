package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

	private final CustomerRepository customerRepository;

	// --- keyword + sort の総合検索 ---
	@Transactional(readOnly = true)
	public List<CustomerResponse> searchCustomers(String keyword, String sort) {

		// 1. ソートオブジェクトの生成
		Sort sortObj;
		switch (sort) {
		case "kana": // 50音順（kana を基準）
			sortObj = Sort.by("kana").ascending(); // 昇順
			break;
		case "created": // 新着順 (created_at を基準)
		default:
			sortObj = Sort.by("createdAt").descending(); // 降順
			break;
		}

		// 2. 検索メソッドの選択と実行
		List<Customer> customers;

		if (keyword != null && !keyword.isEmpty()) {
			// カスタム検索メソッドを使用
			customers = customerRepository.findByNameContainingIgnoreCaseOrKanaContainingIgnoreCase(
					keyword, keyword, sortObj);
		} else {
			// findAll(Sort) を使用
			customers = customerRepository.findAll(sortObj);
		}

		// 3. マッピング
		return customers.stream()
				.map(CustomerResponse::fromEntity)
				.toList();
	}

	// --- 顧客作成 ---
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

	// --- 顧客更新 ---
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
		Customer c = customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("User not found"));

		// 有効ユーザーは削除不可
		if (c.isActive()) {
			throw new IllegalStateException("有効ユーザーは削除できません");
		}

		customerRepository.delete(c);
	}

	// IDでエンティティを取得する（編集モーダル初期表示用など）
	@Transactional(readOnly = true)
	public Customer findById(UUID id) {
		return customerRepository.findById(id)
				.orElseThrow(() -> new RuntimeException("Customer not found"));
	}
}