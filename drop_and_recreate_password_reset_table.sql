-- -- パスワードリセットリクエストテーブル再作成スクリプト
-- -- 実行方法: Supabaseダッシュボードの SQL Editor で実行してください
-- -- 注意: 既存のデータは削除されます

-- -- 既存のテーブルを削除（存在する場合）
-- DROP TABLE IF EXISTS password_reset_requests CASCADE;

-- -- 既存のENUM型を削除（存在する場合）
-- DROP TYPE IF EXISTS password_reset_status CASCADE;

-- -- パスワードリセットステータスENUMを作成
-- -- 注意: ENUM値は小文字で定義します（Javaのcode値と一致させる）
-- CREATE TYPE password_reset_status AS ENUM ('pending', 'approved', 'rejected');

-- -- パスワードリセットリクエストテーブルを作成
-- CREATE TABLE password_reset_requests (
--     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
--     email VARCHAR(255) NOT NULL,
--     name VARCHAR(100) NOT NULL,
--     user_id UUID REFERENCES users(id),
--     status password_reset_status NOT NULL DEFAULT 'pending',
--     requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
--     processed_at TIMESTAMPTZ,
--     processed_by_user_id UUID REFERENCES users(id),
--     note VARCHAR(1000)
-- );

-- -- インデックスを作成（パフォーマンス最適化）
-- CREATE INDEX idx_password_reset_requests_email ON password_reset_requests(email);
-- CREATE INDEX idx_password_reset_requests_status ON password_reset_requests(status);
-- CREATE INDEX idx_password_reset_requests_requested_at ON password_reset_requests(requested_at DESC);

-- -- コメントを追加
-- COMMENT ON TABLE password_reset_requests IS 'パスワードリセットリクエスト';
-- COMMENT ON COLUMN password_reset_requests.id IS 'リクエストID';
-- COMMENT ON COLUMN password_reset_requests.email IS 'リクエスト者のメールアドレス';
-- COMMENT ON COLUMN password_reset_requests.name IS 'リクエスト者の申告名';
-- COMMENT ON COLUMN password_reset_requests.user_id IS '照合されたユーザーID（承認時に設定）';
-- COMMENT ON COLUMN password_reset_requests.status IS 'リクエスト状態（pending/approved/rejected）';
-- COMMENT ON COLUMN password_reset_requests.requested_at IS 'リクエスト日時';
-- COMMENT ON COLUMN password_reset_requests.processed_at IS '処理日時';
-- COMMENT ON COLUMN password_reset_requests.processed_by_user_id IS '処理した管理者ID';
-- COMMENT ON COLUMN password_reset_requests.note IS '管理者メモ（任意）';
