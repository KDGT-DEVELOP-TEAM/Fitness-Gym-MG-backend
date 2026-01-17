package com.example.fitnessgym_mg.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.example.fitnessgym_mg.entity.PasswordResetRequest;
import com.example.fitnessgym_mg.entity.enums.PasswordResetStatus;

import lombok.Data;

/**
 * パスワードリセットリクエストレスポンスDTO
 */
@Data
public class PasswordResetRequestResponse {

	private UUID id;
	private String email;
	private String name;
	private UUID userId;
	private String userName;
	private PasswordResetStatus status;
	private OffsetDateTime requestedAt;
	private OffsetDateTime processedAt;
	private UUID processedByUserId;
	private String processedByUserName;
	private String note;

	public static PasswordResetRequestResponse fromEntity(PasswordResetRequest request) {
		if (request == null) {
			return null;
		}

		PasswordResetRequestResponse response = new PasswordResetRequestResponse();
		response.setId(request.getId());
		response.setEmail(request.getEmail());
		response.setName(request.getName());
		response.setStatus(request.getStatus());
		response.setRequestedAt(request.getRequestedAt());
		response.setProcessedAt(request.getProcessedAt());
		response.setNote(request.getNote());

		if (request.getUser() != null) {
			response.setUserId(request.getUser().getId());
			response.setUserName(request.getUser().getName());
		}

		if (request.getProcessedBy() != null) {
			response.setProcessedByUserId(request.getProcessedBy().getId());
			response.setProcessedByUserName(request.getProcessedBy().getName());
		}

		return response;
	}
}
