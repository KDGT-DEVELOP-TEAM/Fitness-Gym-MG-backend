# FitnessGym MG

## プロジェクト概要


## 技術スタック
- Java 17+
- Spring Boot
- Gradle
- Thymeleaf、CSS（静的ファイル）
- RestApi
- React + TS 

## ディレクトリ構成
```text
project-root/
├── build.gradle                          # 依存関係・ビルド設定
├── gradlew / gradlew.bat                 # Gradle実行スクリプト
├── settings.gradle                       # Gradle設定ファイル
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/fitnessgym_mg/
│   │   │       ├── entity/                      # データベース構造（エンティティ）
│   │   │       │   ├── User.java                # システムユーザー（管理者、店長、トレーナー）エンティティ
│   │   │       │   ├── Customer.java            # 顧客エンティティ
│   │   │       │   ├── Store.java                # 店舗エンティティ
│   │   │       │   ├── Lesson.java               # レッスンエンティティ
│   │   │       │   ├── Training.java             # トレーニング種目エンティティ
│   │   │       │   ├── PostureGroup.java         # 姿勢画像グループエンティティ
│   │   │       │   ├── PostureImage.java         # 姿勢画像エンティティ
│   │   │       │   ├── AuditLog.java             # 監査ログエンティティ
│   │   │       │   ├── UserCustomer.java         # ユーザーと顧客の中間テーブルエンティティ
│   │   │       │   ├── converter/               # JPAコンバーター
│   │   │       │   │   ├── GenderConverter.java # 性別列挙型のDB変換
│   │   │       │   │   ├── PostureImagePositionConverter.java # 姿勢画像位置列挙型のDB変換
│   │   │       │   │   └── UserRoleConverter.java # ユーザーロール列挙型のDB変換
│   │   │       │   └── enums/                   # 列挙型
│   │   │       │       ├── Gender.java           # 性別列挙型（MALE, FEMALE）
│   │   │       │       ├── PostureImagePosition.java # 姿勢画像位置列挙型（FRONT, RIGHT, BACK, LEFT）
│   │   │       │       └── UserRole.java        # ユーザーロール列挙型（ADMIN, MANAGER, TRAINER）
│   │   │       │
│   │   │       ├── repository/                  # DBアクセス層（JPAなど）
│   │   │       │   ├── UserRepository.java      # ユーザーエンティティのリポジトリ
│   │   │       │   ├── CustomerRepository.java  # 顧客エンティティのリポジトリ
│   │   │       │   ├── StoreRepository.java     # 店舗エンティティのリポジトリ
│   │   │       │   ├── LessonRepository.java    # レッスンエンティティのリポジトリ
│   │   │       │   ├── TrainingRepository.java   # トレーニング種目エンティティのリポジトリ
│   │   │       │   ├── PostureGroupRepository.java # 姿勢画像グループエンティティのリポジトリ
│   │   │       │   ├── PostureImageRepository.java # 姿勢画像エンティティのリポジトリ
│   │   │       │   ├── AuditLogRepository.java  # 監査ログエンティティのリポジトリ
│   │   │       │   └── UserCustomerRepository.java # ユーザー顧客中間テーブルのリポジトリ
│   │   │       │
│   │   │       ├── service/                     # ビジネスロジック層
│   │   │       │   ├── AccountService.java      # ユーザーアカウント管理サービス
│   │   │       │   ├── AuditLogService.java     # 監査ログ管理サービス
│   │   │       │   ├── CustomerService.java     # 顧客管理サービス
│   │   │       │   ├── CustomUserDetailsService.java # Spring Security認証用ユーザー詳細サービス
│   │   │       │   ├── LessonService.java       # レッスン管理サービス
│   │   │       │   ├── PostureGroupService.java # 姿勢画像グループ管理サービス
│   │   │       │   ├── PostureImageService.java # 姿勢画像管理サービス
│   │   │       │   ├── SupabaseStorageService.java # Supabaseストレージ操作サービス
│   │   │       │   └── TrainingService.java     # トレーニング種目管理サービス
│   │   │       │
│   │   │       ├── dto/                         # データ転送用オブジェクト
│   │   │       │   ├── request/                 # APIリクエスト用DTO
│   │   │       │   │   ├── LoginRequest.java     # ログインリクエストDTO
│   │   │       │   │   ├── UserRequest.java      # ユーザー作成・更新リクエストDTO
│   │   │       │   │   ├── CustomerRequest.java  # 顧客作成・更新リクエストDTO
│   │   │       │   │   ├── LessonRequest.java    # レッスン作成・更新リクエストDTO
│   │   │       │   │   ├── TrainingRequest.java  # トレーニング種目作成・更新リクエストDTO
│   │   │       │   │   ├── PostureGroupRequest.java # 姿勢画像グループ作成リクエストDTO
│   │   │       │   │   ├── PostureImageUploadRequest.java # 姿勢画像アップロードリクエストDTO
│   │   │       │   │   └── BatchSignedUrlRequest.java # 一括署名URL取得リクエストDTO
│   │   │       │   │
│   │   │       │   └── response/                # APIレスポンス用DTO
│   │   │       │       ├── LoginResponse.java    # ログインレスポンスDTO
│   │   │       │       ├── UserResponse.java     # ユーザー情報レスポンスDTO
│   │   │       │       ├── CustomerResponse.java # 顧客情報レスポンスDTO
│   │   │       │       ├── LessonResponse.java   # レッスン情報レスポンスDTO
│   │   │       │       ├── TrainingResponse.java  # トレーニング種目情報レスポンスDTO
│   │   │       │       ├── PostureGroupResponse.java # 姿勢画像グループ情報レスポンスDTO
│   │   │       │       ├── PostureImageResponse.java # 姿勢画像情報レスポンスDTO
│   │   │       │       ├── PostureImageUploadResponse.java # 姿勢画像アップロードレスポンスDTO
│   │   │       │       ├── AuditLogResponse.java # 監査ログ情報レスポンスDTO
│   │   │       │       ├── BatchSignedUrlResponse.java # 一括署名URLレスポンスDTO
│   │   │       │       ├── SignedUrlResponse.java # 署名URLレスポンスDTO
│   │   │       │       ├── HomeResponse.java     # ホーム画面情報レスポンスDTO
│   │   │       │       └── VitalsHistoryResponse.java # バイタル履歴情報レスポンスDTO
│   │   │       │
│   │   │       ├── controller/                  # エンドポイント（Web/API）
│   │   │       │   ├── UserController.java      # ユーザー管理Webコントローラー
│   │   │       │   ├── api/                     # REST API用コントローラー
│   │   │       │   │   ├── AuthApiController.java # 認証APIコントローラー
│   │   │       │   │   ├── UserApiController.java # ユーザー管理APIコントローラー
│   │   │       │   │   ├── CustomerApiController.java # 顧客管理APIコントローラー
│   │   │       │   │   ├── LessonApiController.java # レッスン管理APIコントローラー
│   │   │       │   │   ├── PostureGroupApiController.java # 姿勢画像グループ管理APIコントローラー
│   │   │       │   │   ├── PostureImageApiController.java # 姿勢画像管理APIコントローラー
│   │   │       │   │   ├── AuditLogApiController.java # 監査ログAPIコントローラー
│   │   │       │   │   ├── HomeApiController.java # ホーム画面APIコントローラー
│   │   │       │   │   └── VitalsApiController.java # バイタル情報APIコントローラー
│   │   │       │   ├── web/                     # Web用コントローラー
│   │   │       │   │   ├── AuthController.java  # 認証Webコントローラー
│   │   │       │   │   ├── CustomerController.java # 顧客管理Webコントローラー
│   │   │       │   │   ├── LessonController.java # レッスン管理Webコントローラー
│   │   │       │   │   ├── PostureWebController.java # 姿勢画像管理Webコントローラー
│   │   │       │   │   └── ErrorController.java # エラー処理Webコントローラー
│   │   │       │   └── util/                    # コントローラー用ユーティリティ
│   │   │       │       ├── ControllerModelUtils.java # コントローラー用モデルユーティリティ
│   │   │       │       └── ControllerPathUtils.java # コントローラー用パスユーティリティ
│   │   │       │
│   │   │       ├── config/                      # 設定クラス群
│   │   │       │   ├── AppConfig.java          # アプリケーション設定
│   │   │       │   ├── WebConfig.java          # Web設定（MVC設定など）
│   │   │       │   ├── SupabaseStorageProperties.java # Supabaseストレージ設定プロパティ
│   │   │       │   ├── JsonUtilsControllerAdvice.java # JSON変換用ControllerAdvice
│   │   │       │   └── security/                # セキュリティ設定
│   │   │       │       ├── SecurityConfig.java # Spring Security設定
│   │   │       │       ├── handler/            # 認証ハンドラー
│   │   │       │       │   └── CustomAuthenticationSuccessHandler.java # カスタム認証成功ハンドラー
│   │   │       │       └── service/            # セキュリティサービス
│   │   │       │           └── LoginRedirectService.java # ログイン後リダイレクトサービス
│   │   │       │
│   │   │       ├── exception/                   # 例外処理
│   │   │       │   ├── AuthenticationException.java # 認証例外
│   │   │       │   ├── EntityNotFoundException.java # エンティティ未検出例外
│   │   │       │   ├── JsonConversionException.java # JSON変換例外
│   │   │       │   ├── ErrorResponse.java      # エラーレスポンスDTO
│   │   │       │   ├── GlobalExceptionHandler.java # グローバル例外ハンドラー
│   │   │       │   └── WebExceptionHandler.java # Web例外ハンドラー
│   │   │       │
│   │   │       ├── util/                        # ユーティリティクラス
│   │   │       │   ├── JsonUtils.java           # JSON操作ユーティリティ
│   │   │       │   └── SecurityUtil.java        # セキュリティユーティリティ
│   │   │       │
│   │   │       └── FitnessgymMgApplication.java # Spring Boot メインクラス
│   │   │
│   │   └── resources/
│   │       ├── static/                          # CSS・JS・画像など静的ファイル
│   │       │   └── css/
│   │       │       ├── style.css                # メインスタイルシート
│   │       │       └── reset.css                 # CSSリセット
│   │       │
│   │       ├── templates/                       # HTMLテンプレート（Thymeleaf）
│   │       │   ├── layout/
│   │       │   │   └── base.html                # 共通レイアウト
│   │       │   ├── auth/
│   │       │   │   ├── login.html                # ログイン画面
│   │       │   │   └── register.html             # ユーザー登録画面
│   │       │   ├── user/
│   │       │   │   └── user-list.html            # ユーザー一覧画面
│   │       │   ├── customer/
│   │       │   │   ├── customer_list.html       # 顧客一覧画面
│   │       │   │   ├── customer-detail.html     # 顧客詳細画面
│   │       │   │   ├── customer-profile.html    # 顧客プロフィール画面
│   │       │   │   └── customer-select.html     # 顧客選択画面
│   │       │   ├── lesson/
│   │       │   │   ├── lesson-list.html         # レッスン一覧画面
│   │       │   │   ├── lesson-detail.html       # レッスン詳細画面
│   │       │   │   └── lesson-new.html          # レッスン新規作成画面
│   │       │   ├── training/
│   │       │   │   └── training-detail.html     # トレーニング詳細画面
│   │       │   ├── posture/
│   │       │   │   ├── posture-group.html       # 姿勢画像グループ画面
│   │       │   │   └── posture-image.html       # 姿勢画像画面
│   │       │   └── error/
│   │       │       ├── 403.html                 # 403 Forbiddenエラー画面
│   │       │       ├── 404.html                 # 404 Not Foundエラー画面
│   │       │       └── error.html               # 汎用エラー画面
│   │       │
│   │       └── application.properties           # 設定ファイル
│   │
│   └── test/
│       └── java/
│           └── com/example/fitnessgym_mg/
│               ├── FitnessgymMgApplicationTests.java # Spring Bootアプリケーションテスト
│               ├── PasswordHashGenerator.java        # パスワードハッシュ生成ユーティリティ
│               └── controller/
│                   └── PostureGroupControllerTest.java # 姿勢画像グループコントローラーテスト
│
└── README.md
```

