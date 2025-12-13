package com.example.fitnessgym_mg.config.security.exception;

public class RedirectException extends RuntimeException {
	public RedirectException(String message) {
		super(message);
	}
}