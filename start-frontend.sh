#!/bin/bash

# =============================================================================
# CodeTop FSRS Frontend 启动脚本
# =============================================================================
# 该脚本启动前端开发服务器（Next.js）
# 
# 使用方法：
#   chmod +x start-frontend.sh
#   ./start-frontend.sh
# =============================================================================

set -e

echo "🚀 正在启动 CodeTop FSRS Frontend..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查Node.js环境
echo -e "${BLUE}📋 检查Node.js环境...${NC}"
if ! command -v node &> /dev/null; then
    echo -e "${RED}❌ Node.js未安装，请先安装Node.js 18.17+或20+${NC}"
    exit 1
fi

NODE_VERSION=$(node -v | cut -d'v' -f2 | cut -d'.' -f1)
if [ "$NODE_VERSION" -lt 18 ]; then
    echo -e "${RED}❌ Node.js版本过低，需要18.17+或20+，当前版本：$(node -v)${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Node.js环境检查通过：$(node -v)${NC}"

# 检查npm环境
echo -e "${BLUE}📋 检查npm环境...${NC}"
if ! command -v npm &> /dev/null; then
    echo -e "${RED}❌ npm未安装${NC}"
    exit 1
fi

echo -e "${GREEN}✅ npm环境检查通过：$(npm -v)${NC}"

# 进入前端目录
echo -e "${BLUE}📂 进入前端目录...${NC}"
cd frontend

# 检查package.json是否存在
if [ ! -f "package.json" ]; then
    echo -e "${RED}❌ package.json文件不存在${NC}"
    exit 1
fi

# 检查node_modules是否存在，如果不存在则安装依赖
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}📦 node_modules不存在，正在安装依赖...${NC}"
    npm install
    
    if [ $? -ne 0 ]; then
        echo -e "${RED}❌ 依赖安装失败${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}✅ 依赖安装成功${NC}"
else
    echo -e "${GREEN}✅ 依赖已存在${NC}"
fi

# 检查后端是否在运行
echo -e "${BLUE}📋 检查后端服务状态...${NC}"
if curl -s http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✅ 后端服务已运行${NC}"
else
    echo -e "${YELLOW}⚠️  后端服务未运行，请先启动后端服务${NC}"
    echo -e "${YELLOW}   执行: ./start-backend.sh${NC}"
fi

# 启动开发服务器
echo -e "${BLUE}🚀 启动Next.js开发服务器...${NC}"
echo -e "${YELLOW}📝 应用启动后可访问：${NC}"
echo -e "${YELLOW}   - 前端应用: http://localhost:3000${NC}"
echo -e "${YELLOW}   - 测试账号: 2441933762@qq.com${NC}"
echo -e "${YELLOW}   - 测试密码: password123_${NC}"
echo -e "${YELLOW}📝 按 Ctrl+C 停止应用${NC}"
echo ""

# 启动开发服务器
npm run dev