## データベース構造

### Stores（店舗）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| name | varchar |  | ○ | ○ |  | 店舗名 |

### Users（ユーザー）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| email | varchar |  | ○ | ○ | ○ | メールアドレス（CHECK制約あり） |
| kana | varchar |  | ○ |  |  | フリガナ |
| name | varchar |  | ○ |  |  | 氏名 |
| pass | varchar(255) |  | ○ |  |  | ハッシュ化パスワード（bcrypt、60文字） |
| role | user_role |  | ○ |  |  | 権限区分 |
| is_active | boolean |  | ○ |  |  | 有効／無効 |
| created_at | timestamptz |  | ○ |  |  | 登録日時 |

### Customers（顧客）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| kana | varchar |  | ○ |  |  | フリガナ |
| name | varchar |  | ○ |  |  | 氏名 |
| gender | gender |  | ○ |  |  | 性別 |
| birthday | date |  | ○ |  |  | 生年月日 |
| height | numeric |  | ○ |  |  | 身長 |
| email | varchar |  | ○ | ○ |  | メールアドレス |
| phone | varchar |  | ○ |  |  | 電話番号 |
| address | varchar |  | ○ |  |  | 住所 |
| medical | varchar |  |  |  |  | 医療・既往歴 |
| taboo | varchar |  |  |  |  | 禁忌事項 |
| first_posture_group_id | uuid |  |  |  |  | FK → `posture_groups`（初回姿勢画像） |
| memo | varchar |  |  |  |  | 自由記入メモ |
| created_at | timestamptz |  | ○ |  |  | 登録日時 |
| is_active | boolean |  | ○ |  |  | 有効／無効 |

