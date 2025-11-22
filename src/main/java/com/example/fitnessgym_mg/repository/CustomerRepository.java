package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
}

