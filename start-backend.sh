#!/bin/bash

# =============================================================================
# CodeTop FSRS Backend 启动脚本
# =============================================================================
# 该脚本启动后端服务，跳过测试，使用开发环境配置
# 
# 使用方法：
#   chmod +x start-backend.sh
#   ./start-backend.sh
# =============================================================================

set -e

echo "🚀 正在启动 CodeTop FSRS Backend..."

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 检查Java环境
echo -e "${BLUE}📋 检查Java环境...${NC}"
if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java未安装，请先安装Java 17或更高版本${NC}"
    exit 1
fi

JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}❌ Java版本过低，需要Java 17或更高版本，当前版本：$JAVA_VERSION${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Java环境检查通过${NC}"

# 检查Maven环境
echo -e "${BLUE}📋 检查Maven环境...${NC}"
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven未安装，请先安装Maven${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Maven环境检查通过${NC}"

# 检查Docker容器状态
echo -e "${BLUE}📋 检查数据库容器状态...${NC}"
MYSQL_STATUS=$(docker ps --filter "name=codetop-mysql" --format "{{.Status}}" | head -1)
REDIS_STATUS=$(docker ps --filter "name=codetop-redis" --format "{{.Status}}" | head -1)
MONGODB_STATUS=$(docker ps --filter "name=codetop-mongodb" --format "{{.Status}}" | head -1)

if [[ ! "$MYSQL_STATUS" =~ "Up" ]]; then
    echo -e "${YELLOW}⚠️  MySQL容器未运行，正在启动...${NC}"
    docker compose up -d mysql
    echo -e "${BLUE}⏳ 等待MySQL容器启动...${NC}"
    sleep 10
fi

if [[ ! "$REDIS_STATUS" =~ "Up" ]]; then
    echo -e "${YELLOW}⚠️  Redis容器未运行，正在启动...${NC}"
    docker compose up -d redis
    echo -e "${BLUE}⏳ 等待Redis容器启动...${NC}"
    sleep 5
fi

if [[ ! "$MONGODB_STATUS" =~ "Up" ]]; then
    echo -e "${YELLOW}⚠️  MongoDB容器未运行，正在启动...${NC}"
    docker compose up -d mongodb
    echo -e "${BLUE}⏳ 等待MongoDB容器启动...${NC}"
    sleep 10
fi

echo -e "${GREEN}✅ 所有数据库容器已启动${NC}"

# 设置环境变量
echo -e "${BLUE}📋 设置环境变量...${NC}"
export SPRING_PROFILES_ACTIVE=dev
export JWT_SECRET=development_secret_key_minimum_256_bits_long_for_security_requirements_2024
export JWT_EXPIRATION=86400000
export JWT_REFRESH_EXPIRATION=604800000
export CORS_ALLOWED_ORIGINS=http://localhost:3000,http://127.0.0.1:3000
export RATE_LIMIT_ENABLED=false
export DB_USERNAME=root
export DB_PASSWORD=root
export REDIS_HOST=localhost
export REDIS_PORT=6380
export SPRING_DATA_MONGODB_URI=mongodb://root:root@localhost:27018/codetop_notes_dev?authSource=admin
export LLM_ENABLED=true
export LLM_BASE_URL=https://api.deepseek.com/v1
export LLM_MODEL=deepseek-chat
export DEEPSEEK_API_KEY=sk-a77b899053c8497f9d8b5320b5339969
export DRUID_SECURITY_ENABLED=false
export PROMETHEUS_ENABLED=true

echo -e "${GREEN}✅ 环境变量设置完成${NC}"

# 等待数据库就绪
echo -e "${BLUE}⏳ 等待数据库就绪...${NC}"
sleep 5

# 检查数据库连接
echo -e "${BLUE}📋 检查MySQL连接...${NC}"
until docker exec codetop-mysql mysqladmin ping -h localhost --silent; do
    echo -e "${YELLOW}⏳ 等待MySQL启动...${NC}"
    sleep 2
done
echo -e "${GREEN}✅ MySQL连接成功${NC}"

echo -e "${BLUE}📋 检查Redis连接...${NC}"
until docker exec codetop-redis redis-cli ping | grep -q PONG; do
    echo -e "${YELLOW}⏳ 等待Redis启动...${NC}"
    sleep 2
done
echo -e "${GREEN}✅ Redis连接成功${NC}"

# 编译项目（跳过测试）
echo -e "${BLUE}🔧 编译项目（跳过测试）...${NC}"
mvn clean compile -DskipTests -q

if [ $? -ne 0 ]; then
    echo -e "${RED}❌ 项目编译失败${NC}"
    exit 1
fi

echo -e "${GREEN}✅ 项目编译成功${NC}"

# 启动应用
echo -e "${BLUE}🚀 启动Spring Boot应用...${NC}"
echo -e "${YELLOW}📝 应用启动后可访问：${NC}"
echo -e "${YELLOW}   - API文档: http://localhost:8080/api/v1/swagger-ui.html${NC}"
echo -e "${YELLOW}   - 健康检查: http://localhost:8080/api/v1/actuator/health${NC}"
echo -e "${YELLOW}   - Druid监控: http://localhost:8080/api/v1/druid/${NC}"
echo -e "${YELLOW}📝 按 Ctrl+C 停止应用${NC}"
echo ""

# 启动应用（跳过测试）
mvn spring-boot:run -Dspring-boot.run.profiles=dev -DskipTests