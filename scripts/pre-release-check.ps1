$ErrorActionPreference = "Stop"

Write-Host "=== MineVOICE Pre-Release Safety Check ===" -ForegroundColor Cyan

# 1. 检查是否存在未提交的代码
$gitStatus = git status --porcelain
if ($gitStatus) {
    Write-Host "WARNING: You have uncommitted changes in your working tree." -ForegroundColor Yellow
}

# 2. 检查意外提交的构建产物
Write-Host "🔍 Checking for tracked build artifacts..."
$trackedArtifacts = git ls-files "*.jar" "*.class" "build/" ".gradle/" "*/build/" | Where-Object { $_ -notmatch "gradle-wrapper\.jar" }
if ($trackedArtifacts) {
    Write-Host "ERROR: Found build artifacts tracked in git:" -ForegroundColor Red
    Write-Host $trackedArtifacts
    exit 1
}

# 3. 检查敏感词 (secrets, tokens)
Write-Host "🔍 Checking for hardcoded secrets/tokens..."
# 使用 git grep 查找可能被硬编码的 secret、token 等
$secretMatches = git grep -inE "(secret|token|password|apikey)[_ ]*=[_ ]*[`"`'][^`"`']+[`"`']" -- ':(exclude)*.md' ':(exclude)docs/*' ':(exclude)scripts/*' ':(exclude)*/example/*'
if ($secretMatches) {
    Write-Host "WARNING: Potential hardcoded secrets found:" -ForegroundColor Yellow
    Write-Host $secretMatches
    Write-Host "Please verify these are not real credentials before releasing." -ForegroundColor Yellow
}

Write-Host "✅ All automated safety checks completed." -ForegroundColor Green
