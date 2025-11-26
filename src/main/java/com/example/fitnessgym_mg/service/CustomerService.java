package com.example.fitnessgym_mg.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.Customer;
import com.example.fitnessgym_mg.entity.UserCustomer;
import com.example.fitnessgym_mg.repository.CustomerRepository;
import com.example.fitnessgym_mg.repository.UserCustomerRepository;

import lombok.RequiredArgsConstructor;

/**
 * 顧客管理サービス
 * トレーナーが担当している顧客リストの取得、顧客詳細情報の取得、顧客情報の更新を担当
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserCustomerRepository userCustomerRepository;

    /**
     * トレーナーIDから担当顧客リストを取得
     * 
     * 処理の流れ：
     * 1. トレーナーが担当している顧客との関連を取得
     * 2. 各関連からCustomerエンティティを取得
     * 3. CustomerエンティティをCustomerResponse（DTO）に変換
     * 4. CustomerResponseのリストを返す
     */
    public List<CustomerResponse> getCustomersByTrainer(UUID trainerId) {
        // トレーナーが担当している顧客との関連を取得
        List<UserCustomer> userCustomers = userCustomerRepository.findByUserIdWithCustomer(trainerId);
        
        // CustomerエンティティをCustomerResponseに変換
        return userCustomers.stream()
                .map(uc -> toCustomerResponse(uc.getCustomer()))
                .collect(Collectors.toList());
    }

    /**
     * 顧客IDから顧客詳細を取得
     * 
     * 処理の流れ：
     * 1. IDで顧客を検索
     * 2. CustomerエンティティをCustomerResponse（DTO）に変換して返す
     */
    public CustomerResponse getCustomerById(UUID customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("顧客が見つかりません: " + customerId));
        
        return toCustomerResponse(customer);
    }

    /**
     * 顧客情報を更新
     * 
     * 処理の流れ：
     * 1. IDで顧客を検索
     * 2. CustomerRequest（DTO）の値で顧客情報を更新
     * 3. 更新された顧客情報をデータベースに保存
     * 4. 更新された顧客情報をCustomerResponse（DTO）に変換して返す
     */
    @Transactional
    public CustomerResponse updateCustomer(UUID customerId, CustomerRequest request) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("顧客が見つかりません: " + customerId));
        
        // CustomerRequest（DTO）の値で顧客情報を更新
        customer.setName(request.getName());
        customer.setKana(request.getKana());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setBirthdate(request.getBirthdate());
        customer.setHeight(request.getHeight());
        customer.setGender(request.getGender());
        customer.setAddress(request.getAddress());
        
        Customer updatedCustomer = customerRepository.save(customer);
        return toCustomerResponse(updatedCustomer);
    }

    /**
     * CustomerエンティティをCustomerResponse（DTO）に変換
     */
    private CustomerResponse toCustomerResponse(Customer customer) {
        return CustomerResponse.builder()
                .id(customer.getId())
                .name(customer.getName())
                .kana(customer.getKana())
                .email(customer.getEmail())
                .phone(customer.getPhone())
                .birthdate(customer.getBirthdate())
                .height(customer.getHeight())
                .gender(customer.getGender())
                .address(customer.getAddress())
                .build();
    }
}