### Lessons（レッスン）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| store_id | uuid |  | ○ |  |  | FK → `stores` |
| user_id | uuid |  | ○ |  |  | FK → `users`（担当トレーナー） |
| customer_id | uuid |  | ○ |  |  | FK → `customers` |
| posture_group_id | uuid |  |  |  |  | FK → `posture_groups`（レッスン時姿勢） |
| condition | varchar |  |  |  |  | 体調メモ |
| weight | numeric |  |  |  |  | 体重 |
| meal | varchar |  |  |  |  | 食事内容 |
| memo | varchar |  |  |  |  | レッスンメモ |
| start_date | timestamptz |  |  |  |  | レッスン開始日時 |
| end_date | timestamptz |  |  |  |  | レッスン終了日時 |
| next_date | timestamptz |  |  |  |  | 次回予約日時 |
| next_store_id | uuid |  |  |  |  | FK → `stores`（次回予定店舗） |
| next_user_id | uuid |  |  |  |  | FK → `users`（次回担当） |
| created_at | timestamptz |  | ○ |  |  | 追加日時 |

### Trainings（トレーニング）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| lesson_id | uuid | ○ | ○ |  |  | 複合PK（lesson_id + order_no）、FK → `lessons` |
| order_no | int | ○ | ○ |  |  | 複合PK（lesson_id + order_no） |
| name | varchar |  | ○ |  |  | 種目名 |
| reps | int |  | ○ |  |  | 回数 |

