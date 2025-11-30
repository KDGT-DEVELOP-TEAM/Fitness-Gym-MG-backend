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
     * JOIN FETCHを使ってCustomerエンティティも一緒に取得（N+1問題を防ぐ）
     */
    @Query("SELECT uc FROM UserCustomer uc JOIN FETCH uc.customer WHERE uc.id.userId = :userId ORDER BY uc.customer.kana")
    List<UserCustomer> findByUserIdWithCustomer(@Param("userId") UUID userId);
}

