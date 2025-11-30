# 最初のユーザー（Admin）作成手順

最初の管理者ユーザーは、ログイン前に作成する必要があるため、Supabaseで直接作成します。

## 手順

### 1. パスワードハッシュを生成

1. Eclipseで `PasswordHashGenerator.java` を開く
   - パス: `src/test/java/com/example/fitnessgym_mg/PasswordHashGenerator.java`
   
2. 使用したいパスワードを設定
   - コード内の `passwords` 配列を編集
   - 例: `new String[]{"yourPassword123"}`

3. 実行してハッシュ値を取得
   - Eclipseで右クリック → 「実行」→ 「Java アプリケーション」
   - またはエディタ上部の実行ボタン（緑の再生ボタン）をクリック

4. コンソールに表示されたハッシュ値をコピー
   - 例: `$2a$10$xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 2. Supabaseでユーザーを作成

1. Supabaseダッシュボードにアクセス
   - https://supabase.com/dashboard

2. プロジェクトを選択

3. 左メニューから「Table Editor」を選択

4. `users` テーブルを開く

5. 「Insert row」をクリック

6. 以下の情報を入力:

   | カラム名 | 値の例 | 備考 |
   |---------|--------|------|
   | `id` | (空欄) | UUIDは自動生成されます。または `gen_random_uuid()` を使用 |
   | `email` | `admin@example.com` | メールアドレス（UNIQUE制約あり） |
   | `name` | `管理者` | 氏名 |
   | `kana` | `カンリシャ` | フリガナ |
   | `pass` | `$2a$10$...` | ステップ1で生成したBCryptハッシュ値を貼り付け |
   | `role` | `admin` | 小文字で `admin`, `manager`, `trainer` のいずれか |
   | `is_active` | `true` | チェックボックスをON |
   | `created_at` | (現在時刻) | 現在時刻を設定（例: `now()` または `CURRENT_TIMESTAMP`） |

7. 「Save」をクリック

### 3. 確認

1. 作成されたユーザーが `users` テーブルに表示されることを確認

2. アプリケーションでログインできることを確認
   - メールアドレス: 入力したメールアドレス
   - パスワード: ステップ1で使用したパスワード（ハッシュ化前の元のパスワード）

## 重要な注意点

### role の値
- 必ず**小文字**で入力: `admin`, `manager`, `trainer`
- 大文字や全角は使用しない

### pass の値
- **必ずBCryptハッシュ値**を使用（平文のパスワードは不可）
- ハッシュ値は `$2a$10$...` のように `$` で始まる60文字程度の文字列

### created_at の値
- PostgreSQLの関数を使用: `now()` または `CURRENT_TIMESTAMP`
- または、手動でタイムスタンプを入力（例: `2025-01-20 12:00:00+09`）

### id の値
- UUIDはSupabaseが自動生成してくれます
- または、`gen_random_uuid()` 関数を使用

## トラブルシューティング

### ログインできない場合
1. `pass` カラムに正しいBCryptハッシュ値が設定されているか確認
2. `is_active` が `true` になっているか確認
3. `role` が小文字の `admin` になっているか確認
4. メールアドレスが正しく入力されているか確認

### エラーが発生する場合
- `email` のUNIQUE制約違反: 既に同じメールアドレスのユーザーが存在
- `role` の値が不正: `admin`, `manager`, `trainer` のいずれかを使用
- `pass` がNULL: BCryptハッシュ値を必ず設定

## 次のステップ

最初の管理者ユーザーでログイン後、アプリケーションのユーザー管理画面から追加のユーザーを作成できます:
- `/admin/users` - 本部管理者用ユーザー管理画面
- `/manager/{storeId}/users` - 店長用ユーザー管理画面

