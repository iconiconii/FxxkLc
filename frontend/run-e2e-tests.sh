#!/bin/bash

# E2E测试运行脚本
# CodeTop FSRS 项目端到端测试

set -e

echo "🚀 开始 E2E 测试..."

# 检查环境
echo "📋 检查测试环境..."

# 检查Node.js
if ! command -v node &> /dev/null; then
    echo "❌ Node.js 未安装"
    exit 1
fi

# 检查npm
if ! command -v npm &> /dev/null; then
    echo "❌ npm 未安装"
    exit 1
fi

echo "✅ Node.js 环境检查完成"

# 检查后端服务
echo "🔍 检查后端服务 (localhost:8080)..."
if curl -f -s http://localhost:8080/api/v1/actuator/health > /dev/null; then
    echo "✅ 后端服务正常运行"
else
    echo "⚠️  警告: 后端服务可能未运行"
    echo "请确保运行: mvn spring-boot:run"
fi

# 检查前端开发服务器
echo "🔍 检查前端服务 (localhost:3000)..."
if curl -f -s http://localhost:3000 > /dev/null; then
    echo "✅ 前端服务正常运行"
else
    echo "⚠️  警告: 前端服务可能未运行"
    echo "请确保运行: npm run dev"
fi

# 安装依赖
echo "📦 检查依赖..."
if [ ! -d "node_modules" ] || [ ! -d "node_modules/@playwright" ]; then
    echo "📦 安装依赖..."
    npm install
fi

# 检查Playwright浏览器
echo "🌐 检查 Playwright 浏览器..."
if [ ! -d "~/.cache/ms-playwright" ]; then
    echo "📥 安装 Playwright 浏览器..."
    npx playwright install chromium
fi

# 运行测试
echo "🧪 运行 E2E 测试..."

# 设置环境变量
export BASE_URL=http://localhost:3000
export CI=false

# 运行测试的函数
run_test() {
    local test_file=$1
    local test_name=$2
    
    echo "🔬 运行测试: $test_name"
    
    if npx playwright test "$test_file" --project=chromium --reporter=list; then
        echo "✅ $test_name 测试通过"
        return 0
    else
        echo "❌ $test_name 测试失败"
        return 1
    fi
}

# 运行基础冒烟测试
echo "🏃 开始基础功能测试..."

failed_tests=0

# 1. 基础冒烟测试
if run_test "tests/e2e/simple-smoke.spec.ts" "基础冒烟测试"; then
    echo "✅ 基础冒烟测试通过"
else
    ((failed_tests++))
fi

# 如果基础测试通过，继续运行其他测试
if [ $failed_tests -eq 0 ]; then
    echo "🎯 基础测试通过，运行完整测试套件..."
    
    # 2. 认证功能测试
    if run_test "tests/e2e/auth.spec.ts" "认证功能测试"; then
        echo "✅ 认证功能测试通过"
    else
        ((failed_tests++))
    fi
    
    # 3. 笔记功能测试
    if run_test "tests/e2e/notes.spec.ts" "笔记功能测试"; then
        echo "✅ 笔记功能测试通过"
    else
        ((failed_tests++))
    fi
    
    # 4. 导航功能测试
    if run_test "tests/e2e/navigation.spec.ts" "导航功能测试"; then
        echo "✅ 导航功能测试通过"
    else
        ((failed_tests++))
    fi
else
    echo "⚠️  基础测试失败，跳过其他测试"
fi

# 生成测试报告
echo "📊 生成测试报告..."
if [ -d "playwright-report" ]; then
    echo "📄 测试报告已生成: playwright-report/index.html"
    echo "🔗 查看报告: npx playwright show-report"
fi

# 测试结果总结
echo ""
echo "📋 测试结果总结:"
echo "════════════════════"

if [ $failed_tests -eq 0 ]; then
    echo "🎉 所有测试通过！"
    echo "✅ 应用程序功能正常"
    exit 0
else
    echo "❌ $failed_tests 个测试失败"
    echo "💡 查看详细报告: npx playwright show-report"
    echo "🔧 调试建议:"
    echo "  1. 检查后端和前端服务是否正常运行"
    echo "  2. 确认测试账号 2441933762@qq.com 存在且密码正确"
    echo "  3. 查看测试截图和录制视频: test-results/"
    exit 1
fi