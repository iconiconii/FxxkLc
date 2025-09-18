#!/bin/bash

# =============================================================================
# CodeTop FSRS 项目停止脚本
# =============================================================================
# 该脚本会停止所有运行的服务：
# 1. 停止前端Next.js应用
# 2. 停止后端Spring Boot应用
# 3. 停止Docker容器
# 
# 使用方法：
#   chmod +x stop-all.sh
#   ./stop-all.sh
# =============================================================================

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

echo -e "${PURPLE}🛑 CodeTop FSRS 项目停止脚本${NC}"
echo -e "${PURPLE}===============================================${NC}"

# 停止后端进程
if [ -f "backend.pid" ]; then
    BACKEND_PID=$(cat backend.pid)
    echo -e "${BLUE}🛑 停止后端服务 (PID: $BACKEND_PID)...${NC}"
    
    if kill -0 $BACKEND_PID 2>/dev/null; then
        kill $BACKEND_PID
        echo -e "${GREEN}✅ 后端服务已停止${NC}"
    else
        echo -e "${YELLOW}⚠️  后端进程不存在${NC}"
    fi
    
    rm -f backend.pid
else
    echo -e "${YELLOW}⚠️  未找到后端PID文件${NC}"
fi

# 停止可能运行的Java进程（Spring Boot应用）
echo -e "${BLUE}🛑 检查并停止Spring Boot进程...${NC}"
JAVA_PIDS=$(pgrep -f "spring-boot:run" || true)
if [ ! -z "$JAVA_PIDS" ]; then
    echo -e "${BLUE}   发现Spring Boot进程: $JAVA_PIDS${NC}"
    kill $JAVA_PIDS
    echo -e "${GREEN}✅ Spring Boot进程已停止${NC}"
else
    echo -e "${YELLOW}⚠️  未发现运行中的Spring Boot进程${NC}"
fi

# 停止可能运行的Node.js进程（Next.js应用）
echo -e "${BLUE}🛑 检查并停止Next.js进程...${NC}"
NODE_PIDS=$(pgrep -f "next dev" || true)
if [ ! -z "$NODE_PIDS" ]; then
    echo -e "${BLUE}   发现Next.js进程: $NODE_PIDS${NC}"
    kill $NODE_PIDS
    echo -e "${GREEN}✅ Next.js进程已停止${NC}"
else
    echo -e "${YELLOW}⚠️  未发现运行中的Next.js进程${NC}"
fi

# 停止Docker容器
echo -e "${BLUE}🛑 停止Docker容器...${NC}"
if docker ps --filter "name=codetop" --format "{{.Names}}" | grep -q codetop; then
    docker compose down
    echo -e "${GREEN}✅ Docker容器已停止${NC}"
else
    echo -e "${YELLOW}⚠️  未发现运行中的codetop容器${NC}"
fi

# 清理临时文件
echo -e "${BLUE}🧹 清理临时文件...${NC}"
rm -f backend.log backend.pid

echo -e "${GREEN}✅ 所有服务已停止${NC}"
echo -e "${YELLOW}📝 如需重新启动，请执行: ./start-all.sh${NC}"