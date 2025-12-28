機能名: ログイン
画面 (React Route): /login
API (Spring REST): GET /api/auth/login
HTTPメソッド: GET
権限: 共通
進捗内容: -

ログイン（POST）
機能名: ログイン
画面 (React Route): /login
API (Spring REST): POST /api/auth/login
HTTPメソッド: POST
権限: 共通
進捗内容: 認証情報送信、JWTトークン取得（またはセッショントークン）

ログアウト
機能名: ログアウト
画面 (React Route): （全画面共通）
API (Spring REST): POST /api/auth/logout
HTTPメソッド: POST
権限: 共通
進捗内容: サーバー側セッション破棄/クライアント側トークン削除

2. ホーム/ダッシュボード
トレーナーHome
機能名: トレーナーHome
画面 (React Route): /trainer/home
API (Spring REST): GET /api/trainers/home
HTTPメソッド: GET
権限: トレーナー
進捗内容: 当日・直近(1週間以内)の予約状況/レッスン概要の取得

本部Home
機能名: 本部Home
画面 (React Route): /admin/home
API (Spring REST): GET /api/admin/home
HTTPメソッド: GET
権限: 本部管理者
進捗内容: 全店舗のレッスン実施回数/レッスン履歴一覧計情報概要の取得

店長Home
機能名: 店長Home
画面 (React Route): /manager/home
API (Spring REST): GET /api/stores/{store_id}/manager/home
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: 所属店舗のレッスン実施回数/レッスン履歴一覧計情報概要の取得

3. 本部: ユーザーアカウント管理
ユーザー一覧（本部）
機能名: ユーザー一覧（本部）
画面 (React Route): /admin/users
API (Spring REST): GET /api/admin/users
HTTPメソッド: GET
権限: 本部管理者
進捗内容: 全ユーザーアカウントの一覧取得（検索/フィルタリング可）

ユーザー検索（本部）
機能名: ユーザー検索（本部）
画面 (React Route): /admin/users
API (Spring REST): GET /api/admin/users?name={keyword}
HTTPメソッド: GET
権限: 本部管理者
クエリパラメータ: 
  - name: 検索キーワード
進捗内容: ユーザー名での検索

ユーザー詳細（本部）
機能名: ユーザー詳細（本部）
画面 (React Route): /admin/users/{u_id}/detail
API (Spring REST): GET /api/admin/users/{user_id}
HTTPメソッド: GET
権限: 本部管理者
パスパラメータ: 
  - user_id: ユーザーID
進捗内容: 特定ユーザー情報の取得

ユーザー作成（本部）
機能名: ユーザー作成（本部）
画面 (React Route): /admin/users/create
API (Spring REST): POST /api/admin/users
HTTPメソッド: POST
権限: 本部管理者
進捗内容: ユーザーアカウントの新規作成

ユーザー情報更新（本部）
機能名: ユーザー情報更新（本部）
画面 (React Route): /admin/users/{u_id}/edit
API (Spring REST): PATCH /api/admin/users/{user_id}
HTTPメソッド: PATCH
権限: 本部管理者
パスパラメータ: 
  - user_id: ユーザーID
進捗内容: 既存ユーザーの情報更新

ユーザー削除（本部）
機能名: ユーザー削除（本部）
画面 (React Route): /admin/users/{u_id}/delete
API (Spring REST): DELETE /api/admin/manager/users/{user_id}
HTTPメソッド: DELETE
権限: 本部管理者
パスパラメータ: 
  - user_id: ユーザーID
進捗内容: ユーザーアカウントの削除

4. 店長: ユーザーアカウント管理
ユーザー一覧（店長）
機能名: ユーザー一覧（店長）
画面 (React Route): /manager/users
API (Spring REST): GET /api/stores/{store_id}/manager/users
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: 全ユーザーアカウントの一覧取得（検索/フィルタリング可）

ユーザー検索（店長）
機能名: ユーザー検索（店長）
画面 (React Route): /manager/users
API (Spring REST): GET /api/stores/{store_id}/manager/users?name={keyword}
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
クエリパラメータ: 
  - name: 検索キーワード
進捗内容: ユーザー名での検索

ユーザー詳細（店長）
機能名: ユーザー詳細（店長）
画面 (React Route): /manager/users/{u_id}/detail
API (Spring REST): GET /api/stores/{store_id}/manager/users/{user_id}
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - user_id: ユーザーID
進捗内容: 特定ユーザー情報の取得

ユーザー作成（店長）
機能名: ユーザー作成（店長）
画面 (React Route): /manager/users/create
API (Spring REST): POST /api/stores/{store_id}/manager/users
HTTPメソッド: POST
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: ユーザーアカウントの新規作成

