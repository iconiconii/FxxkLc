# 技术审计与改进建议（缓存一致性 / 安全 / 业务一致性 / 运维）

本报告基于当前仓库的代码与配置，梳理关键技术风险并提供可落地的改进方案与示例。聚焦四类问题：缓存一致性、安全、业务逻辑与数据一致性、运维与可观测性，并附带优先级执行清单。

## 总览
- 后端：Spring Boot + MyBatis-Plus + Redis + MongoDB（笔记内容），采用 JWT 鉴权与自研限流；存在多种缓存用法并存与失效策略不统一的问题。
- 前端：Next.js（App Router），令牌存储在 localStorage，存在 XSS 下令牌泄露风险。
- 运维：Nginx 反代 + docker-compose；部分安全头与配置可进一步加固；dev/prod 配置存在踩坑点（如 Redis URL）。

---

## 一、缓存一致性

### 问题与风险
- 多套缓存实现并存：
  - 统一抽象（`CacheHelper` + `CacheService` + `CacheKeyBuilder`）与直接 `RedisTemplate` 操作并存（如 `CodeTopFilterService`），TTL、Key、失效策略分散，易错漏和行为不一致。
- 失效时机“提交前删除”带来的脏写风险：
  - `@TransactionalEventListener(BEFORE_COMMIT)` + `REQUIRES_NEW` 先删缓存、后提交数据库。在删与提交之间若发生读，将读取旧DB并把旧值回填缓存，导致短期脏缓存。
- 批量删除用 `KEYS pattern`：
  - `redisTemplate.keys("pattern")` 在生产有阻塞与性能风险。
- MyBatis 二级缓存与 Redis 并存：
  - `mybatis-plus.configuration.cache-enabled: true` 与手动 Redis 缓存并用，若无联动失效，易残留旧值。
- Key 命名不统一：
  - 需强制所有缓存/失效通过 `CacheKeyBuilder` 生成，避免出现诸如 `codetop:codetop:*` 的非预期前缀扩散。

### 改进建议
- 统一缓存入口
  - 将直接 `redisTemplate.opsForValue()` 的服务（如 `CodeTopFilterService`）迁移到 `CacheHelper.cacheOrCompute` 与 `CacheService`，TTL 统一从 `CacheConfiguration` 读取。
- 调整失效时机与策略
  - 将大多数业务事件监听改为 `AFTER_COMMIT`；对热点数据采用“双删”（提交后立即删 + 延迟再删一次）或“逻辑过期/版本号”策略降低竞态。
  - 示例（事件监听调整）：
    ```java
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(ProblemEvent e) { /* 删除缓存 */ }
    ```
- 替换 KEYS 为 SCAN / 集合索引
  - 高危 `keys(pattern)` 改为 `SCAN` 游标遍历或维护“域索引集合”（写入时登记，域失效时批量删集合成员）。
  - SCAN 示例（伪代码）：
    ```java
    RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
    try (Cursor<byte[]> cursor = conn.scan(ScanOptions.scanOptions().match(pattern).count(1000).build())) {
      while (cursor.hasNext()) { keys.add(new String(cursor.next(), StandardCharsets.UTF_8)); }
    }
    redisTemplate.delete(keys);
    ```
- 关闭 MyBatis 二级缓存或完善联动
  - 推荐默认关闭：`mybatis-plus.configuration.cache-enabled: false`，统一依赖 Redis 缓存与显式失效。
- 规范 Key 构造
  - 所有 set/get/evict 统一使用 `CacheKeyBuilder`；统一命名空间与参数清洗，避免键过长与非法字符。

---

## 二、安全（后端 / 前端 / 反向代理）

### 问题与风险
- 令牌存储在 localStorage（前端）
  - `frontend/lib/auth-api.ts` 将 access/refresh token 存 localStorage，XSS 下可被窃取。
- CSRF 完全禁用
  - `SecurityConfig` 全局 `csrf().disable()`。JWT 场景可以，但若未来切换 Cookie 鉴权或有跨域写接口，需要更严格边界。
- 真实 IP 获取可信度不足
  - `RateLimitFilter` 优先取 `X-Forwarded-For` 首个IP；易被伪造，影响限流与审计。
- Redis PROD 配置不当
  - `application.yml` 使用 `spring.data.redis.host: ${REDIS_URL}`，而 compose 传 `REDIS_URL=redis://redis:6379`，host 字段不接收 URL，可能导致线上连接失败。
- 接口暴露与安全头
  - Nginx 未设置 CSP；Swagger 在 prod 暴露风险（应关闭或加鉴权/IP 白名单）。
