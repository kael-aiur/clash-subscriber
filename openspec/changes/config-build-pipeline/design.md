## Context

当前系统的定时任务（ScheduledTask）承担了过多职责：拉取所有订阅、合并节点、执行 Pipeline 处理、推送到 mihomo 实例。这导致：

- 订阅获取逻辑硬编码在 SchedulerServiceImpl 中，无法独立触发单个流程
- 每次执行拉取全部订阅，无法按流程指定特定数据源
- 定时任务与构建逻辑耦合，扩展困难
- 只记录最近一次执行状态，无历史可追溯

本设计引入 BuildPipeline 实体，将构建逻辑从定时任务中解耦，形成独立的"构建流程"概念。

## Goals / Non-Goals

**Goals:**
- 创建 BuildPipeline 实体，封装完整的构建链路（订阅 → 脚本处理 → 推送）
- 支持手动触发和 cron 定时触发
- 记录完整的构建历史（时间、状态、日志）
- 新建独立模块 module-pipeline，不破坏现有模块依赖
- 前端新增"构建流程"管理页面

**Non-Goals:**
- 不重构现有 PipelineConfig / ConfigProcessor 机制（复用而非重写）
- 不迁移现有 ScheduledTask 数据到 BuildPipeline（后续版本处理）
- 不支持构建流程之间的依赖关系
- 不支持构建队列或并发控制（单次执行一个流程）

## Decisions

### D1：新建独立模块 module-pipeline

- **选择**：创建新 Maven 模块 `module-pipeline`，包含 BuildPipeline、BuildRecord 及其服务
- **理由**：构建流程依赖订阅、处理器、mihomo 三个模块，放在任何一个现有模块都会引入不自然的依赖方向
- **已考虑 alternative**：
  - 扩展 module-processor — 拒绝，会导致 processor 依赖 subscription 和 mihomo，破坏现有依赖方向
  - 扩展 module-scheduler — 拒绝，scheduler 应保持通用调度器职责，不承担构建执行逻辑

### D2：混合模式数据源

- **选择**：主订阅（必须）+ 额外订阅列表（可选），拉取后合并 ProxyNode
- **理由**：兼顾单订阅的简单场景和多订阅合并的灵活需求
- **已考虑 alternative**：
  - 仅单订阅 — 拒绝，无法满足合并多个服务商节点的需求
  - 仅多订阅 — 拒绝，增加简单场景的配置复杂度

### D3：脚本直接关联 + 自动生成 PipelineConfig

- **选择**：BuildPipeline 直接关联脚本文件名（scriptName），执行时自动生成 PipelineConfig 并通过 PipelineService 执行
- **理由**：简化前端 UI（只需选择脚本，不需要理解 Pipeline 概念），同时复用底层 Pipeline 执行能力
- **已考虑 alternative**：
  - 关联现有 PipelineConfig — 拒绝，用户需要理解 Pipeline/Step 抽象，增加使用门槛

### D4：定时调度内嵌到 BuildPipeline

- **选择**：BuildPipeline 自带 cronExpression 和 enabled 字段，SchedulerService 读取并注册 cron 任务
- **理由**：定时是构建流程的自然属性，用户在同一界面配置构建和调度，体验更直观
- **已考虑 alternative**：
  - ScheduledTask 独立关联 BuildPipeline — 拒绝，增加实体间跳转，用户需要在两个页面分别配置

### D5：单目标实例

- **选择**：每个 BuildPipeline 只推送到一个 mihomo 实例
- **理由**：一对一关系简单明确，如果需要推送到多实例，创建多个 BuildPipeline 即可
- **已考虑 alternative**：
  - 多目标实例 — 拒绝，增加配置复杂度，且多实例推送失败时的处理逻辑复杂

### D6：JSON 文件存储

- **选择**：与现有实体一致，使用 JSON 文件存储，路径 `{data.path}/build-pipelines/` 和 `{data.path}/build-records/`
- **理由**：保持项目存储方式一致性，无需引入数据库依赖
- **已考虑 alternative**：
  - SQLite / H2 嵌入式数据库 — 拒绝，与现有架构不一致，增加复杂度

## Risks / Trade-offs

- [风险] BuildRecord 数量增长导致 JSON 文件过大 → 限制单个流程保留最近 100 条记录，超出自动清理
- [风险] 现有 ScheduledTask 与新 BuildPipeline 并存期间功能重叠 → 标记 ScheduledTask 为废弃，前端添加提示引导用户迁移
- [取舍] 不支持构建并发控制 → 接受，当前场景为单用户使用，不需要队列机制
- [取舍] 执行时拉取订阅可能超时 → 复用现有 SubscriptionService.fetch() 的超时和缓存机制

## Migration Plan

1. 新增 module-pipeline 模块，不影响现有功能
2. 前端新增"构建流程"菜单，与"定时任务"菜单并存
3. 现有定时任务功能保持不变，用户可逐步迁移到构建流程
4. 后续版本移除 ScheduledTask 相关代码

## Open Questions

无
