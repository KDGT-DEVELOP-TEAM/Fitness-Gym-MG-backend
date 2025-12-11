package com.example.fitnessgym_mg.repository;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.fitnessgym_mg.entity.UserCustomer;

@Repository
public interface UserCustomerRepository extends JpaRepository<UserCustomer, UserCustomer.UserCustomerId> {

	List<UserCustomer> findByIdUserId(UUID userId);

	List<UserCustomer> findByIdCustomerId(UUID customerId);

	@Query("SELECT uc FROM UserCustomer uc JOIN FETCH uc.customer WHERE uc.id.userId = :userId ORDER BY uc.customer.kana")
	List<UserCustomer> findByUserIdwithCustomer(@Param("userId") UUID userId);
}