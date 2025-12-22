package com.example.fitnessgym_mg.controller.web;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import com.example.fitnessgym_mg.controller.util.ControllerModelUtils;
import com.example.fitnessgym_mg.controller.util.ControllerPathUtils;
import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.PostureGroup;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.exception.AuthenticationException;
import com.example.fitnessgym_mg.exception.EntityNotFoundException;
import com.example.fitnessgym_mg.service.CustomerService;
import com.example.fitnessgym_mg.service.PostureGroupService;
import com.example.fitnessgym_mg.util.SecurityUtil;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 顧客管理画面コントローラー
 * 顧客選択、顧客プロフィール表示・編集を提供
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final PostureGroupService postureGroupService;
    private final SecurityUtil securityUtil;

    /**
     * GET /trainer/customers
     * トレーナー用顧客選択画面
     * 
     * 処理の流れ：
     * 1. ログイン中のトレーナー情報を取得
     * 2. そのトレーナーが担当している顧客リストを取得
     * 3. 検索キーワードがある場合は、名前またはかなでフィルタリング
     * 4. 画面に表示するデータをModelに設定して返す
     */
    @GetMapping("/trainer/customers")
    public String trainerCustomerList(
            @RequestParam(required = false) String search,
            Model model) {
        
        // ログインユーザー（トレーナー）を取得
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        // 担当顧客リストを取得
        List<CustomerResponse> customers = customerService.getCustomersByTrainer(currentUser.getId());
        
        // 検索フィルタリング（名前またはかな）
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase();
            customers = customers.stream()
                    .filter(c -> c.getName().toLowerCase().contains(searchLower) 
                            || (c.getKana() != null && c.getKana().toLowerCase().contains(searchLower)))
                    .toList();
        }
        
        model.addAttribute("customers", customers);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("search", search);
        
        return "customer/customer-select";
    }

    /**
     * GET /customer/{customerId}
     * GET /admin/customers/{customerId}
     * GET /manager/{storeId}/customers/{customerId}
     * 顧客プロフィール表示
     * 
     * 処理の流れ：
     * 1. 顧客IDで顧客詳細情報を取得
     * 2. ログイン中のユーザー情報を取得
     * 3. ユーザーの権限を確認して編集可能かどうかを判定（管理者/店長のみ編集可能）
     * 4. 画面に表示するデータをModelに設定して返す
     * 5. エラーが発生した場合は、エラーメッセージを設定して顧客選択画面にリダイレクト
     */
    @GetMapping({"/customer/{customerId}", "/admin/customers/{customerId}", "/manager/{storeId}/customers/{customerId}"})
    public String customerProfile(
            @PathVariable(required = false) UUID storeId,
            @PathVariable UUID customerId,
            HttpServletRequest request,
            Model model) {
        
        try {
            // 顧客詳細を取得（firstPostureGroupIdも含まれる）
            CustomerResponse customerResponse = customerService.getCustomerById(customerId);
            User currentUser = securityUtil.getCurrentUserOrThrow();
            
            // 編集可能かどうかを判定（管理者/店長のみ）
            boolean canEdit = securityUtil.hasRole("ROLE_ADMIN") || securityUtil.hasRole("ROLE_MANAGER");
            
            // ユーザータイプを判定（共通ユーティリティを使用）
            String requestPath = request.getRequestURI();
            ControllerPathUtils.UserTypeInfo userTypeInfo = ControllerPathUtils.determineUserTypeFromPath(requestPath);
            ControllerModelUtils.setUserTypeFlags(model, userTypeInfo.isAdmin(), userTypeInfo.isManager(), userTypeInfo.isTrainer());
            
            // 姿勢画像グループ一覧を取得
            List<PostureGroup> postureGroups = postureGroupService.findByCustomerId(customerId);
            
            // CustomerResponseからCustomerRequestを作成（フォーム用）
            CustomerRequest customerRequest = CustomerRequest.fromResponse(customerResponse, customerResponse.getFirstPostureGroupId());
            
            model.addAttribute("customer", customerResponse); // 表示用
            model.addAttribute("customerRequest", customerRequest); // フォーム用
            model.addAttribute("postureGroups", postureGroups); // 姿勢画像グループ一覧
            model.addAttribute("storeId", storeId);
            model.addAttribute("canEdit", canEdit);
            model.addAttribute("currentUser", currentUser);
            model.addAttribute("customerId", customerId);
            
            return "customer/customer-profile";
            
        } catch (EntityNotFoundException e) {
            log.warn("顧客情報の取得に失敗しました: customerId={}, error={}", customerId, e.getMessage());
            model.addAttribute("errorMessage", "顧客情報の取得に失敗しました: " + e.getMessage());
            return "redirect:/trainer/customers";
        } catch (AuthenticationException e) {
            log.warn("認証エラーが発生しました: customerId={}, error={}", customerId, e.getMessage());
            model.addAttribute("errorMessage", "認証エラーが発生しました: " + e.getMessage());
            return "redirect:/trainer/customers";
        } catch (Exception e) {
            log.error("予期しないエラーが発生しました: customerId={}, error={}", customerId, e.getMessage(), e);
            model.addAttribute("errorMessage", "顧客情報の取得に失敗しました: " + e.getMessage());
            return "redirect:/trainer/customers";
        }
    }

    /**
     * POST /admin/customers/{customerId}
     * POST /manager/{storeId}/customers/{customerId}
     * 顧客情報更新
     * 
     * 処理の流れ：
     * 1. フォームから送信されたデータをバリデーション（@Valid）
     * 2. バリデーションエラーがある場合、エラーメッセージと共にフォーム画面に戻る
     * 3. バリデーションが成功した場合、顧客情報を更新
     * 4. 更新成功後、成功メッセージを設定してプロフィール画面にリダイレクト
     * 5. エラーが発生した場合、エラーメッセージと共にフォーム画面に戻る
     */
    @PostMapping({"/admin/customers/{customerId}", "/manager/{storeId}/customers/{customerId}"})
    public String updateCustomer(
            @PathVariable(required = false) UUID storeId,
            @PathVariable UUID customerId,
            @Valid @ModelAttribute CustomerRequest request,
            BindingResult result,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // バリデーションエラーがある場合
        if (result.hasErrors()) {
            prepareCustomerProfileModel(customerId, storeId, request.getFirstPostureGroupId(), model, null);
            return "customer/customer-profile";
        }
        
        try {
            // 顧客情報を更新
            customerService.update(customerId, request, storeId);
            
            log.info("顧客情報を更新しました: customerId={}, storeId={}", customerId, storeId);
            
            // 成功メッセージ
            redirectAttributes.addFlashAttribute("successMessage", "顧客情報を更新しました");
            
            // プロフィール画面にリダイレクト
            if (storeId != null) {
                return "redirect:/manager/" + storeId + "/customers/" + customerId;
            } else {
                return "redirect:/admin/customers/" + customerId;
            }
            
        } catch (EntityNotFoundException e) {
            log.warn("顧客情報の更新に失敗しました: customerId={}, storeId={}, error={}", customerId, storeId, e.getMessage());
            prepareCustomerProfileModel(customerId, storeId, request.getFirstPostureGroupId(), model, "顧客情報の更新に失敗しました: " + e.getMessage());
            return "customer/customer-profile";
        } catch (IllegalArgumentException e) {
            log.warn("バリデーションエラー: customerId={}, storeId={}, error={}", customerId, storeId, e.getMessage());
            prepareCustomerProfileModel(customerId, storeId, request.getFirstPostureGroupId(), model, "入力値が不正です: " + e.getMessage());
            return "customer/customer-profile";
        } catch (Exception e) {
            log.error("予期しないエラーが発生しました: customerId={}, storeId={}, error={}", customerId, storeId, e.getMessage(), e);
            prepareCustomerProfileModel(customerId, storeId, request.getFirstPostureGroupId(), model, "顧客情報の更新に失敗しました: " + e.getMessage());
            return "customer/customer-profile";
        }
    }

    /**
     * 顧客プロフィール画面表示用のModelを準備する共通メソッド
     */
    private void prepareCustomerProfileModel(UUID customerId, UUID storeId, UUID firstPostureGroupId, Model model, String errorMessage) {
        CustomerResponse customer = customerService.getCustomerById(customerId);
        User currentUser = securityUtil.getCurrentUserOrThrow();
        
        // ユーザータイプを判定（securityUtilを使用、リクエストパスが利用できないため）
        boolean isAdmin = securityUtil.hasRole("ROLE_ADMIN");
        boolean isManager = securityUtil.hasRole("ROLE_MANAGER");
        boolean isTrainer = !isAdmin && !isManager;
        ControllerModelUtils.setUserTypeFlags(model, isAdmin, isManager, isTrainer);
        
        // 姿勢画像グループ一覧を取得
        List<PostureGroup> postureGroups = postureGroupService.findByCustomerId(customerId);
        
        // CustomerResponseからCustomerRequestを作成（フォーム用）
        CustomerRequest customerRequest = CustomerRequest.fromResponse(customer, firstPostureGroupId);
        
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        model.addAttribute("customer", customer);
        model.addAttribute("customerRequest", customerRequest);
        model.addAttribute("postureGroups", postureGroups);
        model.addAttribute("storeId", storeId);
        model.addAttribute("canEdit", true);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("customerId", customerId);
    }

}

