package com.example.fitnessgym_mg.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import com.example.fitnessgym_mg.entity.Customer;

public interface CustomerRepository extends CrudRepository<Customer, UUID>, JpaSpecificationExecutor<Customer> {
}
