package com.example.fitnessgym_mg.entity;

import java.io.Serializable;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User_Customers中間テーブルエンティティ
 * 
 * トレーナー（User）と顧客（Customer）の多対多の関係を表現する中間テーブル
 * 1人のトレーナーが複数の顧客を担当でき、1人の顧客が複数のトレーナーに担当される可能性がある
 */
@Entity
@Table(name = "user_customers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomer {

    /**
     * 複合主キー（user_idとcustomer_idの組み合わせ）
     */
    @EmbeddedId
    private UserCustomerId id;

    /**
     * トレーナー（User）への参照
     */
    @ManyToOne
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * 顧客（Customer）への参照
     */
    @ManyToOne
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    /**
     * 複合主キークラス
     * user_idとcustomer_idの組み合わせで主キーを構成
     */
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserCustomerId implements Serializable {
        
        @Column(name = "user_id")
        private UUID userId;
        
        @Column(name = "customer_id")
        private UUID customerId;
    }
}

