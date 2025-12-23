# フロントエンド実装ガイド（React + TypeScript）

このドキュメントは、バックエンドREST APIをフロントエンドで使用するための実装ガイドです。

---

## 推奨ファイル構造

### 完全なプロジェクト構成

```bash
fitness-gym-frontend/

├── src/
│   ├── api/
│   │   ├── client.ts                    # APIクライアント（fetch設定）
│   │   ├── authApi.ts                   # 認証API
│   │   ├── admin/
│   │   │   ├── homeApi.ts               # 管理者ホームAPI
│   │   │   ├── usersApi.ts              # ユーザー管理API
│   │   │   ├── customersApi.ts          # 顧客管理API
│   │   │   └── logsApi.ts               # 監査ログAPI
│   │   ├── manager/
│   │   │   ├── homeApi.ts               # 店長ホームAPI
│   │   │   ├── usersApi.ts              # ユーザー管理API（店長）
│   │   │   └── customersApi.ts          # 顧客管理API（店長）
│   │   ├── trainer/
│   │   │   ├── homeApi.ts               # トレーナーホームAPI
│   │   │   └── customersApi.ts          # 顧客一覧API（トレーナー）
│   │   ├── customerApi.ts               # 顧客関連API
│   │   ├── lessonApi.ts                 # レッスンAPI
│   │   └── postureApi.ts                # 姿勢画像API

```

### 主要ディレクトリの役割

| ディレクトリ | 役割 |
|------------|------|
| `src/api/` | バックエンドAPI呼び出し関数を配置（ロール別に分割） |
| `src/components/` | 再利用可能なUIコンポーネント（機能別に分割） |
| `src/pages/` | 画面コンポーネント（ロール別に分割） |
| `src/routes/` | React Routerルート設定・認証チェック |
| `src/hooks/` | カスタムフック（ロジックの共通化） |
| `src/contexts/` | グローバルステート管理（React Context） |
| `src/types/` | TypeScript型定義 |
| `src/utils/` | ユーティリティ関数 |
| `src/styles/` | スタイルシート |

### 注記

画面（pages）は量が膨大になるため、API呼び出しコードを完全網羅し、画面側はそれを呼べば動く状態を想定します。

---

## 1. APIクライアント設定

### `src/api/client.ts`

```typescript
const API_BASE_URL = process.env.REACT_APP_API_BASE_URL || '';

export const apiClient = {
  async get<T>(url: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'GET',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }
    
    return response.json();
  },

  async post<T>(url: string, data?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: data ? JSON.stringify(data) : undefined,
    });
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }
    
    return response.json();
  },

  async patch<T>(url: string, data?: any): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
      body: data ? JSON.stringify(data) : undefined,
    });
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }
    
    return response.json();
  },

  async delete<T>(url: string): Promise<T> {
    const response = await fetch(`${API_BASE_URL}${url}`, {
      method: 'DELETE',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }
    
    return response.json();
  },
};
```

---

## 2. 認証API

### `src/api/authApi.ts`

```typescript
import { apiClient } from './client';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  email: string;
  name: string;
  role: 'ADMIN' | 'MANAGER' | 'TRAINER';
  storeId?: string;
}

export const authApi = {
  login: async (credentials: LoginRequest): Promise<LoginResponse> => {
    return apiClient.post('/api/auth/login', credentials);
  },

  logout: async (): Promise<void> => {
    return apiClient.post('/api/auth/logout');
  },

  checkAuth: async (): Promise<LoginResponse> => {
    return apiClient.get('/api/auth/login');
  },
};
```

---

## 3. ホーム / ダッシュボード

### トレーナー

#### `src/api/trainer/homeApi.ts`

```typescript
import { apiClient } from '../client';

export const fetchTrainerHome = () =>
  apiClient.get('/api/trainers/home');
```

### 管理者（本部）

#### `src/api/admin/homeApi.ts`

```typescript
import { apiClient } from '../client';

export const fetchAdminHome = () =>
  apiClient.get('/api/admin/home');
```

### 店長

#### `src/api/manager/homeApi.ts`

```typescript
import { apiClient } from '../client';

export const fetchManagerHome = (storeId: string) =>
  apiClient.get(`/api/stores/${storeId}/manager/home`);
```

---

## 4. ユーザー管理

### 管理者（本部）

#### `src/api/admin/usersApi.ts`

```typescript
import { apiClient } from '../client';

export interface UserListParams {
  page?: number;
  size?: number;
  name?: string;
  role?: string;
  active?: boolean;
}

export const usersApi = {
  getUsers: (params?: UserListParams) => {
    const queryString = new URLSearchParams(params as any).toString();
    return apiClient.get(`/api/admin/users?${queryString}`);
  },

  getUser: (userId: string) =>
    apiClient.get(`/api/admin/users/${userId}`),

  createUser: (userData: any) =>
    apiClient.post('/api/admin/users', userData),

  updateUser: (userId: string, userData: any) =>
    apiClient.patch(`/api/admin/users/${userId}`, userData),

  deleteUser: (userId: string) =>
    apiClient.delete(`/api/admin/users/${userId}`),
};
```

