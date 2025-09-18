# CodeTop FSRS 项目启动指南

## 🚀 快速启动

### 一键启动所有服务
```bash
./start-all.sh
```

### 分步启动
```bash
# 1. 只启动容器
./start-all.sh --containers-only

# 2. 只启动后端（包含容器）
./start-all.sh --backend-only

# 3. 只启动前端
./start-all.sh --frontend-only
```

### 单独启动
```bash
# 启动后端
./start-backend.sh

# 启动前端
./start-frontend.sh
```

### 停止所有服务
```bash
./stop-all.sh
```

## 📋 服务信息

### 容器服务
- **MySQL**: `localhost:3307`
  - 用户名: `root`
  - 密码: `root`
  - 数据库: `codetop_fsrs`

- **Redis**: `localhost:6380`
  - 无密码

- **MongoDB**: `localhost:27018`
  - 用户名: `root`
  - 密码: `root`
  - 数据库: `codetop_notes_dev` (开发环境)

- **Mongo Express**: `http://localhost:8081`
  - 用户名: `admin`
  - 密码: `admin`

### 应用服务
- **后端API**: `http://localhost:8080`
  - Swagger文档: `http://localhost:8080/api/v1/swagger-ui.html`
  - 健康检查: `http://localhost:8080/api/v1/actuator/health`
  - Druid监控: `http://localhost:8080/api/v1/druid/`

- **前端应用**: `http://localhost:3000`

### 测试账号
- **邮箱**: `2441933762@qq.com`
- **密码**: `password123_`

## 🔧 环境配置

### DeepSeek API
- **API Key**: `sk-a77b899053c8497f9d8b5320b5339969`
- **Base URL**: `https://api.deepseek.com/v1`
- **Model**: `deepseek-chat`

### 开发环境
所有环境变量已在 `.env.dev` 中配置，启动脚本会自动加载。

## 📝 使用说明

1. **首次启动**: 使用 `./start-all.sh` 一键启动所有服务
2. **开发调试**: 
   - 后端日志: `tail -f backend.log`
   - 前端在终端直接显示日志
3. **停止服务**: 使用 `./stop-all.sh` 或按 `Ctrl+C`

## 🚨 故障排查

### 常见问题

1. **端口被占用**
   ```bash
   # 检查端口占用
   lsof -i :8080  # 后端端口
   lsof -i :3000  # 前端端口
   lsof -i :3307  # MySQL端口
   ```

2. **容器启动失败**
   ```bash
   # 查看容器状态
   docker ps -a --filter "name=codetop"
   
   # 查看容器日志
   docker logs codetop-mysql
   docker logs codetop-redis
   docker logs codetop-mongodb
   ```

3. **数据库连接失败**
   ```bash
   # 测试MySQL连接
   docker exec codetop-mysql mysql -u root -proot -e "SHOW DATABASES;"
   
   # 测试Redis连接
   docker exec codetop-redis redis-cli ping
   
   # 测试MongoDB连接
   docker exec codetop-mongodb mongosh --eval "db.adminCommand('ping')"
   ```

### 重置环境
```bash
# 停止所有服务
./stop-all.sh

# 清理容器和数据
docker compose down -v

# 重新启动
./start-all.sh
```

## 📦 脚本说明

- `start-all.sh`: 一键启动脚本，支持参数控制启动内容
- `start-backend.sh`: 后端启动脚本，包含环境检查和数据库连接测试
- `start-frontend.sh`: 前端启动脚本，自动安装依赖
- `stop-all.sh`: 停止所有服务的脚本
- `docker-compose.yml`: 容器配置文件
- `.env.dev`: 开发环境配置文件