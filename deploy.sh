#!/bin/bash

# CodeTop FSRS 项目部署脚本
# 用法: ./deploy.sh [start|stop|restart|logs|update]

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 配置
PROJECT_NAME="codetop-fsrs"
COMPOSE_FILE="docker-compose.prod.yml"
ENV_FILE=".env.prod"

# 日志函数
log() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] $1${NC}"
}

warn() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] WARNING: $1${NC}"
}

error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ERROR: $1${NC}"
    exit 1
}

# 检查依赖
check_dependencies() {
    log "检查系统依赖..."
    
    if ! command -v docker &> /dev/null; then
        error "Docker 未安装，请先安装 Docker"
    fi
    
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        error "Docker Compose 未安装，请先安装 Docker Compose"
    fi
    
    log "依赖检查完成"
}

# 检查环境变量
check_env() {
    log "检查环境变量配置..."
    
    if [ ! -f "$ENV_FILE" ]; then
        error "环境变量文件 $ENV_FILE 不存在，请先复制并配置"
    fi
    
    # 检查必需的环境变量
    source "$ENV_FILE"
    
    if [ -z "$JWT_SECRET" ] || [ ${#JWT_SECRET} -lt 32 ]; then
        error "JWT_SECRET 未设置或长度不足32位"
    fi
    
    if [ -z "$MYSQL_PASSWORD" ]; then
        warn "MYSQL_PASSWORD 未设置，将使用默认值"
    fi
    
    if [ -z "$CORS_ALLOWED_ORIGINS" ]; then
        warn "CORS_ALLOWED_ORIGINS 未设置，请确保在生产环境中设置正确的域名"
    fi
    
    log "环境变量检查完成"
}

# 准备部署环境
prepare_deployment() {
    log "准备部署环境..."
    
    # 创建必要的目录
    mkdir -p logs/nginx
    mkdir -p ssl
    mkdir -p mysql-config
    mkdir -p redis-config
    mkdir -p mongo-init
    mkdir -p init-scripts
    
    # 设置权限
    chmod +x deploy.sh
    
    log "部署环境准备完成"
}

# 构建和启动服务
start_services() {
    log "构建并启动服务..."
    
    # 停止现有服务（如果存在）
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" down 2>/dev/null || true
    
    # 构建镜像
    log "构建应用镜像..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" build --no-cache
    
    # 启动服务
    log "启动服务..."
    docker-compose -f "$COMPOSE_FILE" --env-file "$ENV_FILE" up -d
    
    # 等待服务启动
    log "等待服务启动..."
    sleep 30
    
    # 检查服务状态
    check_health
    
    log "服务启动完成！"
    show_access_info
}

# 停止服务
stop_services() {
    log "停止服务..."
    docker-compose -f "$COMPOSE_FILE" down
    log "服务已停止"
}

# 重启服务
restart_services() {
    log "重启服务..."
    stop_services
    start_services
}

# 查看日志
show_logs() {
    if [ -z "$2" ]; then
        docker-compose -f "$COMPOSE_FILE" logs -f
    else
        docker-compose -f "$COMPOSE_FILE" logs -f "$2"
    fi
}

# 更新应用
update_app() {
    log "更新应用..."
    
    # 拉取最新代码
    if [ -d ".git" ]; then
        git pull origin main
    else
        warn "不是Git仓库，跳过代码更新"
    fi
    
    # 重新构建和部署
    start_services
}

# 健康检查
check_health() {
    log "检查服务健康状态..."
    
    services=("mysql" "redis" "mongodb" "backend" "frontend" "nginx")
    
    for service in "${services[@]}"; do
        status=$(docker-compose -f "$COMPOSE_FILE" ps -q "$service" | xargs docker inspect -f '{{.State.Health.Status}}' 2>/dev/null || echo "no-health-check")
        
        if [ "$status" = "healthy" ] || [ "$status" = "no-health-check" ]; then
            log "✓ $service 服务运行正常"
        else
            warn "✗ $service 服务状态: $status"
        fi
    done
}

# 显示访问信息
show_access_info() {
    log "部署成功！访问信息："
    echo ""
    echo "🌐 前端应用: http://localhost:3000"
    echo "🔧 后端API: http://localhost:8080/api/v1"
    echo "📊 Swagger文档: http://localhost:8080/api/v1/swagger-ui.html"
    echo "❤️ 健康检查: http://localhost:8080/api/v1/actuator/health"
    echo ""
    log "注意：请确保防火墙开放相应端口，并配置域名解析（如果使用域名访问）"
}

# 生成SSL证书（使用Let's Encrypt）
generate_ssl() {
    log "生成SSL证书..."
    
    if [ -z "$DOMAIN" ]; then
        error "请在 $ENV_FILE 中设置 DOMAIN 变量"
    fi
    
    if [ -z "$SSL_EMAIL" ]; then
        error "请在 $ENV_FILE 中设置 SSL_EMAIL 变量"
    fi
    
    # 安装certbot
    if ! command -v certbot &> /dev/null; then
        log "安装certbot..."
        apt-get update && apt-get install -y certbot
    fi
    
    # 生成证书
    certbot certonly --standalone -d "$DOMAIN" -d "www.$DOMAIN" --email "$SSL_EMAIL" --agree-tos --no-eff-email
    
    # 复制证书到项目目录
    cp "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ssl/
    cp "/etc/letsencrypt/live/$DOMAIN/privkey.pem" ssl/
    
    log "SSL证书生成完成"
}

# 备份数据
backup_data() {
    log "备份数据..."
    
    BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
    mkdir -p "$BACKUP_DIR"
    
    # 备份MySQL
    docker exec codetop-mysql mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" codetop_fsrs > "$BACKUP_DIR/mysql_backup.sql"
    
    # 备份Redis
    docker exec codetop-redis redis-cli --rdb "$BACKUP_DIR/redis_backup.rdb"
    
    # 备份MongoDB
    docker exec codetop-mongodb mongodump --out "$BACKUP_DIR/mongodb_backup"
    
    log "数据备份完成，保存在: $BACKUP_DIR"
}

# 清理资源
cleanup() {
    log "清理未使用的Docker资源..."
    docker system prune -f
    docker volume prune -f
    log "清理完成"
}

# 主函数
main() {
    case "${1:-start}" in
        "start")
            check_dependencies
            check_env
            prepare_deployment
            start_services
            ;;
        "stop")
            stop_services
            ;;
        "restart")
            restart_services
            ;;
        "logs")
            show_logs "$@"
            ;;
        "update")
            update_app
            ;;
        "health")
            check_health
            ;;
        "ssl")
            source "$ENV_FILE"
            generate_ssl
            ;;
        "backup")
            source "$ENV_FILE"
            backup_data
            ;;
        "cleanup")
            cleanup
            ;;
        "help"|"--help"|"-h")
            echo "用法: $0 [start|stop|restart|logs|update|health|ssl|backup|cleanup|help]"
            echo ""
            echo "命令说明:"
            echo "  start     启动所有服务（默认）"
            echo "  stop      停止所有服务"
            echo "  restart   重启所有服务"
            echo "  logs      查看日志 [服务名]"
            echo "  update    更新并重新部署"
            echo "  health    检查服务健康状态"
            echo "  ssl       生成SSL证书"
            echo "  backup    备份数据"
            echo "  cleanup   清理Docker资源"
            echo "  help      显示帮助信息"
            ;;
        *)
            error "未知命令: $1，使用 '$0 help' 查看帮助"
            ;;
    esac
}

# 执行主函数
main "$@"