### 店長

#### `src/api/manager/usersApi.ts`

```typescript
import { apiClient } from '../client';

export const managerUsersApi = {
  getUsers: (storeId: string, params?: any) => {
    const queryString = new URLSearchParams(params).toString();
    return apiClient.get(`/api/stores/${storeId}/manager/users?${queryString}`);
  },

  getUser: (storeId: string, userId: string) =>
    apiClient.get(`/api/stores/${storeId}/manager/users/${userId}`),

  createUser: (storeId: string, userData: any) =>
    apiClient.post(`/api/stores/${storeId}/manager/users`, userData),

  updateUser: (storeId: string, userId: string, userData: any) =>
    apiClient.patch(`/api/stores/${storeId}/manager/users/${userId}`, userData),

  deleteUser: (storeId: string, userId: string) =>
    apiClient.delete(`/api/stores/${storeId}/manager/users/${userId}`),
};
```

---

## 5. 顧客管理

### 管理者（本部）

#### `src/api/admin/customersApi.ts`

```typescript
import { apiClient } from '../client';

export interface CustomerListParams {
  page?: number;
  size?: number;
  name?: string;
  kana?: string;
  active?: boolean;
}

export const customersApi = {
  getCustomers: (params?: CustomerListParams) => {
    const queryString = new URLSearchParams(params as any).toString();
    return apiClient.get(`/api/admin/customers?${queryString}`);
  },

  createCustomer: (customerData: any) =>
    apiClient.post('/api/admin/customers', customerData),

  enableCustomer: (customerId: string) =>
    apiClient.patch(`/api/admin/customers/${customerId}/enable`),

  disableCustomer: (customerId: string) =>
    apiClient.patch(`/api/admin/customers/${customerId}/disable`),

  deleteCustomer: (customerId: string) =>
    apiClient.delete(`/api/admin/customers/${customerId}`),
};
```

### 店長

#### `src/api/manager/customersApi.ts`

```typescript
import { apiClient } from '../client';

export const managerCustomersApi = {
  getCustomers: (storeId: string, params?: any) => {
    const queryString = new URLSearchParams(params).toString();
    return apiClient.get(`/api/stores/${storeId}/manager/customers?${queryString}`);
  },

  createCustomer: (storeId: string, customerData: any) =>
    apiClient.post(`/api/stores/${storeId}/manager/customers`, customerData),

  enableCustomer: (storeId: string, customerId: string) =>
    apiClient.patch(`/api/stores/${storeId}/manager/customers/${customerId}/enable`),

  disableCustomer: (storeId: string, customerId: string) =>
    apiClient.patch(`/api/stores/${storeId}/manager/customers/${customerId}/disable`),

  deleteCustomer: (storeId: string, customerId: string) =>
    apiClient.delete(`/api/stores/${storeId}/manager/customers/${customerId}`),
};
```

### トレーナー

#### `src/api/trainer/customersApi.ts`

```typescript
import { apiClient } from '../client';

export const trainerCustomersApi = {
  getCustomers: (storeId: string) =>
    apiClient.get(`/api/stores/${storeId}/trainers/customers`),
};
```

### 顧客プロフィール（共通）

#### `src/api/customerApi.ts`

```typescript
import { apiClient } from './client';

export const customerApi = {
  getProfile: (customerId: string) =>
    apiClient.get(`/api/customers/${customerId}/profile`),

  updateProfile: (customerId: string, profileData: any) =>
    apiClient.patch(`/api/customers/${customerId}/profile`, profileData),

  getVitalsHistory: (customerId: string) =>
    apiClient.get(`/api/customers/${customerId}/vitals/history`),
};
```

---

## 6. レッスン管理

### `src/api/lessonApi.ts`

```typescript
import { apiClient } from './client';

export interface LessonCreateRequest {
  customerId: string;
  storeId: string;
  trainerId: string;
  lessonDate: string;
  trainings: Array<{
    menuName: string;
    weight?: number;
    reps?: number;
    sets?: number;
    duration?: number;
  }>;
}

export const lessonApi = {
  createLesson: (customerId: string, lessonData: LessonCreateRequest) =>
    apiClient.post(`/api/customers/${customerId}/lessons`, lessonData),

  getLessons: (customerId: string, params?: { page?: number; size?: number }) => {
    const queryString = new URLSearchParams(params as any).toString();
    return apiClient.get(`/api/customers/${customerId}/lessons?${queryString}`);
  },

  getLesson: (lessonId: string) =>
    apiClient.get(`/api/lessons/${lessonId}`),

  updateLesson: (lessonId: string, lessonData: any) =>
    apiClient.patch(`/api/lessons/${lessonId}`, lessonData),
};
```

