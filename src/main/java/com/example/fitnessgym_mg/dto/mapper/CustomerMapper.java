package com.example.fitnessgym_mg.dto.mapper;

import java.util.UUID;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;

/**
 * CustomerDTO間の変換を行うMapperクラス（編集フォーム初期化専用）
 * 
 * <p>このMapperは編集フォームの初期化専用であり、プロジェクト全体のMapperパターンとは異なります。</p>
 * 
 * <p>プロジェクト全体のMapperパターン:</p>
 * <ul>
 *   <li>Response DTO: {@code fromEntity()} メソッドでEntity→Response変換を提供</li>
 *   <li>Request DTO: Service層で直接Entityに変換（Mapper不要）</li>
 * </ul>
 * 
 * <p>このMapperの用途:</p>
 * <ul>
 *   <li>編集フォームの初期化時に、既存のCustomerResponseからCustomerRequestを作成</li>
 *   <li>Response→Requestの変換のみを提供（片方向）</li>
 * </ul>
 * 
 * <p>注意事項:</p>
 * <ul>
 *   <li>ビジネスロジック（優先順位の決定など）は含めない</li>
 *   <li>呼び出し側で必要な値の決定を行い、その値をそのまま設定する</li>
 *   <li>将来的に他の変換が必要になった場合は、別のMapperクラスを検討すること</li>
 * </ul>
 */
public final class CustomerMapper {

    private CustomerMapper() {
        // インスタンス化を防ぐ
    }

    /**
     * CustomerResponseからCustomerRequestを作成（編集フォーム初期化専用）
     * 
     * <p>このメソッドは編集フォームの初期化時に使用します。
     * 既存のCustomerResponseからCustomerRequestを作成し、フォームの初期値として使用します。</p>
     * 
     * <p>ビジネスロジックの注意:</p>
     * <ul>
     *   <li>このメソッドはデータ変換のみを行います</li>
     *   <li>優先順位の決定などは呼び出し側で行ってください</li>
     *   <li>例: {@code UUID id = param != null ? param : response.getId();}</li>
     * </ul>
     * 
     * <p>使用例:</p>
     * <pre>{@code
     * // 呼び出し側で優先順位を決定
     * UUID postureGroupId = newFirstPostureGroupId != null 
     *     ? newFirstPostureGroupId 
     *     : response.getFirstPostureGroupId();
     * 
     * // Mapperは単純にデータを詰め替えるだけ
     * CustomerRequest request = CustomerMapper.fromResponse(response, postureGroupId);
     * }</pre>
     * 
     * @param response CustomerResponse（nullの場合はnullを返す）
     * @param firstPostureGroupId 初回姿勢画像ID（null可、そのまま設定される）
     * @return CustomerRequest（responseがnullの場合はnull）
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
        request.setMedical(response.getMedical());
        request.setTaboo(response.getTaboo());
        request.setMemo(response.getMemo());
        
        // ビジネスロジックを排除: 受け取った値をそのまま設定
        // 優先順位の決定は呼び出し側で行うこと
        request.setFirstPostureGroupId(firstPostureGroupId);
        
        return request;
    }
}
