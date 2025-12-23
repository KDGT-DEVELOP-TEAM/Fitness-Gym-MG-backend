# React Router × API マッピング

React Routerの各画面URLごとに使用するAPIを明確に記載したマッピングドキュメントです。

---

## 目次

1. [共通機能](#共通機能)
2. [トレーナー画面](#トレーナー画面)
3. [店長画面](#店長画面)
4. [本部管理者画面](#本部管理者画面)

---

## 共通機能

### `/login` - ログイン

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/auth/login` | - | 共通 |
| ログイン実行 | POST | `/api/auth/login` | RequestBody: email, password | 共通 |

### ログアウト（全画面共通）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| ログアウト実行 | POST | `/api/auth/logout` | - | 共通 |

---

## トレーナー画面

### `/trainer/home` - トレーナーホーム

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/trainers/home` | - | トレーナー |

### `/trainer/customers` - 顧客一覧（トレーナー）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/trainers/customers` | store_id: 店舗ID（パス） | トレーナー |

### `/customer/{c_id}` - 顧客プロフィール（閲覧・編集）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/customers/{customer_id}/profile` | customer_id: 顧客ID（パス） | 共通 |
| 更新実行 | PATCH | `/api/customers/{customer_id}/profile` | customer_id: 顧客ID（パス）、RequestBody: 更新情報 | 共通 |

### `/customer/{c_id}/lesson/new` - レッスン新規入力

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 保存実行 | POST | `/api/customers/{customer_id}/lessons` | customer_id: 顧客ID（パス）、RequestBody: レッスン情報 | 共通 |
| 姿勢画像グループ作成 | POST | `/api/lessons/{l_id}/posture_groups` | l_id: レッスンID（パス）、RequestBody: グループ情報 | 共通 |

### `/customer/{c_id}/lessons` - レッスン履歴一覧

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/customers/{customer_id}/lessons` | customer_id: 顧客ID（パス） | 共通 |

### `/lesson/{l_id}` - レッスン履歴詳細（閲覧・編集）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/lessons/{lesson_id}` | lesson_id: レッスンID（パス） | 共通 |
| 更新実行 | PATCH | `/api/lessons/{lesson_id}` | lesson_id: レッスンID（パス）、RequestBody: 更新情報 | 共通 |

### `/customer/{c_id}/lesson/{l_id}/posture` - 姿勢画像アップロード

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 画像アップロード | POST | `/api/posture_images/upload` | RequestBody: file, postureGroupId, position | 共通 |

### `/customer/{c_id}/vitals` - 体重/BMI履歴（グラフ）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/customers/{customer_id}/vitals/history` | customer_id: 顧客ID（パス） | 共通 |

### `/customer/{c_id}/posture_groups` - 姿勢画像一覧（グループ取得）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/customers/{customer_id}/posture_groups` | customer_id: 顧客ID（パス） | 共通 |

### `/customer/{c_id}/posture/{img_id}/delete` - 姿勢画像削除

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 削除実行 | DELETE | `/api/posture_images/{img_id}` | img_id: 画像ID（パス） | 共通 |

### `/customer/{c_id}/posture/compare` - 姿勢画像比較

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/customers/{c_id}/posture_groups` | c_id: 顧客ID（パス） | 共通 |

---

## 店長画面

### `/manager/home` - 店長ホーム

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/manager/home` | store_id: 店舗ID（パス） | 店長 |

### `/manager/users` - ユーザー一覧（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/manager/users` | store_id: 店舗ID（パス） | 店長 |
| 検索 | GET | `/api/stores/{store_id}/manager/users?name={keyword}` | store_id: 店舗ID（パス）、name: 検索キーワード（クエリ） | 店長 |

### `/manager/users/{u_id}/detail` - ユーザー詳細（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/manager/users/{user_id}` | store_id: 店舗ID（パス）、user_id: ユーザーID（パス） | 店長 |

### `/manager/users/create` - ユーザー作成（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 作成実行 | POST | `/api/stores/{store_id}/manager/users` | store_id: 店舗ID（パス）、RequestBody: ユーザー情報 | 店長 |

### `/manager/users/{u_id}/edit` - ユーザー情報更新（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/manager/users/{user_id}` | store_id: 店舗ID（パス）、user_id: ユーザーID（パス） | 店長 |
| 更新実行 | PATCH | `/api/stores/{store_id}/manager/users/{user_id}` | store_id: 店舗ID（パス）、user_id: ユーザーID（パス）、RequestBody: 更新情報 | 店長 |

### `/manager/users/{u_id}/delete` - ユーザー削除（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 削除実行 | DELETE | `/api/stores/{store_id}/manager/users/{user_id}` | store_id: 店舗ID（パス）、user_id: ユーザーID（パス） | 店長 |

### `/manager/customers` - 顧客一覧（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/stores/{store_id}/manager/customers` | store_id: 店舗ID（パス） | 店長 |
| 検索 | GET | `/api/stores/{store_id}/manager/customers?name={keyword}` | store_id: 店舗ID（パス）、name: 検索キーワード（クエリ） | 店長 |

### `/manager/customers/{c_id}/disable` - 顧客無効化（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 無効化実行 | PATCH | `/api/stores/{store_id}/manager/customers/{customer_id}/disable` | store_id: 店舗ID（パス）、customer_id: 顧客ID（パス） | 店長 |

### `/manager/customers/{c_id}/enable` - 顧客有効化（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 有効化実行 | PATCH | `/api/stores/{store_id}/manager/customers/{customer_id}/enable` | store_id: 店舗ID（パス）、customer_id: 顧客ID（パス） | 店長 |

### `/manager/customers/create` - 顧客新規作成（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 作成実行 | POST | `/api/stores/{store_id}/manager/customers` | store_id: 店舗ID（パス）、RequestBody: 顧客情報 | 店長 |

### `/manager/customers/delete` - 顧客削除（店長）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 削除実行 | DELETE | `/api/stores/{store_id}/manager/customers/{customer_id}` | store_id: 店舗ID（パス）、customer_id: 顧客ID（パス） | 店長 |

---

## 本部管理者画面

### `/admin/home` - 本部ホーム

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/home` | - | 本部管理者 |

### `/admin/users` - ユーザー一覧（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/users` | - | 本部管理者 |
| 検索 | GET | `/api/admin/users?name={keyword}` | name: 検索キーワード（クエリ） | 本部管理者 |

### `/admin/users/{u_id}/detail` - ユーザー詳細（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/users/{user_id}` | user_id: ユーザーID（パス） | 本部管理者 |

### `/admin/users/create` - ユーザー作成（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 作成実行 | POST | `/api/admin/users` | RequestBody: ユーザー情報 | 本部管理者 |

### `/admin/users/{u_id}/edit` - ユーザー情報更新（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/users/{user_id}` | user_id: ユーザーID（パス） | 本部管理者 |
| 更新実行 | PATCH | `/api/admin/users/{user_id}` | user_id: ユーザーID（パス）、RequestBody: 更新情報 | 本部管理者 |

### `/admin/users/{u_id}/delete` - ユーザー削除（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 削除実行 | DELETE | `/api/admin/users/{user_id}` | user_id: ユーザーID（パス） | 本部管理者 |

### `/admin/customers` - 顧客一覧（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/customers` | - | 本部管理者 |
| 検索 | GET | `/api/admin/customers?name={keyword}` | name: 検索キーワード（クエリ） | 本部管理者 |

### `/admin/customers/{c_id}/disable` - 顧客無効化（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 無効化実行 | PATCH | `/api/admin/customers/{customer_id}/disable` | customer_id: 顧客ID（パス） | 本部管理者 |

### `/admin/customers/{c_id}/enable` - 顧客有効化（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 有効化実行 | PATCH | `/api/admin/customers/{customer_id}/enable` | customer_id: 顧客ID（パス） | 本部管理者 |

### `/admin/customers/create` - 顧客新規作成（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 作成実行 | POST | `/api/admin/customers` | RequestBody: 顧客情報 | 本部管理者 |

### `/admin/customers/delete` - 顧客削除（本部）

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 削除実行 | DELETE | `/api/admin/customers/{customer_id}` | customer_id: 顧客ID（パス） | 本部管理者 |

### `/admin/logs` - 監査ログ

| タイミング | HTTPメソッド | エンドポイント | パラメータ | 権限 |
|-----------|------------|---------------|----------|------|
| 初期表示 | GET | `/api/admin/logs` | - | 本部管理者 |

---

## 補足

- **パスパラメータ**: URL内の`{param_name}`形式のパラメータ
- **クエリパラメータ**: URLの`?key=value`形式のパラメータ
- **RequestBody**: POST/PATCHリクエストのボディに含めるJSONデータ
- **権限**: そのAPIを呼び出せるロール（共通: 全ロール、トレーナー: トレーナーのみ、店長: 店長のみ、本部管理者: 本部管理者のみ）
