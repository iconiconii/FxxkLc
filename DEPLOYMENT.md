# CodeTop FSRS 项目部署指南

## 云服务器部署完整方案

本指南将帮助您在云服务器上部署 CodeTop FSRS 项目，包含完整的 Spring Boot 后端、Next.js 前端、数据库和反向代理配置。

## 服务器要求

### 最低配置
- **CPU**: 2核心
- **内存**: 4GB RAM
- **存储**: 40GB SSD
- **网络**: 5Mbps带宽
- **操作系统**: Ubuntu 20.04+ / CentOS 8+ / Debian 11+

### 推荐配置
- **CPU**: 4核心
- **内存**: 8GB RAM
- **存储**: 80GB SSD
- **网络**: 10Mbps带宽

## 部署步骤

### 1. 准备服务器环境

```bash
# 更新系统
sudo apt update && sudo apt upgrade -y

# 安装必要工具
sudo apt install -y curl wget git vim

# 安装 Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# 安装 Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 重新登录以应用用户组变更
logout
```

### 2. 克隆项目代码

```bash
# 克隆项目到服务器
git clone <your-repository-url> codetop-fsrs
cd codetop-fsrs
```

### 3. 配置环境变量

```bash
# 复制环境变量模板
cp .env.prod .env

# 编辑环境变量（重要！）
vim .env

# 必须修改的重要配置：
# - JWT_SECRET: 设置为256位以上的安全密钥
# - MYSQL_PASSWORD: 设置MySQL密码
# - REDIS_PASSWORD: 设置Redis密码
# - MONGODB_ROOT_PASSWORD: 设置MongoDB密码
# - CORS_ALLOWED_ORIGINS: 设置为你的域名
# - DOMAIN: 设置你的域名（如果使用HTTPS）
# - SSL_EMAIL: 设置用于SSL证书的邮箱
```

### 4. 配置域名和防火墙

#### 配置域名解析
在你的域名服务商管理面板中，添加 A 记录：
```
@ → 你的服务器IP
www → 你的服务器IP
```

#### 配置防火墙
```bash
# Ubuntu/Debian
sudo ufw allow 22/tcp   # SSH
sudo ufw allow 80/tcp   # HTTP
sudo ufw allow 443/tcp  # HTTPS
sudo ufw enable

# CentOS/RHEL
sudo firewall-cmd --permanent --add-service=ssh
sudo firewall-cmd --permanent --add-service=http
sudo firewall-cmd --permanent --add-service=https
sudo firewall-cmd --reload
```

### 5. 部署应用

使用提供的部署脚本进行一键部署：

```bash
# 启动所有服务
./deploy.sh start

# 或者手动执行步骤：
# 1. 检查配置
./deploy.sh health

# 2. 生成SSL证书（可选，用于HTTPS）
./deploy.sh ssl

# 3. 查看日志
./deploy.sh logs
```

### 6. 验证部署

访问以下地址验证部署是否成功：

- **前端应用**: `http://your-domain.com` 或 `http://server-ip:3000`
- **后端API**: `http://your-domain.com/api/v1` 或 `http://server-ip:8080/api/v1`
- **API文档**: `http://your-domain.com/api/v1/swagger-ui.html`
- **健康检查**: `http://your-domain.com/api/v1/actuator/health`

## 部署架构

```
[Internet] → [Nginx:80/443] → [Frontend:3000] + [Backend:8080]
                                     ↓
                    [MySQL:3306] + [Redis:6379] + [MongoDB:27017]
```

### 服务组件

| 服务 | 端口 | 描述 |
|------|------|------|
| Nginx | 80, 443 | 反向代理和SSL终端 |
| Frontend | 3000 | Next.js前端应用 |
| Backend | 8080 | Spring Boot后端API |
| MySQL | 3306 | 主数据库 |
| Redis | 6379 | 缓存和会话存储 |
| MongoDB | 27017 | 笔记数据存储 |

## 管理命令

### 基本操作
```bash
# 启动所有服务
./deploy.sh start

# 停止所有服务
./deploy.sh stop

# 重启所有服务
./deploy.sh restart

# 查看实时日志
./deploy.sh logs

# 查看特定服务日志
./deploy.sh logs backend
./deploy.sh logs frontend
./deploy.sh logs nginx
```

### 维护操作
```bash
# 检查服务健康状态
./deploy.sh health

# 更新应用（从Git拉取最新代码并重新部署）
./deploy.sh update

# 备份数据
./deploy.sh backup

# 清理未使用的Docker资源
./deploy.sh cleanup

# 生成SSL证书
./deploy.sh ssl
```

## SSL/HTTPS 配置

### 使用 Let's Encrypt（推荐）

1. 确保域名已正确解析到服务器IP
2. 在 `.env` 文件中设置 `DOMAIN` 和 `SSL_EMAIL`
3. 运行：`./deploy.sh ssl`

### 手动配置证书

将证书文件放置在 `ssl/` 目录下：
```bash
ssl/
├── fullchain.pem  # 完整证书链
└── privkey.pem    # 私钥
```