### Logs（監査ログ）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| user_id | uuid |  | ○ |  |  | FK → `users` |
| action | varchar |  | ○ |  |  | 操作内容 |
| target_table | varchar |  | ○ |  |  | 対象テーブル |
| target_id | uuid |  | ○ |  |  | 対象レコードID |
| created_at | timestamptz |  | ○ |  |  | 操作日時 |

### Store_Customers（店舗×顧客）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| store_id | uuid | ○ | ○ |  |  | 複合PK（store_id + customer_id）、FK → `stores` |
| customer_id | uuid | ○ | ○ |  |  | 複合PK（store_id + customer_id）、FK → `customers` |

### User_Stores（ユーザー×店舗）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| user_id | uuid | ○ | ○ |  |  | 複合PK（user_id + store_id）、FK → `users` |
| store_id | uuid | ○ | ○ |  |  | 複合PK（user_id + store_id）、FK → `stores` |

### User_Customers（ユーザー×顧客）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| user_id | uuid | ○ | ○ |  |  | 複合PK（user_id + customer_id）、FK → `users` |
| customer_id | uuid | ○ | ○ |  |  | 複合PK（user_id + customer_id）、FK → `customers` |

### posture_groups（姿勢画像グループ）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| customer_id | uuid |  | ○ |  |  | FK → `customers` |
| lesson_id | uuid |  | ○ |  |  | FK → `lessons` |
| captured_at | timestamptz |  | ○ |  |  | 撮影日時 |
| created_at | timestamptz |  | ○ |  |  | 追加日時 |

