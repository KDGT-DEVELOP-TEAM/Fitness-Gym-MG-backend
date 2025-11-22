# 実装計画：ログイン・顧客選択・履歴画面機能

## 概要
ユースケース UC-01（ログイン）、UCTR-01（顧客選択）、UC-02（履歴画面）に基づいた実装計画

---

## 1. ログイン機能 (UC-01)

### 変更するファイル

#### 1.1 AuthController.java
**パス**: `src/main/java/com/example/fitnessgym_mg/controller/AuthController.java`
**変更内容**: `@RestController` を `@Controller` に変更し、以下のメソッドを実装
- `GET /auth/login`: ログインページ表示
- `POST /auth/login`: ログイン処理（認証成功後、権限に応じてリダイレクト）
  - トレーナー → `/customers/selection`（顧客選択画面）
  - 店長・本部管理者 → `/statistics`（統計情報画面）
- `POST /auth/logout`: ログアウト処理

**実装ポイント**:
- セッション管理（`HttpSession`）を使用
- パスワード検証（BCrypt）
- エラーメッセージ表示
- 無効アカウントチェック

#### 1.2 AuthService.java
**パス**: `src/main/java/com/example/fitnessgym_mg/service/AuthService.java`
**変更内容**: 認証ロジックを実装
- `authenticate(String email, String password)`: メールアドレスとパスワードで認証
- `UserRepository` と `BCryptPasswordEncoder` を使用

#### 1.3 LoginRequest.java
**パス**: `src/main/java/com/example/fitnessgym_mg/dto/request/LoginRequest.java`
**変更内容**: DTOフィールドを追加
- `email`: String（メールアドレス）
- `password`: String（パスワード）
- バリデーション（`@NotBlank`など）

#### 1.4 login.html
**パス**: `src/main/resources/templates/auth/login.html`
**変更内容**: ログインフォームを実装
- メールアドレス入力欄
- パスワード入力欄
- ログインボタン
- 「パスワードを忘れた」ボタン（将来実装用）
- エラーメッセージ表示エリア

#### 1.5 SecurityConfig.java
**パス**: `src/main/java/com/example/fitnessgym_mg/config/SecurityConfig.java`
**変更内容**: セッションベースの認証設定を追加
- `PasswordEncoder` Bean定義
- ログインページのパス設定
- セッション管理設定

#### 1.6 WebConfig.java
**パス**: `src/main/java/com/example/fitnessgym_mg/config/WebConfig.java`
**変更内容**: ルートパス `/` を `/auth/login` にリダイレクト

---

## 2. 顧客選択機能 (UCTR-01)

### 変更するファイル

#### 2.1 CustomerController.java
**パス**: `src/main/java/com/example/fitnessgym_mg/controller/CustomerController.java`
**変更内容**: `@RestController` を `@Controller` に変更し、以下のメソッドを実装
- `GET /customers/selection`: 顧客選択画面表示
  - セッションからトレーナーIDを取得
  - デフォルト：直近一週間の担当顧客を表示
  - 「もっと見る」ボタンで全顧客を表示
- `GET /api/customers/selection`: 顧客一覧取得API（JSON）
  - クエリパラメータ: `period`（`week` or `all`）
  - ページネーション対応（将来拡張）

#### 2.2 CustomerService.java
**パス**: `src/main/java/com/example/fitnessgym_mg/service/CustomerService.java`
**変更内容**: 担当顧客取得ロジックを実装
- `findAssignedCustomersByTrainer(UUID trainerId, String period)`: トレーナーの担当顧客を取得
  - `period="week"`: 直近一週間の予約がある顧客
  - `period="all"`: 全担当顧客
- `toResponse(Customer customer)`: `CustomerResponse` に変換
  - 最新の予約日時を含める

#### 2.3 CustomerResponse.java
**パス**: `src/main/java/com/example/fitnessgym_mg/dto/response/CustomerResponse.java`
**変更内容**: DTOフィールドを追加
- `id`: UUID
- `name`: String（氏名）
- `kana`: String（フリガナ）
- `nextLessonDate`: OffsetDateTime（最新の予約日時）
- その他の顧客情報（必要に応じて）

#### 2.4 CustomerRepository.java
**パス**: `src/main/java/com/example/fitnessgym_mg/repository/CustomerRepository.java`
**変更内容**: クエリメソッドを追加
- `findByTrainerIdAndRecentReservations(UUID trainerId, OffsetDateTime startDate)`: 直近一週間の予約がある顧客を取得
- `findByTrainerId(UUID trainerId)`: 全担当顧客を取得
- `@Query` を使用して `lessons` テーブルとJOIN