## 性能优化

### 系统层面优化

```bash
# 调整系统参数
echo "vm.max_map_count=262144" | sudo tee -a /etc/sysctl.conf
echo "net.core.somaxconn=65535" | sudo tee -a /etc/sysctl.conf
sudo sysctl -p

# 优化文件描述符限制
echo "* soft nofile 65535" | sudo tee -a /etc/security/limits.conf
echo "* hard nofile 65535" | sudo tee -a /etc/security/limits.conf
```

### 应用层面优化

1. **数据库连接池**：已在 `application.yml` 中配置
2. **Redis缓存**：已配置多层缓存策略
3. **JVM参数**：在 `Dockerfile.backend` 中已优化
4. **Nginx缓存**：静态资源已配置缓存

## 监控和维护

### 日志管理
```bash
# 查看各服务日志
docker logs codetop-backend
docker logs codetop-frontend
docker logs codetop-nginx
docker logs codetop-mysql
docker logs codetop-redis
docker logs codetop-mongodb

# 日志轮转配置（防止日志过大）
sudo logrotate -f /etc/logrotate.conf
```

### 数据备份
```bash
# 自动备份（建议设置定时任务）
./deploy.sh backup

# 手动备份MySQL
docker exec codetop-mysql mysqldump -u root -p codetop_fsrs > backup.sql

# 恢复数据库
docker exec -i codetop-mysql mysql -u root -p codetop_fsrs < backup.sql
```

### 定时任务设置
```bash
# 编辑crontab
crontab -e

# 添加以下任务
# 每天凌晨2点备份数据
0 2 * * * cd /path/to/codetop-fsrs && ./deploy.sh backup

# 每周日凌晨3点清理Docker资源
0 3 * * 0 cd /path/to/codetop-fsrs && ./deploy.sh cleanup

# 每天检查SSL证书有效期（Let's Encrypt自动续期）
0 1 * * * certbot renew --quiet && docker-compose -f docker-compose.prod.yml restart nginx
```

## 故障排除

### 常见问题

#### 1. 服务无法启动
```bash
# 检查端口占用
sudo netstat -tlnp | grep :80
sudo netstat -tlnp | grep :443

# 检查Docker服务状态
sudo systemctl status docker

# 查看详细错误日志
./deploy.sh logs
```

#### 2. 数据库连接失败
```bash
# 检查MySQL容器状态
docker ps | grep mysql

# 检查MySQL日志
docker logs codetop-mysql

# 测试数据库连接
docker exec codetop-mysql mysql -u root -p -e "SHOW DATABASES;"
```

#### 3. 前端无法访问后端API
```bash
# 检查Nginx配置
docker exec codetop-nginx nginx -t

# 重新加载Nginx配置
docker exec codetop-nginx nginx -s reload

# 检查网络连接
docker network ls
docker network inspect codetop-fsrs_codetop-network
```

#### 4. SSL证书问题
```bash
# 检查证书文件
ls -la ssl/

# 测试证书有效性
openssl x509 -in ssl/fullchain.pem -text -noout

# 重新生成证书
./deploy.sh ssl
```

## 安全建议

### 基础安全措施
1. **定期更新系统和软件包**
2. **使用强密码和密钥认证**
3. **配置防火墙规则**
4. **启用日志监控**
5. **定期备份数据**

### 应用安全配置
1. **JWT密钥**：使用256位以上的随机密钥
2. **数据库密码**：使用复杂密码
3. **CORS配置**：严格限制允许的域名
4. **API限流**：已配置请求频率限制
5. **HTTPS**：在生产环境中强制使用HTTPS

## 性能监控

### 推荐监控工具
- **系统监控**: htop, iotop, nload
- **Docker监控**: docker stats, ctop
- **应用监控**: Spring Boot Actuator
- **日志聚合**: ELK Stack (可选)

### 关键指标监控
- CPU和内存使用率
- 磁盘空间使用情况
- 网络带宽使用
- 数据库连接数
- API响应时间
- 错误率统计

## 扩展部署

### 水平扩展
当单服务器无法满足需求时，可以考虑：
1. **负载均衡**：使用多个后端实例
2. **数据库读写分离**：主从复制
3. **缓存集群**：Redis Cluster
4. **CDN加速**：静态资源分发

### 容器编排
对于更复杂的部署需求，可以考虑使用：
- **Kubernetes**：大规模容器编排
- **Docker Swarm**：简单的集群管理
- **云服务**：AWS ECS, Google GKE, 阿里云容器服务

---

## 快速开始

如果您已经有了云服务器并配置好了域名，可以使用以下命令快速部署：

```bash
# 1. 克隆项目
git clone <your-repo> codetop-fsrs && cd codetop-fsrs

# 2. 配置环境变量
cp .env.prod .env && vim .env

# 3. 一键部署
./deploy.sh start

# 4. 生成SSL证书（可选）
./deploy.sh ssl
```

部署完成后，您就可以通过域名访问您的 CodeTop FSRS 应用了！