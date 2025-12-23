# REST API と画面の対応表

## 目次

1. [認証画面](#1-認証画面)
2. [ホーム/ダッシュボード画面](#2-ホームダッシュボード画面)
3. [ユーザー管理画面](#3-ユーザー管理画面)
4. [顧客管理画面](#4-顧客管理画面)
5. [レッスン管理画面](#5-レッスン管理画面)
6. [姿勢画像管理画面](#6-姿勢画像管理画面)
7. [体重/BMI履歴画面](#7-体重bmi履歴画面)
8. [監査ログ画面](#8-監査ログ画面)

---

## 1. 認証画面

### 1.1 ログイン画面 (`/auth/login.html`)

**REST API エンドポイント:**
- `GET /api/auth/login` - ログイン状態確認
- `POST /api/auth/login` - ログイン処理
- `POST /api/auth/logout` - ログアウト処理

**コントローラー:** `AuthApiController`

---

## 2. ホーム/ダッシュボード画面

### 2.1 トレーナー用ホーム画面

**REST API エンドポイント:**
- `GET /api/trainers/home` - 当日・直近(1週間以内)の予約状況/レッスン概要の取得

**コントローラー:** `HomeApiController`

### 2.2 管理者用ホーム画面

**REST API エンドポイント:**
- `GET /api/admin/home` - 管理者用ダッシュボード情報取得

**コントローラー:** `HomeApiController`

### 2.3 店長用ホーム画面

**REST API エンドポイント:**
- `GET /api/stores/{store_id}/manager/home` - 店長用ダッシュボード情報取得

**コントローラー:** `HomeApiController`

---

## 3. ユーザー管理画面 (`/user/user-list.html`)

### 3.1 管理者用ユーザー管理

**REST API エンドポイント:**
- `GET /api/admin/users` - ユーザー一覧取得（検索/フィルタリング/ページネーション対応）
- `GET /api/admin/users/{user_id}` - 特定ユーザー情報取得
- `POST /api/admin/users` - ユーザー作成
- `PATCH /api/admin/users/{user_id}` - ユーザー更新
- `DELETE /api/admin/users/{user_id}` - ユーザー削除

**コントローラー:** `UserApiController`

### 3.2 店長用ユーザー管理

**REST API エンドポイント:**
- `GET /api/stores/{store_id}/manager/users` - ユーザー一覧取得（検索/フィルタリング/ページネーション対応）
- `GET /api/stores/{store_id}/manager/users/{user_id}` - 特定ユーザー情報取得
- `POST /api/stores/{store_id}/manager/users` - ユーザー作成（トレーナーのみ）
- `PATCH /api/stores/{store_id}/manager/users/{user_id}` - ユーザー更新
- `DELETE /api/stores/{store_id}/manager/users/{user_id}` - ユーザー削除

**コントローラー:** `UserApiController`

---

## 4. 顧客管理画面

### 4.1 顧客一覧画面 (`/customer/customer_list.html`)

**REST API エンドポイント（Web用）:**
- `GET /admin/customers` - 管理者用顧客一覧（Thymeleaf）
- `GET /manager/{storeId}/customers` - 店長用顧客一覧（Thymeleaf）

**REST API エンドポイント（React用）:**
- `GET /api/admin/customers` - 管理者用顧客一覧取得（JSON、検索/フィルタリング/ページネーション対応）
- `GET /api/stores/{store_id}/manager/customers` - 店長用顧客一覧取得（JSON）
- `GET /api/stores/{store_id}/trainers/customers` - トレーナー用顧客一覧取得（JSON）

**コントローラー:** `CustomerApiController`

### 4.2 顧客詳細/プロフィール画面 (`/customer/customer-profile.html`, `/customer/customer-detail.html`)

**REST API エンドポイント（Web用）:**
- `GET /admin/customers/{id}/detail` - 管理者用顧客詳細取得（JSON）
- `GET /manager/{storeId}/customers/{id}/detail` - 店長用顧客詳細取得（JSON）

**REST API エンドポイント（React用）:**
- `GET /api/customers/{customer_id}/profile` - 顧客プロフィール取得
- `PATCH /api/customers/{customer_id}/profile` - 顧客プロフィール更新

**コントローラー:** `CustomerApiController`

### 4.3 顧客作成/編集機能

**REST API エンドポイント（Web用）:**
- `POST /admin/customers/create` - 管理者用顧客作成
- `POST /manager/{storeId}/customers/create` - 店長用顧客作成
- `PUT /admin/customers/{id}/edit` - 管理者用顧客更新
- `PUT /manager/{storeId}/customers/{id}/edit` - 店長用顧客更新

**REST API エンドポイント（React用）:**
- `POST /api/admin/customers` - 管理者用顧客作成
- `POST /api/stores/{store_id}/manager/customers` - 店長用顧客作成

**コントローラー:** `CustomerApiController`

### 4.4 顧客有効化/無効化

**REST API エンドポイント（Web用）:**
- `PATCH /admin/customers/{id}/enable` - 管理者用顧客有効化
- `PATCH /admin/customers/{id}/disable` - 管理者用顧客無効化
- `PATCH /manager/{storeId}/customers/{id}/enable` - 店長用顧客有効化
- `PATCH /manager/{storeId}/customers/{id}/disable` - 店長用顧客無効化

**REST API エンドポイント（React用）:**
- `PATCH /api/admin/customers/{customer_id}/enable` - 管理者用顧客有効化
- `PATCH /api/admin/customers/{customer_id}/disable` - 管理者用顧客無効化
- `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable` - 店長用顧客有効化
- `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable` - 店長用顧客無効化

**コントローラー:** `CustomerApiController`

### 4.5 顧客削除

**REST API エンドポイント（Web用）:**
- `DELETE /admin/customers/{id}/delete` - 管理者用顧客削除
- `DELETE /manager/{storeId}/customers/{id}/delete` - 店長用顧客削除

**REST API エンドポイント（React用）:**
- `DELETE /api/admin/customers/{customer_id}` - 管理者用顧客削除
- `DELETE /api/stores/{store_id}/manager/customers/{customer_id}` - 店長用顧客削除

**コントローラー:** `CustomerApiController`

---

## 5. レッスン管理画面

### 5.1 レッスン一覧画面 (`/lesson/lesson-list.html`)

**REST API エンドポイント（Web用）:**
- `GET /admin/lessons` - 管理者用レッスン一覧（Thymeleaf、検索/フィルタリング/ページネーション対応）
- `GET /manager/{storeId}/lessons` - 店長用レッスン一覧（Thymeleaf）

**コントローラー:** `LessonApiController`

### 5.2 レッスン作成画面 (`/lesson/lesson-new.html`)

**REST API エンドポイント:**
- `POST /api/customers/{customer_id}/lessons` - レッスン作成

**コントローラー:** `LessonApiController`

### 5.3 レッスン詳細画面 (`/lesson/lesson-detail.html`)

**REST API エンドポイント:**
- `GET /api/lessons/{lesson_id}` - レッスン詳細取得
- `PATCH /api/lessons/{lesson_id}` - レッスン更新

**コントローラー:** `LessonApiController`

### 5.4 顧客レッスン履歴画面

**REST API エンドポイント:**
- `GET /api/customers/{customer_id}/lessons` - 顧客のレッスン履歴一覧取得（ページネーション/フィルタリング対応）

**コントローラー:** `LessonApiController`

### 5.5 レッスン統計/チャート画面

**REST API エンドポイント（Web用）:**
- `GET /admin/lessons/chart` - 管理者用レッスン統計チャートデータ取得
- `GET /manager/{storeId}/lessons/chart` - 店長用レッスン統計チャートデータ取得

**コントローラー:** `LessonApiController`

---

## 6. 姿勢画像管理画面

### 6.1 姿勢画像グループ一覧画面 (`/posture/posture-group.html`)

**REST API エンドポイント:**
- `GET /api/customers/{customer_id}/posture_groups` - 顧客の姿勢画像グループ一覧取得

**コントローラー:** `PostureGroupApiController`

### 6.2 姿勢画像グループ作成

**REST API エンドポイント:**
- `POST /api/lessons/{lesson_id}/posture_groups` - 姿勢画像グループ作成

**コントローラー:** `PostureGroupApiController`

### 6.3 姿勢画像管理画面 (`/posture/posture-image.html`)

**REST API エンドポイント:**
- `POST /api/posture_images/upload` - 姿勢画像アップロード
- `GET /api/posture_images/{imageId}/signed-url` - 署名付きURL生成（単一画像）
- `POST /api/posture_images/signed-urls` - バッチ署名付きURL生成（複数画像）
- `DELETE /api/posture_images/{imageId}` - 姿勢画像削除

**コントローラー:** `PostureImageApiController`

---

## 7. 体重/BMI履歴画面

**REST API エンドポイント:**
- `GET /api/customers/{customer_id}/vitals/history` - 体重・BMI履歴の時系列データ取得

**コントローラー:** `VitalsApiController`

---

## 8. 監査ログ画面

**REST API エンドポイント:**
- `GET /api/admin/logs` - 監査ログ一覧取得（ページネーション対応）

**コントローラー:** `AuditLogApiController`

---

