package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Store;

/**
 * 店舗エンティティ用リポジトリ
 * 店舗の基本的なCRUD操作を提供
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

}
