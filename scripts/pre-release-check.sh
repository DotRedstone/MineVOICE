#!/usr/bin/env bash
set -e

echo "=== MineVOICE Pre-Release Safety Check ==="

# 1. 检查是否存在未提交的代码
if [ -n "$(git status --porcelain)" ]; then
    echo "⚠️  WARNING: You have uncommitted changes in your working tree."
fi

# 2. 检查意外提交的构建产物
echo "🔍 Checking for tracked build artifacts..."
TRACKED_ARTIFACTS=$(git ls-files "*.jar" "*.class" "build/" ".gradle/" "*/build/" | grep -v "gradle-wrapper.jar" || true)
if [ -n "$TRACKED_ARTIFACTS" ]; then
    echo "❌ ERROR: Found build artifacts tracked in git:"
    echo "$TRACKED_ARTIFACTS"
    exit 1
fi

# 3. 检查敏感词 (secrets, tokens)
echo "🔍 Checking for hardcoded secrets/tokens..."
# 排除文档、脚本文档和以示例为目的的内容
SECRET_MATCHES=$(git grep -inE "(secret|token|password|apikey)[_ ]*=[_ ]*[\"'][^\"']+[\"']" -- ':(exclude)*.md' ':(exclude)docs/*' ':(exclude)scripts/*' ':(exclude)*/example/*' || true)

if [ -n "$SECRET_MATCHES" ]; then
    echo "⚠️  WARNING: Potential hardcoded secrets found:"
    echo "$SECRET_MATCHES"
    echo "Please verify these are not real credentials before releasing."
    # 我们暂不退出 1，因为可能是合法的测试 token，但需要人为确认
fi

echo "✅ All automated safety checks completed."
