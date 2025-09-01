# 缓存系统迁移计划 - @Cacheable → RedisTemplate

## 📋 迁移概述

将项目中的 Spring Cache `@Cacheable` 注解替换为手动 `RedisTemplate` 缓存管理，以获得更精细的缓存控制和更好的性能监控能力。

## 🎯 迁移目标

- ✅ 完全移除 `@Cacheable`、`@CacheEvict`、`@CachePut` 等注解
- ✅ 统一使用 `RedisTemplate` 进行缓存操作
- ✅ 提供统一的缓存抽象层和工具类
- ✅ 保持现有缓存逻辑和 TTL 配置不变
- ✅ 增强缓存操作的可监控性和调试能力

## 📊 现状分析

### 当前使用 @Cacheable 的服务和方法

#### 1. AuthService (1个方法)
- [ ] `getUserCacheDTOById(Long userId)` - `codetop-user-profile` 缓存

#### 2. FSRSService (3个方法)  
- [ ] `generateReviewQueue(Long userId, int limit)` - `codetop-fsrs-queue` 缓存
- [ ] `getUserLearningStats(Long userId)` - `codetop-fsrs-stats` 缓存
- [ ] `getSystemMetrics(int days)` - `codetop-fsrs-metrics` 缓存

#### 3. ProblemService (12个方法)
- [ ] `findById(Long problemId)` - `codetop-problem-single` 缓存
- [ ] `findAllProblems(Page<Problem> page, String difficulty, String search)` - `codetop-problem-list` 缓存
- [ ] `searchProblems(String keyword, Page<Problem> page)` - `codetop-problem-search` 缓存
- [ ] `advancedSearch(AdvancedSearchRequest request, Page<Problem> page)` - `codetop-problem-advanced` 缓存
- [ ] `enhancedSearch(EnhancedSearchRequest request, Page<Problem> page)` - `codetop-problem-enhanced` 缓存
- [ ] `findByDifficulty(Difficulty difficulty, Page<Problem> page)` - `codetop-problem-difficulty` 缓存
- [ ] `getHotProblems(int minCompanies, int limit)` - `codetop-problem-hot` 缓存
- [ ] `getRecentProblems(int limit)` - `codetop-problem-recent` 缓存
- [ ] `getTagStatistics()` - `codetop-tag-stats` 缓存
- [ ] `getStatistics()` - `codetop-problem-stats` 缓存
- [ ] `getUserProblemProgress(Long userId)` - `codetop-user-progress` 缓存
- [ ] `getProblemMastery(Long userId, Long problemId)` - `codetop-user-mastery` 缓存

### 现有 RedisTemplate 基础设施

✅ **已完成的基础设施：**
- `RedisConfig` - 统一的 RedisTemplate 和序列化配置
- `LeaderboardService` - 已使用手动 RedisTemplate 缓存的参考实现
- `RedisCacheUtil` - 序列化测试工具
- `CacheInvalidationManager` - 缓存失效管理器
- `CacheInvalidationStrategy` - 缓存失效策略

## 🏗️ 技术方案设计

### 1. 缓存抽象层设计

创建统一的缓存服务接口和实现：

```java
// 缓存服务接口
public interface CacheService {
    <T> void put(String key, T value, Duration ttl);
    <T> T get(String key, Class<T> type);
    <T> List<T> getList(String key, TypeReference<List<T>> typeRef);
    void delete(String key);
    void deletePattern(String pattern);
    boolean exists(String key);
    void expire(String key, Duration ttl);
}

// 缓存键构建器增强
public class CacheKeyBuilder {
    // 现有方法保持不变
    // 新增通用方法
    public static String buildKey(String prefix, Object... parts);
    public static String buildUserKey(String prefix, Long userId, Object... parts);
}
```

### 2. 缓存配置统一管理

```java
// 缓存配置类
@ConfigurationProperties(prefix = "cache.config")
public class CacheConfiguration {
    private final Map<String, CacheConfig> caches = new HashMap<>();
    
    public static class CacheConfig {
        private Duration ttl = Duration.ofHours(1);
        private String keyPrefix;
        private boolean enableMetrics = true;
    }
}
```

### 3. 缓存监控和指标

```java
// 缓存指标收集器
@Component
public class CacheMetricsCollector {
    public void recordCacheHit(String cacheName);
    public void recordCacheMiss(String cacheName);
    public void recordCacheOperation(String operation, Duration duration);
}
```

## 📝 详细迁移步骤

### 阶段1：基础设施搭建 (预计2小时)

#### 1.1 创建缓存服务抽象层
- [ ] 创建 `CacheService` 接口
- [ ] 实现 `RedisCacheService` 
- [ ] 增强 `CacheKeyBuilder` 类
- [ ] 创建 `CacheConfiguration` 配置类

#### 1.2 创建缓存工具和监控
- [ ] 创建 `CacheMetricsCollector` 指标收集器
- [ ] 创建 `CacheHelper` 工具类
- [ ] 配置缓存监控端点

### 阶段2：服务层迁移 (预计4小时)

#### 2.1 AuthService 迁移 (预计30分钟)
- [ ] 移除 `@Cacheable` 注解
- [ ] 注入 `CacheService`
- [ ] 重构 `getUserCacheDTOById` 方法
- [ ] 添加缓存失效逻辑
- [ ] 编写单元测试

**迁移前:**
```java
@Cacheable(value = "codetop-user-profile", key = "T(com.codetop.service.CacheKeyBuilder).userProfile(#userId)")
public UserCacheDTO getUserCacheDTOById(Long userId) {
    // 方法实现
}
```

