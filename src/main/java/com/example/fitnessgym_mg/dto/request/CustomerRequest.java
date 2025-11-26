package com.example.fitnessgym_mg.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 顧客情報リクエストDTO
 * 
 * フォームから送信された顧客情報を受け取るためのクラス
 * @Validアノテーションにより、Controllerでバリデーションが実行される
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRequest {

    @NotBlank(message = "名前は必須です")
    @Size(max = 50, message = "名前は50文字以内で入力してください")
    private String name;

    @Size(max = 50, message = "かなは50文字以内で入力してください")
    private String kana;

    @Email(message = "有効なメールアドレスを入力してください")
    @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
    private String email;

    @Size(max = 20, message = "電話番号は20文字以内で入力してください")
    private String phone;

    private LocalDate birthdate;

    @Positive(message = "身長は正の数である必要があります")
    private BigDecimal height;

    @Size(max = 10, message = "性別は10文字以内で入力してください")
    private String gender;

    @Size(max = 200, message = "住所は200文字以内で入力してください")
    private String address;
}

