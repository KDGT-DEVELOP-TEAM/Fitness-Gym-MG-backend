package com.example.fitnessgym_mg.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

	@Query("SELECT DISTINCT c FROM Customer c JOIN c.lessons l " +
			"WHERE l.reservationDate >= :from AND l.reservationDate <= :to")
	List<Customer> findRecentCustomers(LocalDateTime from, LocalDateTime to);
}