# FitnessGym MG

## プロジェクト概要


## 技術スタック
- Java 17+
- Spring Boot
- Gradle
- Thymeleaf、CSS（静的ファイル）

## ディレクトリ構成
```text
project-root/
├── build.gradle または pom.xml        # 依存関係・ビルド設定
├── gradlew / gradlew.bat              # Gradle実行スクリプト
├── settings.gradle                    # Gradle設定ファイル
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/app/
│   │   │       ├── entity/                      # データベース構造（エンティティ）
│   │   │       │   ├── User.java
│   │   │       │   ├── Customer.java
│   │   │       │   ├── Store.java
│   │   │       │   ├── Lesson.java
│   │   │       │   ├── Training.java
│   │   │       │   ├── PostureGroup.java
│   │   │       │   └── PostureImage.java
│   │   │       │
│   │   │       ├── repository/                  # DBアクセス層（JPAなど）
│   │   │       │   ├── UserRepository.java
│   │   │       │   ├── CustomerRepository.java
│   │   │       │   ├── StoreRepository.java
│   │   │       │   ├── LessonRepository.java
│   │   │       │   ├── TrainingRepository.java
│   │   │       │   ├── PostureGroupRepository.java
│   │   │       │   └── PostureImageRepository.java
│   │   │       │
│   │   │       ├── service/                     # ビジネスロジック層
│   │   │       │   ├── AuthService.java
│   │   │       │   ├── AccountService.java
│   │   │       │   ├── CustomerService.java
│   │   │       │   ├── LessonService.java
│   │   │       │   ├── TrainingService.java
│   │   │       │   ├── PostureGroupService.java
│   │   │       │   └── PostureImageService.java
│   │   │       │
│   │   │       ├── dto/                         # データ転送用オブジェクト
│   │   │       │   ├── request/
│   │   │       │   │   ├── LoginRequest.java
│   │   │       │   │   ├── UserRequest.java
│   │   │       │   │   ├── CustomerRequest.java
│   │   │       │   │   ├── LessonRequest.java
│   │   │       │   │   ├── TrainingRequest.java
│   │   │       │   │   ├── PostureGroupRequest.java
│   │   │       │   │   └── PostureImageRequest.java
│   │   │       │   │
│   │   │       │   └── response/
│   │   │       │       ├── LoginResponse.java
│   │   │       │       ├── UserResponse.java
│   │   │       │       ├── CustomerResponse.java
│   │   │       │       ├── LessonResponse.java
│   │   │       │       ├── TrainingResponse.java
│   │   │       │       ├── PostureGroupResponse.java
│   │   │       │       └── PostureImageResponse.java
│   │   │       │
│   │   │       ├── controller/                  # エンドポイント（Web/API）
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── UserController.java
│   │   │       │   ├── CustomerController.java
│   │   │       │   ├── LessonController.java
│   │   │       │   ├── TrainingController.java
│   │   │       │   ├── PostureGroupController.java
│   │   │       │   └── PostureImageController.java
│   │   │       │
│   │   │       ├── config/                      # 設定クラス群
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── WebConfig.java
│   │   │       │   └── AppConfig.java
│   │   │       │
│   │   │       └── FitnessGym_MG.java           # Spring Boot メインクラス
│   │   │
│   │   └── resources/
│   │       ├── static/                          # CSS・JS・画像など静的ファイル
│   │       │   └── css/
│   │       │       ├── style.css
│   │       │       └── reset.css
│   │       │
│   │       ├── templates/                       # HTMLテンプレート（Thymeleaf）
│   │       │   ├── layout/
│   │       │   │   └── base.html                # 共通レイアウト
│   │       │   ├── auth/
│   │       │   │   ├── login.html
│   │       │   │   └── register.html
│   │       │   ├── user/
│   │       │   │   └── user-list.html
│   │       │   ├── customer/
│   │       │   │   └── customer-detail.html
│   │       │   ├── lesson/
│   │       │   │   └── lesson-list.html
│   │       │   ├── training/
│   │       │   │   └── training-detail.html
│   │       │   └── posture/
│   │       │       ├── posture-group.html
│   │       │       └── posture-image.html
│   │       │
│   │       ├── application.properties     # 設定ファイル
│   │       ├── schema.sql                 # DBスキーマ初期化
│   │       └── data.sql                   # 初期データ投入
│   │
│   └── test/
│       └── java/
│           └── com/example/app/
│               ├── entity/
│               │   └── UserEntityTest.java
│               ├── service/
│               │   └── UserServiceTest.java
│               ├── repository/
│               │   └── UserRepositoryTest.java
│               └── controller/
│                   └── UserControllerTest.java
│
└── README.md
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

commit例 feat: ログイン機能実装

