#!/bin/bash

# =============================================================================
# CodeTop FSRS 项目一键启动脚本
# =============================================================================
# 该脚本会按顺序启动：
# 1. Docker容器 (MySQL, Redis, MongoDB)
# 2. 后端Spring Boot应用
# 3. 前端Next.js应用
# 
# 使用方法：
#   chmod +x start-all.sh
#   ./start-all.sh
# 
# 可选参数：
#   --backend-only    只启动后端
#   --frontend-only   只启动前端
#   --containers-only 只启动容器
# =============================================================================

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# 默认启动选项
START_CONTAINERS=true
START_BACKEND=true
START_FRONTEND=true

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    case $1 in
        --backend-only)
            START_CONTAINERS=true
            START_BACKEND=true
            START_FRONTEND=false
            shift
            ;;
        --frontend-only)
            START_CONTAINERS=false
            START_BACKEND=false
            START_FRONTEND=true
            shift
            ;;
        --containers-only)
            START_CONTAINERS=true
            START_BACKEND=false
            START_FRONTEND=false
            shift
            ;;
        -h|--help)
            echo "用法: $0 [选项]"
            echo "选项:"
            echo "  --backend-only    只启动容器和后端"
            echo "  --frontend-only   只启动前端"
            echo "  --containers-only 只启动容器"
            echo "  -h, --help        显示帮助信息"
            exit 0
            ;;
        *)
            echo -e "${RED}❌ 未知参数: $1${NC}"
            echo "使用 $0 --help 查看帮助"
            exit 1
            ;;
    esac
done

echo -e "${PURPLE}🚀 CodeTop FSRS 项目一键启动脚本${NC}"
echo -e "${PURPLE}================================================${NC}"

# 检查必要工具
check_tool() {
    if ! command -v $1 &> /dev/null; then
        echo -e "${RED}❌ $1 未安装，请先安装${NC}"
        exit 1
    fi
}

echo -e "${BLUE}📋 检查必要工具...${NC}"
check_tool docker
if [ "$START_BACKEND" = true ]; then
    check_tool java
    check_tool mvn
fi
if [ "$START_FRONTEND" = true ]; then
    check_tool node
    check_tool npm
fi
echo -e "${GREEN}✅ 工具检查完成${NC}"

# 启动容器
if [ "$START_CONTAINERS" = true ]; then
    echo -e "${CYAN}🐳 启动Docker容器...${NC}"
    echo -e "${BLUE}   启动MySQL, Redis, MongoDB容器...${NC}"
    
    docker compose up -d
    
    echo -e "${BLUE}⏳ 等待容器启动完成...${NC}"
    sleep 10
    
    # 检查容器状态
    echo -e "${BLUE}📋 检查容器状态...${NC}"
    docker ps --filter "name=codetop" --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
    
    # 等待数据库就绪
    echo -e "${BLUE}⏳ 等待数据库就绪...${NC}"
    
    echo -e "${BLUE}   等待MySQL就绪...${NC}"
    until docker exec codetop-mysql mysqladmin ping -h localhost --silent 2>/dev/null; do
        echo -e "${YELLOW}   ⏳ MySQL启动中...${NC}"
        sleep 2
    done
    echo -e "${GREEN}   ✅ MySQL就绪${NC}"
    
    echo -e "${BLUE}   等待Redis就绪...${NC}"
    until docker exec codetop-redis redis-cli ping 2>/dev/null | grep -q PONG; do
        echo -e "${YELLOW}   ⏳ Redis启动中...${NC}"
        sleep 2
    done
    echo -e "${GREEN}   ✅ Redis就绪${NC}"
    
    echo -e "${BLUE}   等待MongoDB就绪...${NC}"
    until docker exec codetop-mongodb mongosh --eval "db.adminCommand('ping')" --quiet 2>/dev/null; do
        echo -e "${YELLOW}   ⏳ MongoDB启动中...${NC}"
        sleep 2
    done
    echo -e "${GREEN}   ✅ MongoDB就绪${NC}"
    
    echo -e "${GREEN}✅ 所有容器启动完成${NC}"
fi

# 启动后端
if [ "$START_BACKEND" = true ]; then
    echo -e "${CYAN}⚙️  启动后端服务...${NC}"
    
    # 设置执行权限
    chmod +x start-backend.sh
    
    if [ "$START_FRONTEND" = true ]; then
        echo -e "${YELLOW}📝 后端将在后台启动，日志输出到 backend.log${NC}"
        echo -e "${YELLOW}📝 可使用 'tail -f backend.log' 查看后端日志${NC}"
        
        # 后台启动后端
        nohup ./start-backend.sh > backend.log 2>&1 &
        BACKEND_PID=$!
        echo $BACKEND_PID > backend.pid
        
        echo -e "${BLUE}⏳ 等待后端启动...${NC}"
        
        # 等待后端就绪
        for i in {1..60}; do
            if curl -s http://localhost:8080/api/v1/actuator/health > /dev/null 2>&1; then
                echo -e "${GREEN}✅ 后端服务已启动 (PID: $BACKEND_PID)${NC}"
                break
            fi
            if [ $i -eq 60 ]; then
                echo -e "${RED}❌ 后端启动超时${NC}"
                exit 1
            fi
            echo -e "${YELLOW}   ⏳ 等待后端启动 ($i/60)...${NC}"
            sleep 2
        done
    else
        echo -e "${YELLOW}📝 只启动后端，前端不启动${NC}"
        exec ./start-backend.sh
    fi
fi

# 启动前端
if [ "$START_FRONTEND" = true ]; then
    echo -e "${CYAN}🌐 启动前端服务...${NC}"
    
    # 设置执行权限
    chmod +x start-frontend.sh
    
    echo -e "${YELLOW}📝 前端开发服务器将启动...${NC}"
    exec ./start-frontend.sh
fi

# 如果只启动容器
if [ "$START_CONTAINERS" = true ] && [ "$START_BACKEND" = false ] && [ "$START_FRONTEND" = false ]; then
    echo -e "${GREEN}✅ 容器启动完成${NC}"
    echo -e "${YELLOW}📝 可用服务：${NC}"
    echo -e "${YELLOW}   - MySQL: localhost:3307${NC}"
    echo -e "${YELLOW}   - Redis: localhost:6380${NC}"
    echo -e "${YELLOW}   - MongoDB: localhost:27018${NC}"
    echo -e "${YELLOW}   - Mongo Express: http://localhost:8081${NC}"
fi