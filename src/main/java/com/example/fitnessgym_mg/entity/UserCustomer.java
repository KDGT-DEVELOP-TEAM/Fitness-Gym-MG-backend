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
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_customer")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCustomer {

	@EmbeddedId
	private UserCustomerId id;

	@ManyToOne
	@JoinColumn(name = "user_id", insertable = false, updatable = false)
	private User user;

	@ManyToOne
	@JoinColumn(name = "customer_id", insertable = false, updatable = false)
	private Customer customer;

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
