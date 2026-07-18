## Context

`module-pipeline` 的构建流程在每次执行结束后落库一条 `BuildRecord`。
持久化由 `JsonFileBuildRecordRepository` 实现：每条记录是
`data/build-records/{recordId}.json` 一个独立文件。当前 `BuildRecordRepository`
接口仅有 `save / findById / findByBuildPipelineId`，没有删除能力，历史记录
无上限增长。

构建有两个写历史的保存点：
- 同步入口 `BuildPipelineServiceImpl.execute()` 末尾（约 line 273）
- 异步入口 `executeWithProgress()` 末尾（约 line 571）

`findByBuildPipelineId()` 已经按 `startedAt` 倒序返回，清理逻辑可零成本复用
该排序。本设计承接 brainstorm 的选定方向，展开实现细节。

## Goals / Non-Goals

**Goals:**
- 每个构建流程（pipeline）保留最新 10 条历史，超出部分按 `startedAt`
  时间顺序删除
- 每次构建完成（同步 + 异步两个入口）后自动触发清理
- 持久化删除细节封装在 Repository 层，Service 层只做编排

**Non-Goals:**
- 阈值可配置化（当前硬编码 10，未来有需要再抽配置项）
- 全局历史总数上限（仅按 pipeline 维度）
- 历史数据迁移脚本（超量遗留记录在对应 pipeline 下次构建后自然收敛）
- 并发锁（边缘场景偶尔多删一条可接受）
- 按状态（SUCCESS/FAILED）差异化保留

## Decisions

### D1：清理范围维度 —— 按构建流程（pipeline）各自 10 条
- **选择**：每个 `buildPipelineId` 维度独立保留最新 10 条
- **理由**：数据模型按 `buildPipelineId` 关联，查询接口与 UI 历史均按
  pipeline 维度组织；按 pipeline 清理语义自然，A 的清理不影响 B
- **已考虑 alternative**：全局总共 10 条 —— 拒绝，会跨 pipeline 误删，
  且需扫描全目录，与现有查询模型不一致

### D2：阈值来源 —— 硬编码常量
- **选择**：`BuildPipelineServiceImpl` 内 `private static final int
  MAX_RECORDS_PER_PIPELINE = 10`
- **理由**：当前无「不同 pipeline 不同上限」需求，硬编码零配置成本
- **已考虑 alternative**：放 `application.yml` 可配 —— 拒绝（YAGNI），
  当前属过度设计

### D3：触发时机 —— 每次 save 之后内联清理
- **选择**：在两个保存点 `recordRepository.save(record)` 之后立即调用
  `pruneOldRecords(pipelineId)`
- **理由**：满足「每次运行完都检查」，延迟最低，无需引入定时任务
- **已考虑 alternative**：定时批量清理 —— 拒绝，存在「窗口期」继续膨胀，
  且需额外调度

### D4：删除能力位置 —— Repository 层
- **选择**：`BuildRecordRepository` 新增 `void deleteById(String id)`，
  `JsonFileBuildRecordRepository` 用 `Files.deleteIfExists(
  recordDir.resolve(id + ".json"))` 实现
- **理由**：文件是持久化细节，应封装在 Repository；Service 不直接碰文件系统
- **已考虑 alternative**：Service 直接删文件 —— 拒绝，破坏分层封装

### D5：RUNNING 状态豁免 —— 不特殊处理
- **选择**：清理时不区分状态
- **理由**：保留最新 10 条天然不会碰到 RUNNING（其 `startedAt` 最新，必在
  前 10 之内）
- **已考虑 alternative**：豁免 RUNNING —— 拒绝，无实际触发场景，徒增复杂度

### D6：并发安全 —— 不加锁
- **选择**：清理逻辑不加任何锁
- **理由**：极端并发下最多偶尔多删一条，无数据损坏；正常 cron + 手动触发
  不会重叠
- **已考虑 alternative**：文件锁/进程锁 —— 拒绝，属过度设计

### D7：排序复用 —— 复用 `findByBuildPipelineId` 的倒序
- **选择**：清理时直接 `findByBuildPipelineId(pipelineId)`，取第 10 条之后
  的记录删除
- **理由**：该方法已按 `startedAt` 倒序，零额外查询成本
- **已考虑 alternative**：新增「分页/计数」查询 —— 拒绝，当前规模无需

## Risks / Trade-offs

- [Risk] 同一 pipeline 两次构建并发完成时，两个清理读到同样超量集合、
  各删一条 → Mitigation: 可接受；无数据损坏，最多多删一条历史
- [Risk] 删除文件时 I/O 异常 → Mitigation: 捕获异常记 warn 日志，不向上
  抛出，不影响构建主流程
- [Risk] `startedAt` 为 null 导致排序 NPE → Mitigation: 所有 record 创建时
  均 `setStartedAt(LocalDateTime.now())`，不会为 null；不额外防御
- [Trade-off] 不提供历史迁移脚本 → 接受理由：超量遗留记录在对应 pipeline
  下次构建后自然收敛，无需一次性清理
- [Trade-off] 阈值不可配置 → 接受理由：当前需求单一，YAGNI

## Migration Plan

N/A — 本 change 不涉及部署变更（纯代码、无 endpoint / DB schema / 依赖变更）。

历史遗留的超量记录无需手动处理：每个 pipeline 在其下次构建完成后会自动
收敛到 10 条。回滚策略即还原代码（移除清理调用与 `deleteById`），历史
恢复无上限增长，已删除的旧记录不可恢复（属预期行为）。

## Open Questions

无 —— 所有关键决策已在 brainstorm 阶段决议。