- Druid 监控（dev）默认口令 `admin/admin`
  - 虽 prod 关闭，但 dev 若暴露公网亦有风险。

### 改进建议
- 使用 HttpOnly + Secure + SameSite=strict Cookie 存储访问令牌
  - 后端 `Set-Cookie`（`HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=...`），前端移除 localStorage 依赖，`fetch` 时 `credentials: 'include'`；CORS 允许凭证且白名单精确。
- 严格可信代理链
  - 后端启用 `ForwardedHeaderFilter` 并在反代层仅注入 `X-Real-IP`；服务端优先读取已信任代理写入的头，忽略客户端自带 XFF。
- 修正 Redis 配置
  - 使用 `spring.data.redis.url: ${REDIS_URL}`（如 `redis://:password@redis:6379/0`）或拆分 host/port。
- 强化安全头与接口暴露策略
  - Nginx 增加 CSP，例如：`Content-Security-Policy: default-src 'self'; img-src 'self' https: data:; ...`。
  - 生产关闭 `/swagger-ui`、`/api-docs` 或加鉴权/IP 白名单。
- Druid 监控仅限内网/开发
  - dev 改强口令/来源IP限制，prod 保持关闭。

---

## 三、业务逻辑与数据一致性

### 问题与风险
- MySQL + MongoDB 写入“伪事务”
  - `ProblemNoteService` 在单个 `@Transactional` 中同时写 MySQL 与 Mongo，但无法跨库原子；易出现部分成功。
- 幂等结果序列化不稳
  - `IdempotencyService.completeIdempotent` 使用 `result.toString()` 写入 Redis；复杂对象/国际化文本/JSON 容易不一致或丢失。
- 大量按模式批删缓存
  - 用户/FSRS/筛选/排行榜等多域使用通配符批删，容易高峰抖动与误删。

### 改进建议
- 采用最终一致性（Outbox + 消费者）或补偿重试
  - 写主库成功后发布域事件（AFTER_COMMIT），由异步消费者持久化 Mongo 内容；失败可重试/对账任务修复。
- 幂等结果统一 JSON 序列化
  - 使用 `ObjectMapper`（带类型信息或外层封装）持久化结果；结果 TTL 与幂等窗口一致，避免长期污染。
- 精准失效 + 域索引集合
  - 缩小批删范围；对用户/题目/过滤等域维护“域→key 集合”，按实体维度精准删，避免全域扫/删。

---

## 四、运维与可观测性

### 问题与风险
- Session 与 JWT 并存
  - `spring.session.store-type=redis` 与 `STATELESS` 冲突；若无会话需求应关闭，避免资源浪费与错配。
- 日志与隐私
  - 仓库已存在大量日志文件；日志包含 `clientIp/userAgent/username` 等，需注意脱敏与留存策略。
- 速率限制头部硬编码
  - `RateLimitFilter` 将 `X-RateLimit-Limit` 写死为 `100`，建议从配置读取，便于环境差异化。

### 改进建议
- 明确是否需要 Session
  - 若仅 JWT，无服务端会话状态，关闭 `spring.session` 相关配置。
- 日志治理
  - 避免日志提交；必要时历史重写；对 PII（如邮箱、IP）做掩码；强化 MDC 贯穿与追踪头透传。
- 限流配置化
  - 统一从 `RateLimitConfig.RateLimitProperties` 读取并设置响应头。

---

## 五、测试与质量保障

- 安全/鉴权/限流集成测试
  - 覆盖未认证、权限不足、令牌过期/黑名单、CORS 凭证与来源白名单、登录/刷新/退出闭环。
- 缓存一致性回归
  - 针对 AFTER_COMMIT + 双删/SCAN 改造增加回归测试，验证“无旧值回填”。
- E2E
  - Playwright 场景补充：令牌 Cookie 化后跨页持久、会话过期交互、受保护路由重定向等。

---

## 六、优先级执行清单（建议顺序）

1) 缓存一致性
- [ ] 统一缓存入口：将 `CodeTopFilterService` 等直接 Redis 使用改为 `CacheHelper/CacheService`，TTL 走 `CacheConfiguration`。
- [ ] 事件监听改为 `AFTER_COMMIT`；为热点数据加“延迟双删”（提交后再次删除，或投递延迟任务）。
- [ ] 将所有 `keys(pattern)` 替换为 SCAN 或域索引集合批删。
- [ ] 关闭 MyBatis 二级缓存或补充联动失效策略。

