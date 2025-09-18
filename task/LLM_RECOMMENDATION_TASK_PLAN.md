# LLM 智能推荐 — 任务计划（执行版）

更新时间：2025-09-13
负责人：@backend @frontend @qa @sre（占位）

## 目标与范围
- 目标：在现有 FSRS 基础上引入 LLM 智能重排与解释，保证低延迟、可降级、可观测，并通过 A/B 验证质量提升。
- 范围（In Scope）
  - 后端：ProviderChain、Prompt 模板、AIRecommendationService、缓存/限流/降级、指标、接口与契约。
  - 前端：推荐卡片/列表、仪表盘集成、用户反馈、基础过滤与无限加载、E2E。
  - 运维：监控告警、成本控制、灰度开关与回滚预案。
- 非目标（Out of Scope）
  - 富交互对话、 embeddings 在线召回（仅列为 P2 选项）、复杂 ML 排序模型训练。

## 当前进展简述（已完成）
- 后端
  - [x] ProviderChain 责任链 + 默认兜底（busy/fsrs_fallback）。
  - [x] OpenAI 兼容 Provider（DeepSeek）+ 同步/异步 + JSON 解析。
  - [x] ChainSelector 路由、LlmToggleService 灰度开关、LlmConfigValidator 启动校验。
  - [x] HybridRanking、RecommendationMixer、ConfidenceCalibrator、缓存键设计与失效策略。
  - [x] 接口：GET /problems/ai-recommendations，POST /problems/{id}/recommendation-feedback。
- 前端
  - [x] AIRecommendationCard、列表（分页/无限滚动）、Dashboard 集成与 FSRS/AI 切换。
  - [x] 解释/置信度展示、用户反馈上报、AIBadge 列表标识。
  - [x] 组件/Hook 单测与 Playwright E2E 脚本（需在运行服务下执行）。
- 配置/运维
  - [x] 多环境 application.yml + .env.example；关键 LLM 配置抽离；基本 Micrometer 指标收集。

## 未完成与风险点（概览）
- [ ] Provider 级 JSON Schema response_format 严格校验（保留 json_object 作为降级）。
- [ ] API 错误语义与头部契约最终定稿（400/429/503；X-* 头一致性）。
- [ ] 指标扩展与标准化：p50/p95、fallback ratio、cache hit、chain hops、原因码维度化。
- [ ] 成本/配额防护：tokens 估算、链级预算、自动退级策略与告警。
- [ ] 前端：推荐历史视图、偏好设置页（topics/difficulty/frequency）与后端打通。
- [ ] 前端：高级排序（score/difficulty/topic）与更多过滤项（现有基础过滤已支持）。
- [ ] 合同测试（REST Docs/OpenAPI）、性能压测、告警阈值/仪表盘落地。
- [ ] 安全合规：Prompt 内容最小化/脱敏审计，密钥只从环境加载校验。
- [ ] 可选 P2：problem_embeddings 表与迁移脚本（离线构建向量召回）。

## 分阶段执行计划

### Phase 1 — 后端 P0（稳态能力）
- Provider/解析
  - [ ] 支持 OpenAI response_format: json_schema；失败自动降级至 json_object；解析双重校验。
  - [ ] 统一错误语义：TIMEOUT/HTTP_429/HTTP_5XX/PARSING_ERROR/DEFAULTED；控制器 400/429/503 输出对齐。
- 指标/日志
  - [ ] Micrometer 指标补充：llm.latency_ms（p50/p95）、llm.fallback.ratio、rec.cache_hit_ratio、llm.chain.hops。
  - [ ] 统一 tags：provider/model/chainId/abGroup/route/disabled_reason。
- 成本与退级
  - [ ] 轻量 token 预算估算（prompt+候选摘要长度近似）；超过阈值降级为 fsrs_fallback 或缩小候选集。
  - [ ] 链级预算配置 + 告警（Prometheus 计数器）。
- 契约与文档
  - [ ] OpenAPI/REST Docs 出具契约文档，固定响应/头部字段；在 CI 校验。

交付物：
- 代码改动（Provider/解析/指标/错误语义），OpenAPI 文档，示例响应，变更日志。

### Phase 2 — 前端 P0（可用性与可控性）
- 体验与偏好
  - [ ] 推荐历史页（近 7/30 天采纳/跳过/解决记录），指标聚合展示。
  - [ ] 偏好设置页（topics/difficulty/frequency），对接后端用户偏好 API。
- 列表增强
  - [ ] 排序：score/difficulty/topic；
  - [ ] 过滤：多主题/难度 + 清晰态指示；
  - [ ] a11y 与移动端适配细节打磨。

交付物：
- 两个新页面 + 路由 + 组件；对接 API；E2E 覆盖新增路径。

### Phase 3 — 质量 / SRE
- 测试
  - [ ] 合同测试（REST Docs 或 OpenAPI 校验）。
  - [ ] 压测脚本（Gatling/JMeter），SLO：缓存命中 p95 < 800ms；非命中 p95 < 2s；错误率 < 1%。
- 观测与告警
  - [ ] Grafana 面板：延迟、成功率、fallback ratio、cache hit、429 速率、成本估计。
  - [ ] 告警规则：429 峰值、超时激增、fallback ratio>阈值、成本超预算。
- 运维
  - [ ] 运行手册：功能开关、责任链策略、回滚/降级、排障 checklist。

交付物：
- CI 中的契约校验、性能报告、仪表盘 JSON、Alert 规则、Runbook。


交付物：
- SQL 迁移、配置样例、评估报告（）。

## 里程碑与排期（建议）
- 第 1 周：Phase 1 — Provider/解析/指标/契约/成本防护。
- 第 2 周：Phase 2 — 历史/偏好/排序过滤 + 文档；Phase 3 契约/压测与初版看板。
- 第 3 周：Phase 3 告警完善 + a11y/移动端打磨；（可选）Phase 4 DDL 与方案评估。

## 依赖与前置条件
- 数据库与 Redis 可用；环境变量：DEEPSEEK_API_KEY/OPENAI_API_KEY、JWT_SECRET。
- application.yml 已示例完备；多 Profile 可用（dev/test/prod）。
- Prometheus + Grafana（或等价监控）接入权限。

## 风险与对策
- 外部 API 不稳定 / 超时：
  - 限时 2s + Retry + 责任链下钻 + FSRS 回退；缓存与预热。
- 成本/配额：
  - 限速、候选裁剪、预算阈值与告警；必要时自动切至 FSRS-only。
- 解析不稳 / 幻觉：
  - 强制 JSON Schema + 提取器降级；解析失败计数与回退。
- 前端体验：
  - Busy/DEFAULT 场景清晰提示；反馈闭环可见；移动端可用性测试。

## 验收标准（DoD）
- 响应始终为 JSON（Schema 校验或稳健降级）；
- 错误语义一致，头部契约稳定（X-Trace-Id / X-Rec-Source / X-Cache-Hit / X-Provider-Chain / X-Recommendation-Type）；
- 指标可观测（延迟/成功率/fallback/cache hit/429）；
- 成本防护生效（估算/阈值/告警/退级）；
- 前端历史/偏好可用，推荐理由/置信度清晰；
- E2E / 合同 / 压测均通过，满足 SLO。

## 运行与验证（开发）
- 启动后端（开发模式）：
  - `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- 启动前端（Next.js）：
  - `cd frontend && npm install && npm run dev`
- 端到端 E2E（需应用运行）：
  - `cd frontend && npm run test:e2e`

---

> 备注：本计划与 `task/task_llm_recommendation_subtasks.md` 同步，已完成项以 [x] 标记，其余按 Phase 推进。
