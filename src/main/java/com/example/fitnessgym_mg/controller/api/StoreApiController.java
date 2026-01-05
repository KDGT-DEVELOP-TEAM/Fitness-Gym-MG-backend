package com.example.fitnessgym_mg.controller.api;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.fitnessgym_mg.dto.response.StoreResponse;
import com.example.fitnessgym_mg.repository.StoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 店舗管理REST APIコントローラー
 * Reactフロントエンドとの連携用
 */
@Slf4j
@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class StoreApiController {

	private final StoreRepository storeRepository;

	/**
	 * GET /api/stores
	 * 店舗一覧取得
	 * 
	 * <p>全店舗の一覧を取得します。認証済みユーザーであれば誰でもアクセス可能です。</p>
	 * 
	 * @return 店舗一覧（StoreResponseのリスト）
	 */
	@GetMapping
	public ResponseEntity<List<StoreResponse>> getStores() {
		log.debug("店舗一覧取得リクエスト");
		
		List<StoreResponse> stores = storeRepository.findAll().stream()
				.map(StoreResponse::fromEntity)
				.collect(Collectors.toList());
		
		log.info("店舗一覧取得成功: count={}", stores.size());
		return ResponseEntity.ok(stores);
	}
}

