#!/bin/bash
# BCryptパスワードハッシュ生成スクリプト

PASSWORD="${1:-Password123}"

echo "========================================"
echo "=== BCrypt Password Hash Generator ==="
echo "========================================"
echo ""
echo "Password: $PASSWORD"
echo ""

# Gradleを使ってPasswordHashGeneratorを実行
cd "$(dirname "$0")"
./gradlew -q --console=plain -PmainClass=com.example.fitnessgym_mg.PasswordHashGenerator run --args="$PASSWORD" 2>&1 | grep -A 50 "BCrypt\|Hash\|Password" || echo "実行に失敗しました。IDEでPasswordHashGenerator.javaを直接実行してください。"

