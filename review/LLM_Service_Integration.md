# LLM Service Integration — Review & Suggestions

本文基于当前代码库，对任务“LLM Service Integration”（第一个子任务）的完成度进行评估，并给出改进建议与落地清单。

## 结论摘要
- 完成度：约 70–80%。已具备可用的服务骨架、Provider 适配、责任链、重试/限流与缓存，以及 Prompt 模板和响应解析。
- 主要缺口：
  - 责任链节点级控制（per-node 超时、重试、限流、onErrorsToNext）未完全落地。
  - 链路指标/可观测性（hops、命中节点、失败原因）未透出到响应 Meta。
  - 多环境配置示例不足；敏感凭据误放在 `application.yml`。
  - Prompt 版本与 DTO 标记不一致；异步链路未应用 RL/Retry。

## 对照验收标准
- [x] Service 能调用 LLM 并解析响应：`OpenAiProvider` 同步/异步实现，JSON 解析健壮（支持 content 直读与 fenced）。
- [~] 多环境与安全凭据：代码支持 env 取值，但 `application.yml` 中存在明文 key；未提供按 profile 的差异化示例。
- [x] 失败降级回退 FSRS：`AIRecommendationService` 在链路失败时回退 FSRS（服务层实现）。
- [x] 限流与链路顺序/兜底：全局&用户级 RateLimiter + `DefaultProvider` 兜底；顺序由 yml 决定。
- [ ] 节点可配“哪些错误下钻”：未落地 `onErrorsToNext`。
- [ ] 输出链路指标与日志：未将 hops 等写入 `Meta`/响应头。

## 已实现亮点
- AIRecommendationService
  - 构建 FSRS 候选（`CandidateBuilder`），支持 sync/async 路径，LLM 结果映射至 `RecommendationItemDTO`，1h 缓存，失败回退 FSRS。
- Provider
  - OpenAiProvider：基于 `HttpClient`，严格 JSON prompt，`response_format: json_object`，健壮解析，错误码统一。
  - MockProvider、DefaultProvider：便于本地与兜底策略。
- 责任链
  - `ProviderChain`：按配置顺序尝试，集成 Resilience4j 全局/用户限流与重试（同步路径）。
- Prompt 模板
  - `PromptTemplateService`：版本化（默认 v2），系统/用户消息结构清晰，约束明确。
- API
  - `GET /problems/ai-recommendations` 与 `POST /problems/{id}/recommendation-feedback` 已实现。
- 构建
  - 项目可编译（`mvn -DskipTests package` 通过）。

## 主要问题与风险
- 配置与安全
  - `src/main/resources/application.yml` 中 `llm.openai.apiKeyEnv` 配置为明文 key，违反“凭据仅经环境变量提供”的要求。
  - 未提供 `dev/test/prod` 的 LLM 差异化示例；`llm.enabled` 仅全局开关，缺少分段（用户分群/AB）开关示例。
- 责任链弹性
  - 节点级参数未生效：`Node.timeoutMs`、`Node.retry.attempts` 未用于节点级装饰。
  - 缺少 `onErrorsToNext` 策略：无法按错误类型决定是否下钻。
  - 异步执行未应用 RL/Retry 装饰，也未统一超时控制（可用 `orTimeout`/自定义装饰）。
- 可观测性与响应 Meta
  - `ProviderChain.Result.hops` 未透出到 `AIRecommendationResponse.Meta.chainHops`（控制器已尝试读取）。
  - `Meta.strategy` 未区分 default 化失败原因（可设置 busy_message/fsrs_fallback + defaultReason）。
- Prompt 与 DTO 一致性
  - `PromptTemplateService.getCurrentPromptVersion()` 返回 `v2`，但 `AIRecommendationService` 将 `promptVersion` 写为 `"v1"`。
- 缓存一致性
  - 缓存键未包含 prompt 版本/策略（变更 prompt 或链策略后存在“脏命中”风险）。
  - `CacheHelper` 存在更统一的 cache-aside 能力，但当前服务直接使用 `CacheService`。

## 建议的改动清单（可执行项）
1) 安全与配置
- 将 `application.yml` 中 `llm.openai.apiKeyEnv` 改为环境变量名（例如 `DEEPSEEK_API_KEY`），禁止明文 key。
- 在 `dev/test/prod` 区块提供 LLM 示例（dev 开；test/off；prod 用 env 注入，默认 off）。
- （可选）新增用户分段开关：`llm.toggles.byTier/byAbGroup`，或在 `RequestContext` 来源处根据当前用户解析。

