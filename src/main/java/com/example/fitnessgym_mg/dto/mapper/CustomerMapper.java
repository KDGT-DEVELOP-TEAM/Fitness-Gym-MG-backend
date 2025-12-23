package com.example.fitnessgym_mg.dto.mapper;

import java.util.UUID;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;

/**
 * CustomerDTO間の変換を行うMapperクラス
 * Request DTOとResponse DTO間の依存関係を解消するため、Mapperクラスに変換ロジックを集約
 */
public final class CustomerMapper {

    private CustomerMapper() {
        // インスタンス化を防ぐ
    }

    /**
     * CustomerResponseからCustomerRequestを作成（フォーム用）
     * 
     * @param response CustomerResponse
     * @param firstPostureGroupId 初回姿勢画像ID（編集時に使用、null可）
     * @return CustomerRequest
     */
    public static CustomerRequest fromResponse(CustomerResponse response, UUID firstPostureGroupId) {
        if (response == null) {
            return null;
        }
        
        CustomerRequest request = new CustomerRequest();
        request.setKana(response.getKana());
        request.setName(response.getName());
        request.setGender(response.getGender());
        request.setBirthday(response.getBirthdate());
        request.setHeight(response.getHeight());
        request.setEmail(response.getEmail());
        request.setPhone(response.getPhone());
        request.setAddress(response.getAddress());
        request.setActive(response.isActive());
        request.setFirstPostureGroupId(firstPostureGroupId != null ? firstPostureGroupId : response.getFirstPostureGroupId());
        
        return request;
    }
}
