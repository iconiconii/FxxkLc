目标与指标

  - P50 响应：缓存命中 < 150ms；LLM 成功 < 2.0s；超时回退 < 2.3s
  - 回退可用性：LLM 不可用时 100% FSRS 可用
  - Token 成本：单次 LLM 请求 tokens 降 30%+
  - 错误率：P95 解析错误/4xx 导致回退率 < 5%

  P0 立即项（当天完成）

  - 后端强制总超时护栏
      - 已加入：AIRecommendationService 使用 providerChain.executeAsync(...).get(guardMs)，guardMs =
  openai.timeoutMs + 400ms（默认约 2.2s），超时即回退 FSRS。
      - 配置建议：llm.chain.nodes[openai].timeoutMs=1200~1800，整体响应保持 < 2.3s。
  - DeepSeek 兼容输出控制
      - 已加入：OpenAiProvider 对 DeepSeek 自动降级 response_format=json_object，避免 400 BAD_REQUEST。
      - 辅助：非 200 响应记录 body 片段，便于快速判因（key、参数、超限）。
  - 候选集缩减（减 token）
      - 在 AIRecommendationService#getRecommendations 里，将进入 LLM 的候选集上限固定为 min(20, limit *
  2)；过多候选只在 FSRS 层内部评估，不传给 LLM。
      - 当前 generateSimplifiedCandidates 直接拿 getDueCards(user, candidateLimit)，将 candidateLimit 计算
  调整为 min(60, limit * 3)，随后用 CandidateEnhancer 排序只保留 Top 20 传 LLM。
  - 快速失败策略
      - ProviderChain 保留对 API_KEY_MISSING/UNAUTHORIZED/BAD_REQUEST 直接 default 的逻辑（不重试/不
  多跳）。
      - onErrorsToNext 仅对 TIMEOUT/HTTP_429/5xx/PARSING_ERROR 尝试下一节点；否则进入默认（FSRS）。

  P1 结果缓存（核心提速）

  - 写入缓存
      - 在 AIRecommendationService LLM 成功后，序列化整包 AIRecommendationResponse 写入
  cacheService.put(cacheKey, json, TTL=3600s)。
      - Key 规范：使用现有 CacheKeyBuilder.buildUserKey("rec-ai", userId, "limit_"+limit, "obj_"+objective,
  "diff_"+difficulty, "domains_"+hash(domains), "timebox_"+timebox, "pv_"+promptVersion, "type_"+recType)；
  domains 用稳定有序拼接后再做短 hash。
  - 读取缓存（命中即 return）
      - 在 LLM 执行前：读取缓存；命中直接反序列化并返回，同时设置响应头/Meta（X-Cache-Hit: true；保持 X-
  Rec-Source 与 meta.strategy）。
      - 控制参数：新增 forceRefresh（或 refresh=true）跳过缓存，强制新鲜结果；落在控制器层解析并传入
  service。
  - 失效策略
      - 以下事件删除 rec-ai:*：提交复习（写入 ReviewLog）、FSRS 参数变更、偏好修改（目标/难度/域）、题目标
  签或难度变更。
      - 入口点：在复习提交/偏好保存/题目变更后调用 cacheService.deleteByPattern(userId 前缀)；优先明确 key
  精确删，避免通配大面积删除。
  - 可见性
      - 命中缓存时响应头追加 X-Cache-Hit: true（控制器已有），X-Rec-Source 保持原值（AI/FSRS/CACHE）。

  P1 中间态缓存（减少 DB/序列化成本）

  - 用户画像缓存
      - Key：userProfile:{userId}；TTL 600s；失效触发同结果缓存。
      - 内容：近 90 天聚合指标、弱项域、难度偏好摘要等；用于 prompt 构建与候选集预筛。
  - 候选集/变量缓存
      - Key：rec-candidates:{userId}:{domainsHash}:{timebox}:{diff}；TTL 60~300s；用于不同 limit 重用。
      - Key：prompt-variables:{userId}:{pv}:{domainsHash}:{diff}:{limitBucket}；TTL 300s；缓存
  candidatesJson 与头像/画像摘要，直接拼装提示。
  - 模板缓存
      - ExternalPromptTemplateService 已加 @Cacheable("promptTemplates")；确保
  PromptTemplateProperties.cacheEnabled=true；在 application.yml 开关。

  P1 控制器与上下文（参数透传与元数据）

  - 透传 ab_group 与 forceRefresh 到上下文
      - 控制器 getAiRecommendations 增加 @RequestParam("ab_group") String abGroup、
  @RequestParam("forceRefresh") Boolean forceRefresh。
      - 将 abGroup 透传到 RequestContext.setAbGroup(...)（当前硬编码 "A"），forceRefresh 传到 service 控制
  缓存读取。
  - 响应头一致性
      - 已有：X-Trace-Id、X-Cache-Hit、X-Rec-Source、X-Provider-Chain、X-Fallback-Reason、X-Recommendation-
  Type、X-Strategy-Used；确保缓存命中时也写齐。

  P1 配置与运维

  - OpenAI/DeepSeek
      - 设置环境变量：DEEPSEEK_API_KEY（或选用 OPENAI_API_KEY）；校验后启动时报 warn。
      - llm.openai.timeoutMs=1200~1800；llm.chain.nodes[openai].timeoutMs 同步；P95 < 2s。
  - 开启/调优限流
      - resilience4j.ratelimiter.llm-global.limit-for-period=5 初始即可；视压测情况提升。
      - 开启 per-user limiter 已存在；必要时将 perUserRps 提到 2~3。
  - 预热开关
      - 在 application.yml 打开推荐预热：将“Recommendation Cache Warming Configuration”中 enabled: true，并
  设定预热用户选择与触发时机（登录后、凌晨定时）。

  P2 预热与异步加载（体感更快）

  - 登录/进入面板预热
      - CacheWarmingService：在用户登录成功 hook、进入 Dashboard 时触发异步预热默认参数的 rec-ai 缓存。
  - 渐进式返回
      - 前端先渲染 FSRS 或最近高频题占位（已有组件支持），随后拉取 AI 结果覆盖；友好提示 X-Rec-Source。
  - SSE/流式（可选）
      - 若后续切换到流式 LLM，后端提供 SSE endpoint，前端分段更新推荐卡片；此项可延后。

  P2 Prompt/请求体精简

  - 候选 JSON 定长化
      - 字段名短化、字段顺序固定；限制每题字段仅包含
  id,topic,difficulty,tags,recentAccuracy,attempts,urgencyScore。
  - 模板精简
      - v3 模板仍可用；将“策略说明”类长文本简化为枚举提示；温度维持 0。
  - TopN 严格控制
      - 传 LLM 的候选不超过 20；limit 只决定返回数量，不放大入参规模。

  P2 Fallback 策略与错误分类

  - 错误分类策略
      - BAD_REQUEST/UNAUTHORIZED：不重试，直接 default（FSRS），记录 X-Fallback-Reason。
      - TIMEOUT/HTTP_429/5xx：一次重试或换节点（若配置）；超过即 default。
  - FSRS 解释增强
      - Fallback 时 reason 字段包含“逾期天数/记忆保留概率/紧急度”，提升可解释性（已实现，可优化文案）。

  P2 监控与报警

  - 指标
      - 链路耗时：总耗时、LLM 调用耗时、缓存命中率、回退率、TopN 候选大小。
      - 失败分类：4xx/5xx/超时/解析失败占比。
  - 日志
      - 对 4xx 记录 body 片段已加；开启慢请求告警阈值（>2s）。
  - 报警
      - 回退率 > 30% 10 分钟内告警；超时率 > 20% 告警。

  前端协作点

  - 查询参数
      - 新增 forceRefresh=true 触发刷新；透传 ab_group 到后端。
  - UI 提示
      - 根据头部 X-Rec-Source 显示徽标与降级提示文案；X-Cache-Hit=true 显示“已为你加速”提示（可选）。
  - 占位/覆盖
      - 初次展示先用 FSRS/热门题占位，再用 AI 结果覆盖，避免白屏等待。

  变更清单（具体到文件/点位）

  - src/main/java/com/codetop/recommendation/controller/ProblemRecommendationController.java
      - 新增 @RequestParam("ab_group"), @RequestParam("forceRefresh")；传给 service。
  - src/main/java/com/codetop/recommendation/service/AIRecommendationService.java
      - 读取缓存命中直接 return；LLM 成功后写缓存；失效点方法；候选 TopN；总超时已加。
  - src/main/java/com/codetop/recommendation/provider/impl/OpenAiProvider.java
      - DeepSeek 使用 json_object；非 200 打印 body 片段（已加）。
  - src/main/resources/application.yml
      - 调整 llm.chain.nodes[openai].timeoutMs；打开预热配置；开启模板缓存；视情况提高 perUserRps。
  - src/main/java/com/codetop/service/FSRSService.java 或复习提交逻辑处
      - 复习完成后调用缓存删除（结果缓存与用户画像缓存）。
  - src/main/java/com/codetop/recommendation/service/ExternalPromptTemplateService.java
      - 确保模板缓存开关；必要时精简模板变量构造。

  测试与验收

  - 单用户热路径
      - 第一次请求：LLM 成功 < 2s；无缓存 X-Cache-Hit: false。
      - 第二次相同参数：< 150ms 命中缓存 X-Cache-Hit: true。
  - 回退路径
      - 禁用/错误 key：FSRS 回退，X-Fallback-Reason 为 API_KEY_MISSING/UNAUTHORIZED，响应 < 600ms。
      - 人为拉长 LLM 响应：总超时触发，FSRS 回退，< 2.3s。
  - 失效路径
      - 完成一次复习/改偏好：旧缓存失效；下一次强制重新生成。
  - Token 降本
      - 对比变更前后，平均候选 JSON 长度与 tokens 降幅 > 30%。