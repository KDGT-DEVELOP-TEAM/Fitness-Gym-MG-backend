# フィットネスジム管理システム - フロントエンド API 接続ガイド

## はじめに

このドキュメントは、React等のフロントエンドフレームワークから本システムのREST APIに接続する際のガイドです。

### 認証方式
- **セッションベース認証**を使用
- ログイン成功後、セッションIDがCookieに保存されます
- 全てのAPI呼び出しにセッションCookieを含める必要があります

### ベースURL
```
http://localhost:8080
```

### 共通レスポンス形式

#### 成功時
- ステータスコード: `200 OK` / `201 Created`
- ボディ: JSONオブジェクトまたは配列

#### エラー時
- ステータスコード: `400 Bad Request` / `401 Unauthorized` / `404 Not Found` / `500 Internal Server Error`
- ボディ: エラーメッセージ

---

## 目次

1. [認証API](#1-認証api)
2. [ホーム/ダッシュボードAPI](#2-ホームダッシュボードapi)
3. [ユーザー管理API](#3-ユーザー管理api)
4. [顧客管理API](#4-顧客管理api)
5. [レッスン記録API](#5-レッスン記録api)
6. [体重/BMI履歴API](#6-体重bmi履歴api)
7. [姿勢画像管理API](#7-姿勢画像管理api)
8. [監査ログAPI](#8-監査ログapi)

---

## 1. 認証API

### 1.1 ログイン状態確認

**エンドポイント**: `GET /api/auth/login`

**権限**: なし（認証状態を確認）

**レスポンス**:
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "name": "山田太郎",
  "role": "ADMIN",
  "token": "session-id"
}
```

**使用例**:
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'GET',
  credentials: 'include'
});
const data = await response.json();
```

---

### 1.2 ログイン

**エンドポイント**: `POST /api/auth/login`

**権限**: なし

**リクエストボディ**:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**レスポンス**:
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "name": "山田太郎",
  "role": "ADMIN",
  "token": "session-id"
}
```

**使用例**:
```javascript
const response = await fetch('http://localhost:8080/api/auth/login', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',
  body: JSON.stringify({
    email: 'admin@example.com',
    password: 'password123'
  })
});
const data = await response.json();
```

---

### 1.3 ログアウト

**エンドポイント**: `POST /api/auth/logout`

**権限**: 認証済み

**レスポンス**: 200 OK

**使用例**:
```javascript
await fetch('http://localhost:8080/api/auth/logout', {
  method: 'POST',
  credentials: 'include'
});
```

---

## 2. ホーム/ダッシュボードAPI

### 2.1 トレーナーホーム

**エンドポイント**: `GET /api/trainers/home`

**権限**: トレーナー

**レスポンス**:
```json
{
  "upcomingLessons": [
    {
      "id": "uuid",
      "startDate": "2025-01-15T10:00:00",
      "endDate": "2025-01-15T11:00:00",
      "storeName": "渋谷店",
      "trainerName": "山田太郎",
      "customerId": "uuid",
      "customerName": "田中花子"
    }
  ]
}
```

**使用例**:
```javascript
const response = await fetch('http://localhost:8080/api/trainers/home', {
  credentials: 'include'
});
const data = await response.json();
```

---

### 2.2 管理者ホーム

**エンドポイント**: `GET /api/admin/home`

**権限**: 本部管理者

**クエリパラメータ**:
- `chartType` (optional): `month` / `week` / `day` (デフォルト: `month`)
- `page` (optional): ページ番号 (デフォルト: 0)
- `size` (optional): 1ページあたりの件数 (デフォルト: 10)

**レスポンス**:
```json
{
  "recentLessons": [
    {
      "id": "uuid",
      "startDate": "2025-01-15T10:00:00",
      "storeName": "渋谷店",
      "trainerName": "山田太郎",
      "customerName": "田中花子"
    }
  ],
  "totalLessonCount": 1500,
  "chartData": {
    "series": [
      {
        "period": "2025-01",
        "count": 150
      }
    ],
    "maxCount": 200,
    "type": "month"
  }
}
```

---

### 2.3 店長ホーム

**エンドポイント**: `GET /api/stores/{store_id}/manager/home`

**権限**: 店長

**パスパラメータ**:
- `store_id`: 店舗ID

**クエリパラメータ**:
- `chartType` (optional): `month` / `week` / `day`
- `page`, `size`

**レスポンス**: 管理者ホームと同じ形式

---

## 3. ユーザー管理API

### 3.1 ユーザー一覧取得（本部）

**エンドポイント**: `GET /api/admin/users`

**権限**: 本部管理者

**クエリパラメータ**:
- `name` (optional): 検索キーワード（最大100文字）
- `role` (optional): ロールフィルター
- `sort` (optional): ソート順 (デフォルト: `created`)
- `page` (optional): ページ番号 (デフォルト: 0)
- `size` (optional): 1ページあたりの件数 (デフォルト: 10, 最大: 100)

**レスポンス**:
```json
{
  "content": [
    {
      "id": "uuid",
      "email": "user@example.com",
      "name": "山田太郎",
      "kana": "ヤマダタロウ",
      "role": "TRAINER",
      "active": true,
      "storeIds": ["uuid1", "uuid2"],
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

**使用例**:
```javascript
const response = await fetch('http://localhost:8080/api/admin/users?name=山田&page=0&size=10', {
  credentials: 'include'
});
const data = await response.json();
```

---

### 3.2 ユーザー詳細取得（本部）

**エンドポイント**: `GET /api/admin/users/{user_id}`

**権限**: 本部管理者

**レスポンス**:
```json
{
  "id": "uuid",
  "email": "user@example.com",
  "name": "山田太郎",
  "kana": "ヤマダタロウ",
  "role": "TRAINER",
  "active": true,
  "storeIds": ["uuid1", "uuid2"],
  "createdAt": "2025-01-01T00:00:00"
}
```

---

### 3.3 ユーザー作成（本部）

**エンドポイント**: `POST /api/admin/users`

**権限**: 本部管理者

**リクエストボディ**:
```json
{
  "email": "newuser@example.com",
  "password": "password123",
  "name": "新規ユーザー",
  "kana": "シンキユーザー",
  "role": "TRAINER",
  "storeIds": ["uuid1", "uuid2"]
}
```

**レスポンス**: 201 Created

**バリデーション**:
- `email`: 必須、有効なメールアドレス、最大255文字
- `password`: 必須、最小8文字
- `name`: 必須、最大100文字
- `kana`: 必須、最大100文字、カタカナのみ
- `role`: 必須、`ADMIN` / `MANAGER` / `TRAINER`

---

### 3.4 ユーザー更新（本部）

**エンドポイント**: `PATCH /api/admin/users/{user_id}`

**権限**: 本部管理者

**リクエストボディ**: ユーザー作成と同じ形式（変更したいフィールドのみ送信可能）

**レスポンス**: 200 OK

---

### 3.5 ユーザー削除（本部）

**エンドポイント**: `DELETE /api/admin/users/{user_id}`

**権限**: 本部管理者

**レスポンス**: 200 OK

---

### 3.6 店長用ユーザー管理API

店長は自分の店舗に所属するユーザーのみを管理できます。

- **ユーザー一覧**: `GET /api/stores/{store_id}/manager/users`
- **ユーザー詳細**: `GET /api/stores/{store_id}/manager/users/{user_id}`
- **ユーザー作成**: `POST /api/stores/{store_id}/manager/users`
- **ユーザー更新**: `PATCH /api/stores/{store_id}/manager/users/{user_id}`
- **ユーザー削除**: `DELETE /api/stores/{store_id}/manager/users/{user_id}`

リクエスト/レスポンス形式は本部用APIと同じです。

---

## 4. 顧客管理API

### 4.1 顧客一覧取得（本部）

**エンドポイント**: `GET /api/admin/customers`

**権限**: 本部管理者

**クエリパラメータ**:
- `name` (optional): 検索キーワード（最大100文字）
- `sort` (optional): `created` / `name` / `age` (デフォルト: `created`)
- `page` (optional): ページ番号 (デフォルト: 0)
- `size` (optional): 1ページあたりの件数 (デフォルト: 10, 最大: 100)

**レスポンス**:
```json
{
  "content": [
    {
      "id": "uuid",
      "name": "田中花子",
      "kana": "タナカハナコ",
      "active": true,
      "email": "hanako@example.com",
      "phone": "090-1234-5678",
      "age": 25,
      "gender": "FEMALE",
      "birthdate": "2000-01-01",
      "address": "東京都渋谷区",
      "height": 165.5,
      "createdAt": "2025-01-01T00:00:00"
    }
  ],
  "totalElements": 200,
  "totalPages": 20,
  "number": 0,
  "size": 10
}
```

---

### 4.2 顧客作成（本部）

**エンドポイント**: `POST /api/admin/customers`

**権限**: 本部管理者

**リクエストボディ**:
```json
{
  "kana": "タナカハナコ",
  "name": "田中花子",
  "gender": "FEMALE",
  "birthday": "2000-01-01",
  "height": 165.5,
  "email": "hanako@example.com",
  "phone": "090-1234-5678",
  "address": "東京都渋谷区",
  "medical": "特になし",
  "taboo": "特になし",
  "memo": "備考",
  "active": true
}
```

**レスポンス**: 201 Created

**バリデーション**:
- `kana`: 必須、最大100文字
- `name`: 必須、最大100文字
- `gender`: 必須、`MALE` / `FEMALE` / `OTHER`
- `birthday`: 必須、日付形式 (YYYY-MM-DD)
- `height`: 必須、50〜300（cm）、小数点以下2桁まで
- `email`: 必須、有効なメールアドレス、最大255文字
- `phone`: 必須、数字とハイフンのみ、最大12文字
- `address`: 必須、最大200文字
- `medical`: 任意、最大100文字
- `taboo`: 任意、最大100文字
- `memo`: 任意、最大500文字

---

### 4.3 顧客プロフィール取得

**エンドポイント**: `GET /api/customers/{customer_id}/profile`

**権限**: 全ての認証済みユーザー

**レスポンス**:
```json
{
  "id": "uuid",
  "name": "田中花子",
  "kana": "タナカハナコ",
  "active": true,
  "email": "hanako@example.com",
  "phone": "090-1234-5678",
  "age": 25,
  "gender": "FEMALE",
  "birthdate": "2000-01-01",
  "address": "東京都渋谷区",
  "height": 165.5,
  "latestWeight": 55.5,
  "firstPostureGroupId": "uuid"
}
```

---

### 4.4 顧客プロフィール更新

**エンドポイント**: `PATCH /api/customers/{customer_id}/profile`

**権限**: 全ての認証済みユーザー

**リクエストボディ**: 顧客作成と同じ形式（変更したいフィールドのみ送信可能）

**レスポンス**: 200 OK

---

### 4.5 顧客無効化（本部）

**エンドポイント**: `PATCH /api/admin/customers/{customer_id}/disable`

**権限**: 本部管理者

**レスポンス**: 200 OK

---

### 4.6 顧客有効化（本部）

**エンドポイント**: `PATCH /api/admin/customers/{customer_id}/enable`

**権限**: 本部管理者

**レスポンス**: 200 OK

---

### 4.7 顧客削除（本部）

**エンドポイント**: `DELETE /api/admin/customers/{customer_id}`

**権限**: 本部管理者

**レスポンス**: 200 OK

---

### 4.8 店長用顧客管理API

店長は自分の店舗に所属する顧客のみを管理できます。

- **顧客一覧**: `GET /api/stores/{store_id}/manager/customers`
- **顧客作成**: `POST /api/stores/{store_id}/manager/customers`
- **顧客無効化**: `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable`
- **顧客有効化**: `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable`
- **顧客削除**: `DELETE /api/stores/{store_id}/manager/customers/{customer_id}`

---

### 4.9 トレーナー用顧客一覧

**エンドポイント**: `GET /api/stores/{store_id}/trainers/customers`

**権限**: トレーナー

**レスポンス**: 担当顧客のリスト（配列形式、ページネーションなし）
```json
[
  {
    "id": "uuid",
    "name": "田中花子",
    "kana": "タナカハナコ",
    "email": "hanako@example.com",
    "age": 25
  }
]
```

---

## 5. レッスン記録API

### 5.1 レッスン作成

**エンドポイント**: `POST /api/customers/{customer_id}/lessons`

**権限**: 全ての認証済みユーザー

**リクエストボディ**:
```json
{
  "customerId": "uuid",
  "storeId": "uuid",
  "trainerId": "uuid",
  "condition": "良好",
  "weight": 55.5,
  "meal": "朝食: パン、昼食: サラダ",
  "memo": "今日の調子は良かった",
  "startDate": "2025-01-15T10:00:00",
  "endDate": "2025-01-15T11:00:00",
  "nextDate": "2025-01-22T10:00:00",
  "nextStoreId": "uuid",
  "nextTrainerId": "uuid",
  "trainings": [
    {
      "orderNo": 1,
      "menuId": "uuid",
      "weight": 50.0,
      "reps": 10,
      "sets": 3,
      "memo": "フォームに注意"
    }
  ]
}
```

**レスポンス**: 201 Created
```json
{
  "id": "uuid",
  "startDate": "2025-01-15T10:00:00",
  "endDate": "2025-01-15T11:00:00",
  "storeName": "渋谷店",
  "trainerName": "山田太郎",
  "customerId": "uuid",
  "customerName": "田中花子",
  "condition": "良好",
  "weight": 55.5,
  "bmi": 20.3,
  "meal": "朝食: パン、昼食: サラダ",
  "memo": "今日の調子は良かった",
  "trainings": [...]
}
```

**バリデーション**:
- `customerId`: 必須
- `storeId`: 必須
- `trainerId`: 必須
- `condition`: 任意、最大500文字
- `weight`: 任意、0〜500（kg）、小数点以下2桁まで
- `meal`: 任意、最大500文字
- `memo`: 任意、最大1000文字
- `startDate`: 必須、日時形式
- `endDate`: 必須、日時形式（startDateより後）
- `trainings`: 任意、トレーニング配列

---

### 5.2 顧客のレッスン履歴一覧

**エンドポイント**: `GET /api/customers/{customer_id}/lessons`

**権限**: 全ての認証済みユーザー

**クエリパラメータ**:
- `page` (optional): ページ番号 (デフォルト: 0)
- `size` (optional): 1ページあたりの件数 (デフォルト: 10, 最大: 100)

**レスポンス**:
```json
{
  "content": [
    {
      "id": "uuid",
      "startDate": "2025-01-15T10:00:00",
      "endDate": "2025-01-15T11:00:00",
      "storeName": "渋谷店",
      "trainerName": "山田太郎",
      "customerId": "uuid",
      "customerName": "田中花子",
      "weight": 55.5,
      "bmi": 20.3
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "number": 0,
  "size": 10
}
```

---

### 5.3 レッスン詳細取得

**エンドポイント**: `GET /api/lessons/{lesson_id}`

**権限**: 全ての認証済みユーザー

**レスポンス**:
```json
{
  "id": "uuid",
  "startDate": "2025-01-15T10:00:00",
  "endDate": "2025-01-15T11:00:00",
  "storeName": "渋谷店",
  "trainerName": "山田太郎",
  "customerId": "uuid",
  "customerName": "田中花子",
  "condition": "良好",
  "weight": 55.5,
  "bmi": 20.3,
  "meal": "朝食: パン、昼食: サラダ",
  "memo": "今日の調子は良かった",
  "nextDate": "2025-01-22T10:00:00",
  "nextStoreName": "渋谷店",
  "nextTrainerName": "山田太郎",
  "trainings": [
    {
      "lessonId": "uuid",
      "orderNo": 1,
      "menuName": "ベンチプレス",
      "weight": 50.0,
      "reps": 10,
      "sets": 3,
      "memo": "フォームに注意"
    }
  ],
  "postureImages": [
    {
      "id": "uuid",
      "storageKey": "posture/uuid/front.jpg",
      "position": "FRONT",
      "takenAt": "2025-01-15T10:00:00",
      "consentPublication": true
    }
  ]
}
```

---

### 5.4 レッスン更新

**エンドポイント**: `PATCH /api/lessons/{lesson_id}`

**権限**: トレーナー

**リクエストボディ**: レッスン作成と同じ形式（変更したいフィールドのみ送信可能）

**レスポンス**: 200 OK（レッスン詳細と同じ形式）

---

## 6. 体重/BMI履歴API

### 6.1 体重/BMI履歴取得

**エンドポイント**: `GET /api/customers/{customer_id}/vitals/history`

**権限**: 全ての認証済みユーザー

**レスポンス**:
```json
{
  "data": [
    {
      "date": "2025-01-15T10:00:00",
      "weight": 55.5,
      "bmi": 20.3
    },
    {
      "date": "2025-01-08T10:00:00",
      "weight": 56.0,
      "bmi": 20.5
    }
  ]
}
```

**使用例（Chart.js）**:
```javascript
const response = await fetch(`http://localhost:8080/api/customers/${customerId}/vitals/history`, {
  credentials: 'include'
});
const { data } = await response.json();

// Chart.jsでグラフ表示
const chartData = {
  labels: data.map(d => new Date(d.date).toLocaleDateString()),
  datasets: [
    {
      label: '体重 (kg)',
      data: data.map(d => d.weight),
      borderColor: 'rgb(75, 192, 192)',
    },
    {
      label: 'BMI',
      data: data.map(d => d.bmi),
      borderColor: 'rgb(255, 99, 132)',
    }
  ]
};
```

---

## 7. 姿勢画像管理API

### 7.1 顧客の姿勢画像グループ一覧

**エンドポイント**: `GET /api/customers/{customer_id}/posture_groups`

**権限**: 全ての認証済みユーザー

**レスポンス**:
```json
[
  {
    "id": "uuid",
    "lessonId": "uuid",
    "lessonStartDate": "2025-01-15T10:00:00+09:00",
    "capturedAt": "2025-01-15T10:30:00",
    "images": [
      {
        "id": "uuid",
        "storageKey": "posture/uuid/front.jpg",
        "position": "FRONT",
        "takenAt": "2025-01-15T10:30:00",
        "consentPublication": true
      },
      {
        "id": "uuid",
        "storageKey": "posture/uuid/right.jpg",
        "position": "RIGHT",
        "takenAt": "2025-01-15T10:31:00",
        "consentPublication": true
      }
    ]
  }
]
```

---

### 7.2 姿勢画像グループ作成

**エンドポイント**: `POST /api/lessons/{lesson_id}/posture_groups`

**権限**: 全ての認証済みユーザー

**レスポンス**: 201 Created（グループ詳細と同じ形式）

---

### 7.3 姿勢画像アップロード

**エンドポイント**: `POST /api/posture_images/upload`

**権限**: 全ての認証済みユーザー

**リクエスト**: `multipart/form-data`
- `file`: 画像ファイル（必須）
- `postureGroupId`: 姿勢画像グループID（必須）
- `position`: `FRONT` / `RIGHT` / `BACK` / `LEFT`（必須）
- `consentPublication`: 公開同意（デフォルト: false）
- `takenAt`: 撮影日時（任意）

**レスポンス**: 200 OK
```json
{
  "id": "uuid",
  "storageKey": "posture/uuid/front.jpg",
  "signedUrl": "https://storage.example.com/..."
}
```

**使用例**:
```javascript
const formData = new FormData();
formData.append('file', imageFile);
formData.append('postureGroupId', groupId);
formData.append('position', 'FRONT');
formData.append('consentPublication', 'true');

const response = await fetch('http://localhost:8080/api/posture_images/upload', {
  method: 'POST',
  credentials: 'include',
  body: formData
});
const data = await response.json();
```

---

### 7.4 署名付きURL生成（単一画像）

**エンドポイント**: `GET /api/posture_images/{imageId}/signed-url`

**権限**: 全ての認証済みユーザー

**クエリパラメータ**:
- `expiresIn` (optional): 有効期限（秒）、60〜604800（デフォルト: 3600）

**レスポンス**:
```json
{
  "imageId": "uuid",
  "signedUrl": "https://storage.example.com/...",
  "expiresIn": 3600
}
```

---

### 7.5 署名付きURL生成（バッチ）

**エンドポイント**: `POST /api/posture_images/signed-urls`

**権限**: 全ての認証済みユーザー

**リクエストボディ**:
```json
{
  "imageIds": ["uuid1", "uuid2", "uuid3"],
  "expiresIn": 3600
}
```

**レスポンス**:
```json
{
  "urls": [
    {
      "imageId": "uuid1",
      "signedUrl": "https://storage.example.com/...",
      "expiresIn": 3600
    },
    {
      "imageId": "uuid2",
      "signedUrl": "https://storage.example.com/...",
      "expiresIn": 3600
    }
  ]
}
```

**使用例**:
```javascript
const response = await fetch('http://localhost:8080/api/posture_images/signed-urls', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json'
  },
  credentials: 'include',
  body: JSON.stringify({
    imageIds: ['uuid1', 'uuid2', 'uuid3'],
    expiresIn: 3600
  })
});
const { urls } = await response.json();

// 画像を表示
urls.forEach(({ imageId, signedUrl }) => {
  const img = document.createElement('img');
  img.src = signedUrl;
  document.body.appendChild(img);
});
```

---

### 7.6 姿勢画像削除

**エンドポイント**: `DELETE /api/posture_images/{imageId}`

**権限**: 全ての認証済みユーザー

**レスポンス**: 204 No Content

---

## 8. 監査ログAPI

### 8.1 監査ログ取得

**エンドポイント**: `GET /api/admin/logs`

**権限**: 本部管理者

**クエリパラメータ**:
- `page` (optional): ページ番号 (デフォルト: 0)
- `size` (optional): 1ページあたりの件数 (デフォルト: 10)

**レスポンス**:
```json
{
  "content": [
    {
      "id": "uuid",
      "userId": "uuid",
      "userName": "山田太郎",
      "action": "CREATE",
      "targetType": "LESSON",
      "targetId": "uuid",
      "details": "新規レッスン作成",
      "createdAt": "2025-01-15T10:00:00"
    }
  ],
  "totalElements": 1000,
  "totalPages": 100,
  "number": 0,
  "size": 10
}
```

---

## 画面とAPIの対応表

| 画面 | React Route | 使用するAPI |
|------|-------------|------------|
| ログイン | `/login` | `POST /api/auth/login` |
| トレーナーHome | `/trainer/home` | `GET /api/trainers/home` |
| 本部Home | `/admin/home` | `GET /api/admin/home` |
| 店長Home | `/manager/home` | `GET /api/stores/{store_id}/manager/home` |
| ユーザー一覧（本部） | `/admin/users` | `GET /api/admin/users` |
| ユーザー詳細（本部） | `/admin/users/:id` | `GET /api/admin/users/{user_id}` |
| ユーザー作成（本部） | `/admin/users/create` | `POST /api/admin/users` |
| ユーザー編集（本部） | `/admin/users/:id/edit` | `PATCH /api/admin/users/{user_id}` |
| 顧客一覧（本部） | `/admin/customers` | `GET /api/admin/customers` |
| 顧客作成（本部） | `/admin/customers/create` | `POST /api/admin/customers` |
| 顧客一覧（トレーナー） | `/trainer/customers` | `GET /api/stores/{store_id}/trainers/customers` |
| 顧客プロフィール | `/customer/:id` | `GET /api/customers/{customer_id}/profile` |
| 顧客プロフィール編集 | `/customer/:id/edit` | `PATCH /api/customers/{customer_id}/profile` |
| レッスン新規入力 | `/customer/:id/lesson/new` | `POST /api/customers/{customer_id}/lessons` |
| レッスン履歴一覧 | `/customer/:id/lessons` | `GET /api/customers/{customer_id}/lessons` |
| レッスン詳細 | `/lesson/:id` | `GET /api/lessons/{lesson_id}` |
| レッスン編集 | `/lesson/:id/edit` | `PATCH /api/lessons/{lesson_id}` |
| 体重/BMI履歴 | `/customer/:id/vitals` | `GET /api/customers/{customer_id}/vitals/history` |
| 姿勢画像一覧 | `/customer/:id/posture_groups` | `GET /api/customers/{customer_id}/posture_groups` |
| 姿勢画像比較 | `/customer/:id/posture/compare` | `GET /api/customers/{customer_id}/posture_groups` + `POST /api/posture_images/signed-urls` |
| 監査ログ | `/admin/logs` | `GET /api/admin/logs` |

---

## エラーハンドリング

### 共通エラーレスポンス形式

```json
{
  "timestamp": "2025-01-15T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "バリデーションエラー",
  "path": "/api/customers"
}
```

### エラーコード

| コード | 説明 | 対処方法 |
|--------|------|---------|
| 400 | リクエストが不正 | リクエストボディやパラメータを確認 |
| 401 | 認証が必要 | ログインしてセッションを確立 |
| 403 | アクセス権限がない | 適切な権限を持つユーザーでログイン |
| 404 | リソースが見つからない | IDやパスパラメータを確認 |
| 500 | サーバーエラー | サーバーログを確認 |

### 使用例（エラーハンドリング）

```javascript
async function fetchData(url) {
  try {
    const response = await fetch(url, {
      credentials: 'include'
    });
    
    if (!response.ok) {
      if (response.status === 401) {
        // 認証エラー: ログイン画面にリダイレクト
        window.location.href = '/login';
        return;
      }
      
      const error = await response.json();
      throw new Error(error.message || 'エラーが発生しました');
    }
    
    return await response.json();
  } catch (error) {
    console.error('API Error:', error);
    // エラーメッセージを表示
    alert(error.message);
  }
}
```

---

## 開発時の注意事項

### 1. CORS設定

開発環境では、フロントエンドとバックエンドが異なるポートで動作する場合、CORS設定が必要です。

**React側（proxy設定）**:
```json
// package.json
{
  "proxy": "http://localhost:8080"
}
```

または

**Vite使用時**:
```javascript
// vite.config.js
export default {
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
}
```

### 2. 認証状態の管理

**React Context使用例**:
```javascript
import { createContext, useContext, useState, useEffect } from 'react';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // 認証状態を確認
    fetch('/api/auth/login', { credentials: 'include' })
      .then(res => res.ok ? res.json() : null)
      .then(data => setUser(data))
      .finally(() => setLoading(false));
  }, []);

  const login = async (email, password) => {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ email, password })
    });
    
    if (response.ok) {
      const data = await response.json();
      setUser(data);
      return data;
    }
    throw new Error('ログインに失敗しました');
  };

  const logout = async () => {
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include'
    });
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
```

### 3. APIクライアントの作成

**共通化例**:
```javascript
class ApiClient {
  constructor(baseURL = '') {
    this.baseURL = baseURL;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    const config = {
      ...options,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
    };

    const response = await fetch(url, config);

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.message || 'エラーが発生しました');
    }

    return response.json();
  }

  get(endpoint) {
    return this.request(endpoint);
  }

  post(endpoint, data) {
    return this.request(endpoint, {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  patch(endpoint, data) {
    return this.request(endpoint, {
      method: 'PATCH',
      body: JSON.stringify(data),
    });
  }

  delete(endpoint) {
    return this.request(endpoint, {
      method: 'DELETE',
    });
  }
}

export const apiClient = new ApiClient('/api');
```

**使用例**:
```javascript
import { apiClient } from './apiClient';

// 顧客一覧を取得
const customers = await apiClient.get('/admin/customers?page=0&size=10');

// 顧客を作成
const newCustomer = await apiClient.post('/admin/customers', {
  name: '田中花子',
  email: 'hanako@example.com',
  // ...
});
```

---

## まとめ

本ドキュメントは、フロントエンド開発者が本システムのREST APIに接続する際の基本的なガイドです。

- 全てのAPIは`/api`プレフィックスで始まります
- セッションベース認証を使用するため、`credentials: 'include'`を必ず指定してください
- バリデーションエラーは400番台、認証エラーは401/403で返されます
- ページネーション対応APIは`page`と`size`パラメータを受け付けます

詳細な実装やエラーハンドリングについては、各APIのドキュメントを参照してください。