### posture_images（姿勢画像）
| カラム名 | 型 | 主キー | NOT NULL | UNIQUE | INDEX | 備考 |
| --- | --- | --- | --- | --- | --- | --- |
| id | uuid | ○ | ○ | ○ | ○ | 主キー |
| posture_group_id | uuid |  | ○ |  |  | FK → `posture_groups` |
| storage_key | varchar |  | ○ | ○ |  | ストレージ上のパス |
| consent_publication | boolean |  | ○ |  |  | 公開同意フラグ |
| taken_at | timestamptz |  | ○ |  |  | 撮影日時 |
| created_at | timestamptz |  | ○ |  |  | 追加日時 |
| position | posture_image_position |  | ○ |  |  | 撮影方向 |

#### 列挙型の型定義（DB実体）
- user_role: ['admin','manager','trainer']
- gender: ['male','female']
- posture_image_position: ['front','right','back','left']

### ER 図（Mermaid）
```mermaid
erDiagram
    STORES ||--o{ LESSONS : "実施・次回"
    STORES ||--o{ STORE_CUSTOMERS : "所属"
    STORES ||--o{ USER_STORES : "配属"

    USERS ||--o{ LESSONS : "担当"
    USERS ||--o{ USER_STORES : "配属"
    USERS ||--o{ USER_CUSTOMERS : "担当"
    USERS ||--o{ LOGS : "操作"

    CUSTOMERS ||--o{ LESSONS : "受講"
    CUSTOMERS ||--o{ STORE_CUSTOMERS : "所属"
    CUSTOMERS ||--o{ USER_CUSTOMERS : "担当"
    CUSTOMERS ||--o{ POSTURE_GROUPS : "姿勢"

    LESSONS ||--o{ TRAININGS : "メニュー"
    LESSONS ||--o{ POSTURE_GROUPS : "姿勢記録"

    POSTURE_GROUPS ||--o{ POSTURE_IMAGES : "撮影"
```

## パッケージ概要
- `entity`: ユーザー、顧客、店舗、レッスン等の永続化モデル
- `repository`: Spring Data JPA を利用したデータアクセス層
- `service`: 業務ロジックとユースケースの調整
- `dto/request`, `dto/response`: コントローラーとクライアント間の入出力モデル
- `controller`: REST/API・Web エンドポイント
- `config`: セキュリティやアプリケーション全体の設定
- `resources/templates`: Thymeleaf テンプレート
- `resources/static`: CSS などの静的アセット
- `resources/schema.sql`, `resources/data.sql`: 初期化スクリプト
- `test/java`: 各レイヤーに対応したユニットテスト／インテグレーションテスト

## セットアップと実行
1. 依存関係を取得  
   `./gradlew build`
2. アプリケーションを起動  
   `./gradlew bootRun`
3. テスト実行  
   `./gradlew test`

## コミットメッセージ規則

- feat : 新機能の追加
- fix : バグの修正
- refactor : コードの整理・構造的改善
- update : 既存機能の改善（非バグ修正）
- docs : ドキュメントの修正
- test : テストコードの追加・修正
- chore : 定型作業や管理タスク
- style : フォーマット修正（動作変更なし
- build : ビルドシステムや依存関係の変更
- ci : CI/CD 設定変更
- revert : 変更の取り消し

commit例 "feat: ログイン機能実装"