ユーザー情報更新（店長）
機能名: ユーザー情報更新（店長）
画面 (React Route): /manager/users/{u_id}/edit
API (Spring REST): PATCH /api/stores/{store_id}/manager/users/{user_id}
HTTPメソッド: PATCH
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - user_id: ユーザーID
進捗内容: 既存ユーザー情報の更新（権限変更など）

ユーザー削除（店長）
機能名: ユーザー削除（店長）
画面 (React Route): /manager/users/{u_id}/delete
API (Spring REST): DELETE /api/stores/{store_id}/manager/users/{user_id}
HTTPメソッド: DELETE
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - user_id: ユーザーID
進捗内容: ユーザーアカウントの削除

5. 本部: 顧客管理
顧客一覧（本部）
機能名: 顧客一覧（本部）
画面 (React Route): /admin/customers
API (Spring REST): GET /api/admin/customers
HTTPメソッド: GET
権限: 本部管理者
進捗内容: 顧客一覧取得

顧客検索（本部）
機能名: 顧客検索（本部）
画面 (React Route): /admin/customers
API (Spring REST): GET /api/admin/customers?name={keyword}
HTTPメソッド: GET
権限: 本部管理者
クエリパラメータ: 
  - name: 検索キーワード
進捗内容: 顧客名による検索

顧客無効化（本部）
機能名: 顧客無効化（本部）
画面 (React Route): /admin/customers/{c_id}/disable
API (Spring REST): PATCH /api/admin/customers/{customer_id}/disable
HTTPメソッド: PATCH
権限: 本部管理者
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客の無効化

顧客有効化（本部）
機能名: 顧客有効化（本部）
画面 (React Route): /admin/customers/{c_id}/enable
API (Spring REST): PATCH /api/admin/customers/{customer_id}/enable
HTTPメソッド: PATCH
権限: 本部管理者
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客の再有効化

顧客新規作成（本部）
機能名: 顧客新規作成（本部）
画面 (React Route): /admin/customers/create
API (Spring REST): POST /api/admin/customers
HTTPメソッド: POST
権限: 本部管理者
進捗内容: 顧客の新規登録

顧客削除（本部）
機能名: 顧客削除（本部）
画面 (React Route): /admin/customers/delete
API (Spring REST): DELETE /api/admin/customers/{customer_id}
HTTPメソッド: DELETE
権限: 本部管理者
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客の削除

6. 店長: 顧客管理
顧客一覧（店長）
機能名: 顧客一覧（店長）
画面 (React Route): /manager/customers
API (Spring REST): GET /api/stores/{store_id}/manager/customers
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: 顧客一覧取得(店舗内管轄)

顧客検索（店長）
機能名: 顧客検索（店長）
画面 (React Route): /manager/customers
API (Spring REST): GET /api/stores/{store_id}/manager/customers?name={keyword}
HTTPメソッド: GET
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
クエリパラメータ: 
  - name: 検索キーワード
進捗内容: 顧客名による検索(店舗内管轄)

顧客無効化（店長）
機能名: 顧客無効化（店長）
画面 (React Route): /manager/customers/{c_id}/disable
API (Spring REST): PATCH /api/stores/{store_id}/manager/customers/{customer_id}/disable
HTTPメソッド: PATCH
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - customer_id: 顧客ID
進捗内容: 顧客の無効化(店舗内管轄)

顧客有効化（店長）
機能名: 顧客有効化（店長）
画面 (React Route): /manager/customers/{c_id}/enable
API (Spring REST): PATCH /api/stores/{store_id}/manager/customers/{customer_id}/enable
HTTPメソッド: PATCH
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - customer_id: 顧客ID
進捗内容: 顧客の再有効化(店舗内管轄)

顧客新規作成（店長）
機能名: 顧客新規作成（店長）
画面 (React Route): /manager/customers/create
API (Spring REST): POST /api/stores/{store_id}/manager/customers
HTTPメソッド: POST
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: 顧客の新規登録(店舗内管轄)

顧客削除（店長）
機能名: 顧客削除（店長）
画面 (React Route): /manager/customers/delete
API (Spring REST): DELETE /api/stores/{store_id}/manager/customers/{customer_id}
HTTPメソッド: DELETE
権限: 店長
パスパラメータ: 
  - store_id: 店舗ID
  - customer_id: 顧客ID
進捗内容: 顧客の削除(店舗内管轄)

