package com.example.fitnessgym_mg.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.User;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    
    /**
     * メールアドレスでユーザーを検索
     */
    Optional<User> findByEmail(String email);
    
    /**
     * メールアドレスでアクティブなユーザーを検索
     */
    Optional<User> findByEmailAndIsActiveTrue(String email);
}