---

## 7. 姿勢画像管理

### `src/api/postureApi.ts`

```typescript
import { apiClient } from './client';

export const postureApi = {
  getPostureGroups: (customerId: string) =>
    apiClient.get(`/api/customers/${customerId}/posture_groups`),

  createPostureGroup: (lessonId: string, groupData: any) =>
    apiClient.post(`/api/lessons/${lessonId}/posture_groups`, groupData),

  uploadImage: async (file: File, lessonId: string, groupId: string, position: string) => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await fetch(`/api/lessons/${lessonId}/posture_groups/${groupId}/images`, {
      method: 'POST',
      credentials: 'include',
      body: formData,
    });

    if (!response.ok) {
      throw new Error(`Upload failed: ${response.status}`);
    }

    return response.json();
  },

  deleteImage: (imageId: string) =>
    apiClient.delete(`/api/posture_images/${imageId}`),
};
```

---

## 8. 監査ログ（管理者専用）

### `src/api/admin/logsApi.ts`

```typescript
import { apiClient } from '../client';

export interface LogParams {
  page?: number;
  size?: number;
}

export const logsApi = {
  getLogs: (params?: LogParams) => {
    const queryString = new URLSearchParams(params as any).toString();
    return apiClient.get(`/api/admin/logs?${queryString}`);
  },
};
```

---

## 9. React Router ルーティング設計

### `src/routes/AppRouter.tsx`

```typescript
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './ProtectedRoute';
import { LoginPage } from '../pages/auth/LoginPage';
import { AdminDashboard } from '../pages/admin/AdminDashboard';
import { ManagerDashboard } from '../pages/manager/ManagerDashboard';
import { TrainerHome } from '../pages/trainer/TrainerHome';

export const AppRouter = () => {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />

        <Route path="/trainer" element={<ProtectedRoute roles={['TRAINER']} />}>
          <Route index element={<Navigate to="/trainer/home" replace />} />
          <Route path="home" element={<TrainerHome />} />
          <Route path="customers" element={<CustomerSelectPage />} />
        </Route>

        <Route path="/admin" element={<ProtectedRoute roles={['ADMIN']} />}>
          <Route index element={<Navigate to="/admin/home" replace />} />
          <Route path="home" element={<AdminDashboard />} />
          <Route path="users" element={<UserListPage />} />
          <Route path="users/:userId/detail" element={<UserDetailPage />} />
          <Route path="users/create" element={<UserCreatePage />} />
          <Route path="users/:userId/edit" element={<UserEditPage />} />
          <Route path="customers" element={<CustomerListPage />} />
          <Route path="customers/create" element={<CustomerCreatePage />} />
          <Route path="customers/:customerId/disable" element={<CustomerDisablePage />} />
          <Route path="customers/:customerId/enable" element={<CustomerEnablePage />} />
          <Route path="customers/delete" element={<CustomerDeletePage />} />
          <Route path="logs" element={<AuditLogPage />} />
        </Route>

        <Route path="/manager" element={<ProtectedRoute roles={['MANAGER']} />}>
          <Route index element={<Navigate to="/manager/home" replace />} />
          <Route path="home" element={<ManagerDashboard />} />
          <Route path="users" element={<UserListPage />} />
          <Route path="users/:userId/detail" element={<UserDetailPage />} />
          <Route path="users/create" element={<UserCreatePage />} />
          <Route path="users/:userId/edit" element={<UserEditPage />} />
          <Route path="customers" element={<CustomerListPage />} />
          <Route path="customers/create" element={<CustomerCreatePage />} />
          <Route path="customers/:customerId/disable" element={<CustomerDisablePage />} />
          <Route path="customers/:customerId/enable" element={<CustomerEnablePage />} />
          <Route path="customers/delete" element={<CustomerDeletePage />} />
        </Route>

        <Route path="/customer/:customerId" element={<ProtectedRoute roles={['ADMIN', 'MANAGER', 'TRAINER']} />}>
          <Route index element={<CustomerProfilePage />} />
          <Route path="edit" element={<CustomerProfileEditPage />} />
          <Route path="lesson/new" element={<LessonCreatePage />} />
          <Route path="lessons" element={<LessonHistoryPage />} />
          <Route path="vitals" element={<VitalsHistoryPage />} />
          <Route path="posture_groups" element={<PostureGroupListPage />} />
          <Route path="posture/compare" element={<PostureComparePage />} />
        </Route>

        <Route path="/lesson/:lessonId" element={<ProtectedRoute roles={['ADMIN', 'MANAGER', 'TRAINER']} />}>
          <Route index element={<LessonDetailPage />} />
          <Route path="edit" element={<LessonEditPage />} />
        </Route>

        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  );
};
```