7. トレーナー: 顧客管理（閲覧のみ）
顧客一覧（トレーナー）
機能名: 顧客一覧（トレーナー）
画面 (React Route): /trainer/customers
API (Spring REST): GET /api/stores/{store_id}/trainers/customers
HTTPメソッド: GET
権限: トレーナー
パスパラメータ: 
  - store_id: 店舗ID
進捗内容: 担当顧客の一覧取得

顧客プロフィール閲覧
機能名: 顧客プロフィール閲覧
画面 (React Route): /customer/{c_id}
API (Spring REST): GET /api/customers/{customer_id}/profile
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客の基本プロフィール情報取得

顧客プロフィール編集
機能名: 顧客プロフィール編集
画面 (React Route): /customer/{c_id}/edit
API (Spring REST): PATCH /api/customers/{customer_id}/profile
HTTPメソッド: PATCH
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客プロフィールの更新

8. レッスン記録・履歴
レッスン新規入力
機能名: レッスン新規入力
画面 (React Route): /customer/{c_id}/lesson/new
API (Spring REST): POST /api/customers/{customer_id}/lessons
HTTPメソッド: POST
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 新しいレッスン記録の作成

姿勢画像グループ作成（新規レッスン入力に準拠）
機能名: 姿勢画像グループ作成（新規レッスン入力に準拠）
画面 (React Route): 同上(内部処理)
API (Spring REST): POST /api/lessons/{l_id}/posture_groups
HTTPメソッド: POST
権限: 共通
パスパラメータ: 
  - l_id: レッスンID
進捗内容: 新しいレッスン記録に伴う、新しい姿勢画像群に対する型グループの作成

姿勢画像アップロード
機能名: 姿勢画像アップロード
画面 (React Route): /customer/{c_id}/lesson/{l_id}/posture
API (Spring REST): POST /api/lessons/{lesson_id}/posture_groups/{group_id}/images
HTTPメソッド: POST
権限: 共通
パスパラメータ: 
  - lesson_id: レッスンID
  - group_id: グループID
進捗内容: 各方向(front / right / back /left)の画像撮影/追加

レッスン履歴一覧
機能名: レッスン履歴一覧
画面 (React Route): /customer/{c_id}/lessons
API (Spring REST): GET /api/customers/{customer_id}/lessons
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 顧客のレッスン履歴一覧（ページネーション/フィルタリング）

レッスン履歴詳細閲覧
機能名: レッスン履歴詳細閲覧
画面 (React Route): /lesson/{l_id}
API (Spring REST): GET /api/lessons/{lesson_id}
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - lesson_id: レッスンID
進捗内容: 特定のレッスン記録の詳細情報取得

レッスン履歴詳細編集
機能名: レッスン履歴詳細編集
画面 (React Route): /lesson/{l_id}/edit
API (Spring REST): PATCH /api/lessons/{lesson_id}
HTTPメソッド: PATCH
権限: トレーナー
パスパラメータ: 
  - lesson_id: レッスンID
進捗内容: 既存レッスン情報の編集

9. 体重/BMI履歴（グラフ）
体重/BMI履歴（グラフ）
機能名: 体重/BMI履歴（グラフ）
画面 (React Route): /customer/{c_id}/vitals
API (Spring REST): GET /api/customers/{customer_id}/vitals/history
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 体重・BMIデータ履歴の時系列データ取得

10. 姿勢画像管理
姿勢画像一覧（グループ取得）
機能名: 姿勢画像一覧（グループ取得）
画面 (React Route): /customer/{c_id}/posture_groups
API (Spring REST): GET /api/customers/{customer_id}/posture_groups
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - customer_id: 顧客ID
進捗内容: 姿勢画像一覧の取得

姿勢画像削除
機能名: 姿勢画像削除
画面 (React Route): /customer/{c_id}/posture/{img_id}/delete
API (Spring REST): DELETE /api/posture_images/{img_id}
HTTPメソッド: DELETE
権限: 共通
パスパラメータ: 
  - img_id: 画像ID
進捗内容: 姿勢画像の削除

姿勢画像比較
機能名: 姿勢画像比較
画面 (React Route): /customer/{c_id}/posture/compare
API (Spring REST): GET /api/customers/{c_id}/posture_groups
HTTPメソッド: GET
権限: 共通
パスパラメータ: 
  - c_id: 顧客ID
進捗内容: 姿勢画像2枚を左右に配列し、比較する

11. 監査ログ
監査ログの取得
機能名: 監査ログの取得
画面 (React Route): /api/admin/logs
API (Spring REST): GET /api/admin/logs
HTTPメソッド: GET
権限: 本部管理者
進捗内容: 監査ログの閲覧