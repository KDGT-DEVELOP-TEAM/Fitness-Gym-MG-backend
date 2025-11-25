package com.example.fitnessgym_mg.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.repository.CustomerRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomerService {

	private final CustomerRepository customerRepository;

	public List<Customer> getRecentCustomers(int days) {
		LocalDateTime from = LocalDateTime.now().minusDays(days);
		LocalDateTime to = LocalDateTime.now();
		return customerRepository.findRecentCustomers(from, to);
	}
}