#### 2.5 customer-selection.html
**パス**: `src/main/resources/templates/customer/customer-selection.html`
**変更内容**: 顧客選択画面テンプレートを新規作成
- 顧客一覧表示
- 顧客名と予約日時を表示
- 「もっと見る」ボタン
- 顧客選択時に `/customers/{customerId}/history` に遷移

---

## 3. 履歴画面機能 (UC-02)

### 変更するファイル

#### 3.1 LessonController.java（または HistoryController.java を新規作成）
**パス**: `src/main/java/com/example/fitnessgym_mg/controller/LessonController.java`
**変更内容**: `@RestController` を `@Controller` に変更し、以下のメソッドを実装
- `GET /customers/{customerId}/history`: 履歴画面表示
- `GET /api/customers/{customerId}/lessons`: レッスン履歴取得API（JSON）
  - クエリパラメータ: `months`（デフォルト: 1）
  - ページネーション（10件以上の場合）

**代替案**: `HistoryController.java` を新規作成して履歴画面専用にする

#### 3.2 LessonService.java
**パス**: `src/main/java/com/example/fitnessgym_mg/service/LessonService.java`
**変更内容**: レッスン履歴取得ロジックを実装
- `findHistoryByCustomerId(UUID customerId, int months)`: 指定月数のレッスン履歴を取得
  - 体重・BMIデータを含む
  - 開始日時の降順でソート
- `calculateBMI(BigDecimal weight, BigDecimal height)`: BMI計算
- `toResponse(Lesson lesson)`: `LessonResponse` に変換
  - BMIを含める

#### 3.3 LessonResponse.java
**パス**: `src/main/java/com/example/fitnessgym_mg/dto/response/LessonResponse.java`
**変更内容**: DTOフィールドを追加
- `id`: UUID
- `startDate`: OffsetDateTime（レッスン開始日時）
- `endDate`: OffsetDateTime（レッスン終了日時）
- `weight`: BigDecimal（体重）
- `bmi`: BigDecimal（BMI）
- `condition`: String（体調メモ）
- `memo`: String（レッスンメモ）
- その他のレッスン情報（必要に応じて）

#### 3.4 LessonRepository.java
**パス**: `src/main/java/com/example/fitnessgym_mg/repository/LessonRepository.java`
**変更内容**: クエリメソッドを追加（既存メソッドを確認）
- `findByCustomerIdOrderByStartDateDesc(UUID customerId)`: 既存
- `findByCustomerIdAndStartDateAfter(UUID customerId, OffsetDateTime startDate)`: 指定日以降のレッスンを取得

#### 3.5 history.html
**パス**: `src/main/resources/templates/customer/history.html`
**変更内容**: 履歴画面テンプレートを新規作成
- 体重・BMI折れ線グラフ（過去3ヶ月）
  - 横軸: レッスン実施日
  - 縦軸（左）: 体重
  - 縦軸（右）: BMI
  - 横スクロールで3ヶ月以前のデータを閲覧可能
  - 初回レッスン時の計測値を点線で表示
- レッスン履歴一覧
  - 開始日時・終了日時、体重、BMIを表示
  - ページネーション（10件以上の場合）
  - レッスンを選択するとレッスン詳細画面へ遷移（将来実装）

#### 3.6 BMI計算ロジック
**パス**: `src/main/java/com/example/fitnessgym_mg/service/LessonService.java` またはユーティリティクラス
**実装内容**: BMI = weight (kg) / (height (m))²

---

## データフロー

1. **ログイン** → セッションに `userId` と `userRole` を保存
2. **顧客選択** → セッションから `userId` を取得して担当顧客を取得
3. **履歴画面** → URLパラメータから `customerId` を取得してレッスン履歴を表示

---

## セッション管理

セッションに保存する情報:
- `userId`: UUID（ユーザーID）
- `userRole`: UserRole（権限）

---

## 注意事項

1. **セキュリティ**
   - パスワードはBCryptでハッシュ化
   - セッションタイムアウト設定
   - CSRF保護（本番環境で有効化）

2. **パフォーマンス**
   - 顧客選択画面で大量データ取得時はページネーションを検討
   - グラフデータは必要最小限の期間のみ取得

3. **エラーハンドリング**
   - ログイン失敗時のエラーメッセージ
   - 無効アカウント時のエラーメッセージ
   - データ取得失敗時のエラーメッセージ

---

## 実装順序

1. **ログイン機能**（AuthController, AuthService, login.html）
2. **顧客選択機能**（CustomerController, CustomerService, customer-selection.html）
3. **履歴画面機能**（LessonController, LessonService, history.html）