2) 责任链增强（按节点生效）
- 扩展配置：在 `LlmProperties.Node` 增加：
  - `List<String> onErrorsToNext`
  - `RateLimit rateLimit { rps, burst }`（可选）
- `ProviderChain` 同步/异步路径：
  - 按节点读取 `retry.attempts` 生成/选择对应 `Retry` 实例。
  - 应用节点级 `timeoutMs`（同步用 request 超时/装饰，异步用 `orTimeout`）。
  - 根据 `onErrorsToNext` 判断是否切换到下个节点或直接 default。
  - 为异步路径增加 RL/Retry 装饰或显式节流。

3) 可观测性/响应
- 在 `AIRecommendationService` 将 `Result.hops` 写入 `Meta.chainHops`；当默认化时设置 `Meta.strategy=busy_message` 并附带 `defaultReason`（可扩展 `Meta` 字段或通过响应头）。
- 控制器已有 `X-Provider-Chain`/`X-Rec-Source` 逻辑，确保填充所需 Meta 字段即可出头。

4) Prompt/DTO 一致性
- 注入 `PromptTemplateService` 到 `AIRecommendationService`，用真实 `getCurrentPromptVersion()` 写入 DTO。
- 缓存键追加 prompt 版本前缀（如 `pv2`），避免不同 prompt 版本共享缓存。

5) 缓存整合
- 用 `CacheHelper.cacheOrComputeList` 统一缓存路径，利用默认 TTL/监控能力；并在缓存键加入用户偏好/参数哈希（limit、promptVersion、abGroup 等）。

## 关联文件与建议修改点（路径/关键点）
- `src/main/resources/application.yml`
  - 修改 `llm.openai.apiKeyEnv` 为 `DEEPSEEK_API_KEY`（或 `OPENAI_API_KEY`）；按 profile 提供示例；确保 `llm.enabled` 在 prod 默认为 `false` 并由环境开关。
- `src/main/java/com/codetop/recommendation/config/LlmProperties.java`
  - `Node` 增加 `onErrorsToNext`, `RateLimit` 配置；`Chain` 保持不变。
- `src/main/java/com/codetop/recommendation/chain/ProviderChain.java`
  - 同步/异步均按节点应用 Retry/RL/Timeout 装饰；依据 `onErrorsToNext` 决策。
  - `Result` 增加 `defaultReason` 透传；返回 hops。
- `src/main/java/com/codetop/recommendation/service/AIRecommendationService.java`
  - 将 `Result.hops` 写入 `Meta.chainHops`；为 default/fallback 设置 `Meta.strategy` 与必要的 headers 来源字段。
  - 统一 `promptVersion` 与 `PromptTemplateService`；缓存键纳入 promptVersion。
  - 可用 `CacheHelper.cacheOrComputeList` 简化缓存。
- `src/main/java/com/codetop/recommendation/provider/impl/OpenAiProvider.java`
  - `resolveApiKey` 若使用字面量 key 打 warning 日志；保持 env 优先。

## 可选改进
- 为链路与 Provider 增加结构化日志（模型名、延迟、重试次数、错误码）；接入 `micrometer` 指标并在 Actuator 暴露。
- 在 `DefaultProvider` 支持 `fsrs_fallback` 策略：返回带标记的信号，让 Service 决策是否进入 FSRS 回退；或直接在 Provider 内构造 FSRS 推荐（需依赖裁剪）。
- 异步执行使用受控线程池而非 `ForkJoinPool.commonPool`，提高可控性。

## 当前实现参考（亮点文件）
- 责任链与 Provider：
  - `recommendation/chain/ProviderChain.java`
  - `recommendation/provider/impl/{OpenAiProvider,MockProvider,DefaultProvider}.java`
- Prompt：`recommendation/service/PromptTemplateService.java`
- 候选集：`recommendation/alg/CandidateBuilder.java`
- 服务层：`recommendation/service/AIRecommendationService.java`
- API：`recommendation/controller/ProblemRecommendationController.java`
- 配置：`recommendation/config/{LlmConfiguration,LlmProperties}.java`、`resources/application.yml`

---

如需，我可以直接提交以下最小变更集：
- 清理 `application.yml` 明文密钥，切换为 `DEEPSEEK_API_KEY`。
- 在 `AIRecommendationService` 写入 `Meta.chainHops` 与对齐 `promptVersion`。
- 为 `ProviderChain.executeAsync` 增加 RL/Retry/timeout 装饰（与同步一致）。
- 在 `LlmProperties.Node` 加 `onErrorsToNext` 字段并在链路中应用。

这些改动能快速将“第一个子任务”的剩余必需项补齐到可验收水平。