### ルートとAPIの対応表

| ルートパス | 権限 | 対応するバックエンドAPI |
|-----------|------|----------------------|
| `/login` | Public | `POST /api/auth/login` |
| `/trainer/home` | TRAINER | `GET /api/trainers/home` |
| `/trainer/customers` | TRAINER | `GET /api/stores/{store_id}/trainers/customers` |
| `/admin/home` | ADMIN | `GET /api/admin/home` |
| `/admin/users` | ADMIN | `GET /api/admin/users` |
| `/admin/users/:userId/detail` | ADMIN | `GET /api/admin/users/{user_id}` |
| `/admin/users/create` | ADMIN | `POST /api/admin/users` |
| `/admin/users/:userId/edit` | ADMIN | `PATCH /api/admin/users/{user_id}` |
| `/admin/customers` | ADMIN | `GET /api/admin/customers` |
| `/admin/customers/create` | ADMIN | `POST /api/admin/customers` |
| `/admin/customers/:customerId/disable` | ADMIN | `PATCH /api/admin/customers/{customer_id}/disable` |
| `/admin/customers/:customerId/enable` | ADMIN | `PATCH /api/admin/customers/{customer_id}/enable` |
| `/admin/customers/delete` | ADMIN | `DELETE /api/admin/customers/{customer_id}` |
| `/admin/logs` | ADMIN | `GET /api/admin/logs` |
| `/manager/home` | MANAGER | `GET /api/stores/{store_id}/manager/home` |
| `/manager/users` | MANAGER | `GET /api/stores/{store_id}/manager/users` |
| `/manager/users/:userId/detail` | MANAGER | `GET /api/stores/{store_id}/manager/users/{user_id}` |
| `/manager/users/create` | MANAGER | `POST /api/stores/{store_id}/manager/users` |
| `/manager/users/:userId/edit` | MANAGER | `PATCH /api/stores/{store_id}/manager/users/{user_id}` |
| `/manager/customers` | MANAGER | `GET /api/stores/{store_id}/manager/customers` |
| `/manager/customers/create` | MANAGER | `POST /api/stores/{store_id}/manager/customers` |
| `/manager/customers/:customerId/disable` | MANAGER | `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable` |
| `/manager/customers/:customerId/enable` | MANAGER | `PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable` |
| `/manager/customers/delete` | MANAGER | `DELETE /api/stores/{store_id}/manager/customers/{customer_id}` |
| `/customer/:customerId` | 共通 | `GET /api/customers/{customer_id}/profile` |
| `/customer/:customerId/edit` | 共通 | `PATCH /api/customers/{customer_id}/profile` |
| `/customer/:customerId/lesson/new` | 共通 | `POST /api/customers/{customer_id}/lessons` |
| `/customer/:customerId/lessons` | 共通 | `GET /api/customers/{customer_id}/lessons` |
| `/customer/:customerId/vitals` | 共通 | `GET /api/customers/{customer_id}/vitals/history` |
| `/customer/:customerId/posture_groups` | 共通 | `GET /api/customers/{customer_id}/posture_groups` |
| `/customer/:customerId/posture/compare` | 共通 | `GET /api/customers/{customer_id}/posture_groups` |
| `/lesson/:lessonId` | 共通 | `GET /api/lessons/{lesson_id}` |
| `/lesson/:lessonId/edit` | TRAINER | `PATCH /api/lessons/{lesson_id}` |

---

## 10. 保護されたルート（Protected Route）

### `src/routes/ProtectedRoute.tsx`

```typescript
import { Navigate, Outlet } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';

interface ProtectedRouteProps {
  roles?: string[];
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ roles }) => {
  const { user, isAuthenticated, isLoading } = useAuth();

  if (isLoading) {
    return <div>Loading...</div>;
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (roles && !roles.includes(user.role)) {
    return <Navigate to="/403" replace />;
  }

  return <Outlet />;
};
```

---

## 11. 環境変数設定

### `.env`

```env
REACT_APP_API_BASE_URL=http://localhost:8080
```

### `.env.example`

```env
REACT_APP_API_BASE_URL=
```

---

## 12. 重要な注意点

### 認証方式

- セッションベース（Cookie）
- `credentials: 'include'` を必ず指定

### CSRF

- `/api/**` エンドポイントはCSRF無効

### CORS

- 同一オリジンなら設定不要
- 別オリジン（例: React開発サーバーが `localhost:3000`、バックエンドが `localhost:8080`）の場合は要設定
