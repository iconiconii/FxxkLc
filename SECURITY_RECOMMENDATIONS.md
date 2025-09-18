# AI 推荐系统安全配置建议

## 已修复的安全问题

### ✅ 1. GET /problems/ai-recommendations 用户隔离
- **问题**: 原接口允许通过 `userId` 参数访问其他用户的推荐
- **修复**: 完全移除 `userId` 参数，强制使用当前登录用户ID
- **影响**: 100% 防止跨用户数据泄露

### ✅ 2. 展示数据优化  
- **问题**: AI推荐返回"Problem <id>"等占位符数据
- **修复**: 集成 `ProblemMapper.selectBatchIds()` 批量获取真实题目数据
- **优化**: 
  - 真实题目标题替换占位符
  - 基于难度的智能时间估算 (Easy: 20min, Medium: 30min, Hard: 45min)
  - 批量查询避免N+1性能问题

## 生产环境安全配置

### 必须设置的配置项

```yaml
# application-prod.yml
app:
  security:
    ai-recs-public: false  # ⚠️ 必须设为 false，确保需要认证
    
spring:
  security:
    require-ssl: true
    
llm:
  enabled: true
  # 生产环境API密钥通过环境变量注入
  openai:
    apiKeyEnv: OPENAI_API_KEY
  azure:
    apiKeyEnv: AZURE_OPENAI_KEY
```

### 环境变量安全设置

```bash
# 生产环境必须设置
export OPENAI_API_KEY="sk-..."
export AZURE_OPENAI_KEY="..."
export JWT_SECRET="生产级256位密钥"

# CORS 白名单 - 禁止使用通配符
export CORS_ALLOWED_ORIGINS="https://codetop.cc,https://www.codetop.cc"
```

## 运行时安全验证

### API 端点安全检查
- ✅ `GET /problems/ai-recommendations` - 无法指定userId参数
- ✅ `POST /problems/{id}/recommendation-feedback` - 用户ID校验和覆盖
- ✅ 所有推荐接口需要有效JWT token

### 数据隔离验证
- ✅ 推荐结果仅基于当前用户的FSRS数据
- ✅ 用户画像和学习历史完全隔离
- ✅ 缓存Key包含用户ID，防止缓存污染

## 监控建议

### 安全监控指标
- 未认证请求数量 (`security.unauthenticated_requests`)
- 跨用户访问尝试 (`security.cross_user_attempts`) 
- API限流触发频率 (`llm.rate_limit_hits`)

### 日志记录
- 所有推荐请求包含 `traceId` 和 `userId`
- LLM调用包含链路追踪信息
- 敏感数据已脱敏处理

## 应急响应

### 如发现安全问题
1. 立即检查 `app.security.ai-recs-public` 配置
2. 验证JWT token验证是否正常工作  
3. 检查CORS配置是否过于宽松
4. 查看最近的跨用户访问日志

### 降级方案
- 紧急情况下可设置 `llm.enabled=false` 回退到FSRS推荐
- 或设置更严格的限流参数临时限制访问

---

**重要**: 此文档应定期更新，确保安全配置与代码修复保持同步。