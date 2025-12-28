package com.example.fitnessgym_mg.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.UserCustomer;

/**
 * User_Customers中間テーブル用のリポジトリインターフェース
 * 複合主キー（UserCustomerId）を使用
 */
@Repository
public interface UserCustomerRepository extends JpaRepository<UserCustomer, UserCustomer.UserCustomerId> {
    
    /**
     * ユーザーIDに紐づく顧客関連リストを取得
     */
    List<UserCustomer> findByIdUserId(UUID userId);
    
    /**
     * 顧客IDに紐づくユーザー関連リストを取得
     */
    List<UserCustomer> findByIdCustomerId(UUID customerId);
    
    /**
     * ユーザーIDから顧客リストを取得（結合クエリ）
     * 
     * <p>JOIN FETCHを使ってCustomerエンティティも一緒に取得（N+1問題を防ぐ）</p>
     * 
     * <p>設計方針:</p>
     * <ul>
     *   <li>論理削除された顧客（deletedAt IS NULL）と無効（active = true）の顧客は除外されます。</li>
     *   <li>UserCustomer経由ではactiveな顧客しか扱わないことを前提としています。</li>
     *   <li>この仕様は、CustomerRepositoryのnotDeleted()とは別の設計判断です。</li>
     * </ul>
     * 
     * <p>注意: 将来コレクションFETCH（例: customer.stores）を追加する場合は、
     * FETCH JOIN + ORDER BYの組み合わせによる重複や順序不定の問題に注意してください。</p>
     * 
     * @param userId ユーザーID
     * @return 顧客リスト（かな順）
     */
    @Query("""
            SELECT uc 
            FROM UserCustomer uc 
            JOIN FETCH uc.customer 
            WHERE uc.id.userId = :userId 
              AND uc.customer.deletedAt IS NULL
              AND uc.customer.active = true
            ORDER BY uc.customer.kana
            """)
    List<UserCustomer> findByUserIdWithCustomer(@Param("userId") UUID userId);
    
    /**
     * トレーナーと顧客の関連が存在するか確認
     * JpaRepositoryから継承されたメソッドを明示的にドキュメント化
     * 
     * @param id 複合主キー（UserCustomerId）
     * @return 関連が存在する場合 true
     */
    boolean existsById(UserCustomer.UserCustomerId id);
    
    /**
     * ユーザーと顧客の関連が存在するか確認
     * 
     * <p>Service層で複合キー構造を知らないようにするため、UUIDを直接受け取る。</p>
     * 
     * <p>EXISTSを使用することで、COUNTベースのクエリよりも効率的に動作します。
     * 早期停止が可能になり、パフォーマンスが向上します。</p>
     * 
     * @param userId ユーザーID
     * @param customerId 顧客ID
     * @return 関連が存在する場合 true
     */
    @Query("""
        SELECT CASE WHEN EXISTS (
            SELECT 1
            FROM UserCustomer uc
            WHERE uc.id.userId = :userId
              AND uc.id.customerId = :customerId
        ) THEN true ELSE false END
        """)
    boolean existsByUserIdAndCustomerId(
        @Param("userId") UUID userId,
        @Param("customerId") UUID customerId
    );
}