2) 安全
- [ ] 令牌改为 HttpOnly Cookie；前端移除 localStorage 令牌依赖，`fetch` 开启 `credentials: 'include'`；CORS 调整为白名单 + allow-credentials。
- [ ] 启用 `ForwardedHeaderFilter`；后端仅信任受控代理头（优先 `X-Real-IP`），忽略客户端自带 XFF。
- [ ] 修正 Redis PROD：改用 `spring.data.redis.url: ${REDIS_URL}` 或拆分 host/port。
- [ ] Nginx 增加 CSP；生产关闭 Swagger 或加鉴权/IP 白名单；dev Druid 加强保护。

3) 业务一致性
- [ ] 为笔记 MySQL→Mongo 写入落地 Outbox + 异步消费者 或补偿重试；事件发布改 AFTER_COMMIT。
- [ ] 幂等结果统一 JSON 序列化，稳定 Key 构造与过期策略。
- [ ] 精准失效与域索引集合，减少大范围通配符批删。

4) 运维
- [ ] 明确是否保留 `spring.session`；如无需求则关闭。
- [ ] 日志治理：杜绝日志提交、脱敏、追踪头统一。
- [ ] 限流头部读取配置，移除硬编码。

---

## 七、配置与代码示例片段

- Redis（prod）配置修正（`application.yml`）
  ```yaml
  spring:
    data:
      redis:
        url: ${REDIS_URL}  # 例如 redis://:password@redis:6379/0
  ```

- 事件监听改为提交后删除
  ```java
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onProblemChanged(ProblemEvent e) {
      cacheInvalidationStrategy.invalidateProblemCachesSync();
      // 可选：调度延迟再次删除，缓解竞态回写
  }
  ```

- SCAN 替代 KEYS（简化示例）
  ```java
  ScanOptions options = ScanOptions.scanOptions().match(pattern).count(1000).build();
  RedisConnection conn = redisTemplate.getConnectionFactory().getConnection();
  Set<String> keys = new HashSet<>();
  try (Cursor<byte[]> c = conn.scan(options)) {
    while (c.hasNext()) keys.add(new String(c.next(), StandardCharsets.UTF_8));
  }
  if (!keys.isEmpty()) redisTemplate.delete(keys);
  ```

- 幂等结果 JSON 序列化
  ```java
  String json = objectMapper.writeValueAsString(result);
  redisTemplate.opsForValue().set(resultKey, json, expireSeconds, TimeUnit.SECONDS);
  ```

- Nginx 增加 CSP（示例）
  ```nginx
  add_header Content-Security-Policy "default-src 'self'; img-src 'self' https: data:; script-src 'self'; style-src 'self' 'unsafe-inline'; connect-src 'self' https:; frame-ancestors 'none'" always;
  ```

---

## 八、关联文件（供实施参考）
- 缓存：
  - `src/main/java/com/codetop/util/CacheHelper.java`
  - `src/main/java/com/codetop/service/cache/*`
  - `src/main/java/com/codetop/service/CacheKeyBuilder.java`
  - `src/main/java/com/codetop/service/CacheInvalidationManager.java`
  - `src/main/java/com/codetop/service/CacheInvalidationStrategy.java`
  - `src/main/java/com/codetop/service/CodeTopFilterService.java`
- 安全：
  - `src/main/java/com/codetop/config/SecurityConfig.java`
  - `src/main/java/com/codetop/security/*`
  - `src/main/resources/application.yml`（Redis、CORS、profiles）
  - `nginx/nginx.conf`、`nginx/conf.d/default.conf`
  - 前端：`frontend/lib/auth-api.ts`、`frontend/lib/api.ts`
- 业务一致性：
  - `src/main/java/com/codetop/service/ProblemNoteService.java`（MySQL+Mongo）
  - `src/main/java/com/codetop/service/IdempotencyService.java`、`src/main/java/com/codetop/aspect/IdempotentAspect.java`

---

## 九、后续工作与里程碑

- 里程碑 A（1 周）：缓存统一与 KEYS→SCAN 改造、事件 AFTER_COMMIT、限流与真实 IP 信任链修正、Redis 配置修正。
- 里程碑 B（1–2 周）：令牌 Cookie 化（前后端联调）、CSP 与 Swagger 策略、MyBatis 二级缓存策略调整。
- 里程碑 C（2–3 周）：MySQL→Mongo 最终一致性（Outbox + 消费者）落地、幂等结果JSON化、精准失效/索引集合。
- 里程碑 D（持续）：测试完善（安全/缓存/E2E）、日志与可观测性治理。

> 如需，我可以基于此文档逐步提交 PR：先做“缓存统一 + KEYS→SCAN + AFTER_COMMIT + Redis 配置修正”的小步快跑改造。

