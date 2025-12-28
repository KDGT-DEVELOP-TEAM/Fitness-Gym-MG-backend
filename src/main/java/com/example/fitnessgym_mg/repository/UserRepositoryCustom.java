package com.example.fitnessgym_mg.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.entity.enums.UserRole;

/**
 * Userリポジトリのカスタムインターフェース
 * 全文検索機能を提供
 */
public interface UserRepositoryCustom {
	
	/**
	 * 全文検索を使用したユーザー検索
	 * PostgreSQLのtsvectorを使用して高速検索を実現
	 * 
	 * @param keyword 検索キーワード（名前・かな）
	 * @param role ロール（nullの場合は全ロール）
	 * @param pageable ページネーション情報
	 * @return 検索結果のページ
	 */
	Page<User> searchByFullText(String keyword, UserRole role, Pageable pageable);
}

