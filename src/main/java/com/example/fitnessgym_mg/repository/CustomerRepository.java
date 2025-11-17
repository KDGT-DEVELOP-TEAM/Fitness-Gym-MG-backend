package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.fitnessgym_mg.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}

