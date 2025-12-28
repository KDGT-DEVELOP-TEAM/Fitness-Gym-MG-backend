package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Store;

/**
 * 店舗エンティティ用リポジトリ
 * 
 * <p>店舗の基本的なCRUD操作を提供します。</p>
 * 
 * <p>設計方針:</p>
 * <ul>
 *   <li>Storeは論理削除を行わない前提のマスタエンティティです。</li>
 *   <li>現時点では物理削除を前提としており、deletedAtフィールドは実装されていません。</li>
 *   <li>将来的に論理削除が必要になった場合は、StoreエンティティにdeletedAtフィールドを追加し、
 *       このRepositoryにも論理削除条件を適用する必要があります。</li>
 * </ul>
 * 
 * <p>注意: 認可ロジックでUser.storesを参照しているため、
 * 論理削除を導入する場合は、認可判定への影響を十分に検討してください。</p>
 */
@Repository
public interface StoreRepository extends JpaRepository<Store, UUID> {

}
