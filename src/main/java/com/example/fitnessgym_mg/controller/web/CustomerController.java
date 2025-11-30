package com.example.fitnessgym_mg.controller.web;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.fitnessgym_mg.dto.request.CustomerRequest;
import com.example.fitnessgym_mg.dto.response.CustomerResponse;
import com.example.fitnessgym_mg.entity.User;
import com.example.fitnessgym_mg.repository.UserRepository;
import com.example.fitnessgym_mg.service.CustomerService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 顧客管理画面コントローラー
 * 顧客選択、顧客プロフィール表示・編集を提供
 */
@Controller
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserRepository userRepository;

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
        User currentUser = getCurrentUser();
        
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
     * GET /admin/customers/{customerId}
     * GET /manager/{storeId}/customers/{customerId}
     * GET /trainer/customers/{customerId}
     * 顧客プロフィール表示
     * 
     * 処理の流れ：
     * 1. 顧客IDで顧客詳細情報を取得
     * 2. ログイン中のユーザー情報を取得
     * 3. ユーザーの権限を確認して編集可能かどうかを判定（管理者/店長のみ編集可能）
     * 4. 画面に表示するデータをModelに設定して返す
     * 5. エラーが発生した場合は、エラーメッセージを設定して顧客選択画面にリダイレクト
     */
    @GetMapping({"/admin/customers/{customerId}", "/manager/{storeId}/customers/{customerId}", "/trainer/customers/{customerId}"})
    public String customerProfile(
            @PathVariable(required = false) UUID storeId,
            @PathVariable UUID customerId,
            Model model) {
        
        try {
            // 顧客詳細を取得
            CustomerResponse customer = customerService.getCustomerById(customerId);
            User currentUser = getCurrentUser();
            
            // 編集可能かどうかを判定（管理者/店長のみ）
            boolean canEdit = hasRole("ROLE_ADMIN") || hasRole("ROLE_MANAGER");
            
            model.addAttribute("customer", customer);
            model.addAttribute("storeId", storeId);
            model.addAttribute("canEdit", canEdit);
            model.addAttribute("currentUser", currentUser);
            
            return "customer/customer-profile";
            
        } catch (RuntimeException e) {
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
            CustomerResponse customer = customerService.getCustomerById(customerId);
            model.addAttribute("customer", customer);
            model.addAttribute("storeId", storeId);
            model.addAttribute("canEdit", true);
            return "customer/customer-profile";
        }
        
        try {
            // 顧客情報を更新
            customerService.update(customerId, request, storeId);
            
            // 成功メッセージ
            redirectAttributes.addFlashAttribute("successMessage", "顧客情報を更新しました");
            
            // プロフィール画面にリダイレクト
            if (storeId != null) {
                return "redirect:/manager/" + storeId + "/customers/" + customerId;
            } else {
                return "redirect:/admin/customers/" + customerId;
            }
            
        } catch (Exception e) {
            model.addAttribute("errorMessage", "顧客情報の更新に失敗しました: " + e.getMessage());
            CustomerResponse customer = customerService.getCustomerById(customerId);
            model.addAttribute("customer", customer);
            model.addAttribute("storeId", storeId);
            model.addAttribute("canEdit", true);
            return "customer/customer-profile";
        }
    }

    /**
     * 現在ログイン中のユーザーを取得
     * 
     * 処理の流れ：
     * 1. Spring Securityのセキュリティコンテキストから認証情報を取得
     * 2. 認証情報からメールアドレスを取得
     * 3. メールアドレスでユーザーを検索して返す
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("ログインユーザーが見つかりません"));
    }

    /**
     * 現在のユーザーが指定されたロールを持っているか確認
     * 
     * 処理の流れ：
     * 1. Spring Securityのセキュリティコンテキストから認証情報を取得
     * 2. 認証情報から権限のリストを取得
     * 3. 指定されたロールが権限リストに含まれているか確認
     */
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals(role));
    }
}