**迁移后:**
```java
public UserCacheDTO getUserCacheDTOById(Long userId) {
    String cacheKey = CacheKeyBuilder.userProfile(userId);
    UserCacheDTO cached = cacheService.get(cacheKey, UserCacheDTO.class);
    if (cached != null) {
        return cached;
    }
    
    // 原有数据库查询逻辑
    UserCacheDTO result = loadUserFromDatabase(userId);
    if (result != null) {
        cacheService.put(cacheKey, result, Duration.ofHours(1));
    }
    return result;
}
```

#### 2.2 FSRSService 迁移 (预计1.5小时)
- [ ] 迁移 `generateReviewQueue` 方法 (30分钟)
- [ ] 迁移 `getUserLearningStats` 方法 (30分钟)  
- [ ] 迁移 `getSystemMetrics` 方法 (30分钟)
- [ ] 添加缓存失效逻辑
- [ ] 编写单元测试

#### 2.3 ProblemService 迁移 (预计2小时)
- [ ] 迁移单一实体缓存方法 (30分钟)
  - `findById`
  - `getStatistics`
  - `getTagStatistics`
- [ ] 迁移分页查询缓存方法 (45分钟)
  - `findAllProblems`
  - `searchProblems` 
  - `advancedSearch`
  - `enhancedSearch`
  - `findByDifficulty`
- [ ] 迁移列表缓存方法 (30分钟)
  - `getHotProblems`
  - `getRecentProblems`
- [ ] 迁移用户相关缓存方法 (15分钟)
  - `getUserProblemProgress`
  - `getProblemMastery`
- [ ] 编写单元测试

### 阶段3：配置和清理 (预计1小时)

#### 3.1 Spring Cache 配置清理
- [ ] 从 `RedisConfig` 中移除 `@EnableCaching`
- [ ] 移除 `CacheManager` Bean 配置
- [ ] 清理不需要的缓存配置

#### 3.2 依赖清理
- [ ] 检查并移除不需要的 Spring Cache 依赖
- [ ] 更新相关导入语句
- [ ] 清理缓存相关的配置文件

### 阶段4：测试和验证 (预计2小时)

#### 4.1 功能测试
- [ ] 缓存读写功能测试
- [ ] 缓存失效功能测试  
- [ ] 缓存键生成测试
- [ ] TTL 过期测试

#### 4.2 性能测试
- [ ] 缓存命中率测试
- [ ] 响应时间对比测试
- [ ] 内存使用情况测试
- [ ] 并发访问测试

#### 4.3 集成测试
- [ ] API 端到端测试
- [ ] 缓存一致性测试
- [ ] 故障恢复测试

## 🔧 实现细节

### 缓存键策略

保持现有的 `CacheKeyBuilder` 键生成逻辑不变，确保迁移过程中缓存键的一致性。

### TTL 配置

| 缓存类型 | 当前TTL | 迁移后TTL | 说明 |
|---------|---------|-----------|------|
| user-profile | 1小时 | 1小时 | 用户信息缓存 |
| fsrs-queue | 5分钟 | 5分钟 | FSRS复习队列 |
| fsrs-stats | 10分钟 | 10分钟 | 用户学习统计 |
| fsrs-metrics | 1小时 | 1小时 | 系统指标 |
| problem-* | 30分钟 | 30分钟 | 问题相关缓存 |

### 错误处理策略

1. **缓存异常处理**：当 Redis 不可用时，直接查询数据库
2. **序列化异常**：记录错误日志，使用数据库查询
3. **网络超时**：设置合理的超时时间，失败时降级

### 监控指标

- 缓存命中率 (Hit Ratio)
- 缓存响应时间 (Response Time)
- 缓存大小 (Cache Size)
- 失效频率 (Eviction Rate)

## 🚨 风险评估和预防

### 高风险项
1. **数据一致性风险**：确保缓存失效时机正确
2. **性能回退风险**：迁移后性能可能短期下降
3. **序列化兼容性**：确保新旧数据格式兼容

### 预防措施
1. **分阶段迁移**：逐个服务迁移，降低影响范围
2. **兼容性测试**：充分测试序列化兼容性
3. **回滚方案**：保留原有 `@Cacheable` 代码以便快速回滚
4. **监控预警**：实时监控缓存性能指标

## 📈 迁移验收标准

### 功能验收
- [ ] 所有 `@Cacheable` 注解已完全移除
- [ ] 缓存读写功能正常
- [ ] 缓存失效机制正常
- [ ] API 响应时间无显著增加 (±5%)

### 性能验收  
- [ ] 缓存命中率 ≥ 85%
- [ ] 缓存响应时间 < 10ms (P95)
- [ ] Redis 连接数稳定
- [ ] 内存使用增长 < 10%

### 代码质量验收
- [ ] 单元测试覆盖率 ≥ 90%
- [ ] 集成测试全部通过
- [ ] 代码审查通过
- [ ] 性能测试通过

## 📚 相关资源

- [Spring Data Redis 官方文档](https://docs.spring.io/spring-data/redis/docs/current/reference/html/)
- [Redis Template 最佳实践](https://spring.io/guides/gs/messaging-redis/)
- [项目现有 LeaderboardService 实现参考](src/main/java/com/codetop/service/LeaderboardService.java)

## 🤝 参与人员

- **负责人**：Claude AI Assistant
- **开发**：开发团队
- **测试**：QA团队  
- **运维**：DevOps团队

---

**预计总耗时**：9小时  
**建议完成时间**：2-3个工作日  
**优先级**：中等

> 💡 **提示**：建议在低峰期执行迁移，并准备快速回滚方案以应对突发情况。