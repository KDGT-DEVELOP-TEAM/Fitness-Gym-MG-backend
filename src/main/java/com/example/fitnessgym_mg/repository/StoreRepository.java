package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fitnessgym_mg.entity.Store;

public interface StoreRepository extends JpaRepository<Store, UUID> {

}
