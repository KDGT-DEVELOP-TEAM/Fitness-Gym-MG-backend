package com.example.fitnessgym_mg.repository;

import org.springframework.data.repository.CrudRepository;
import com.example.fitnessgym_mg.entity.Customer;

public interface CustomerRepository extends CrudRepository<Customer, Long> {
}

