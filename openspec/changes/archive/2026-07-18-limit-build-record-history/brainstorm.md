<!--
Raw capture of superpowers:brainstorming output.

本档原样捕捉 brainstorming 的产出，不强制结构。
Skill 的自然产出通常是 decision log 格式（背景 → 决议链 Q1-Qn → 设计取舍），
但依对话内容可能有不同组织方式。

design.md 从本档萃取并重新整理为结构化设计文档。

不要将本档的内容复制到 design.md — design.md 是独立的重组产物，
两者互补但不重叠。

注：本环境的 superpowers:brainstorming skill 不可用，此处原样捕捉
openspec-explore 阶段已完成的设计探索对话（背景澄清 + AskUserQuestion
决议 + 边缘点默认决策）。
-->

# Brainstorm：限制构建历史记录数量

## 背景

`module-pipeline` 的构建流程在每次执行（同步 `execute` 与异步 `executeAsync`）
完成后，都会把一条 `BuildRecord` 写入文件系统。当前持久化实现是
`JsonFileBuildRecordRepository`：每条记录是一个独立的
`data/build-records/{recordId}.json` 文件。

现状问题：

- 历史记录**无上限、永不清理**。每次构建（含定时 cron 触发）都新增一个文件，
  长期运行后 `build-records/` 目录会无限膨胀。
- `BuildRecordRepository` 接口目前只有 `save / findById /
  findByBuildPipelineId`，**没有 delete / count** 能力。

需求：每次构建完成后检查该构建流程的历史数量，按 `startedAt` 时间顺序
只保留最新 10 条，删除更旧的。

代码落点已定位清楚：

- 写历史的两个 hook 点：
  - 同步：`BuildPipelineServiceImpl.execute()` 末尾 `recordRepository.save(record)`
    （约 line 273）
  - 异步：`BuildPipelineServiceImpl.executeWithProgress()` 末尾
    `recordRepository.save(record)`（约 line 571）
- 已有可复用的排序：`findByBuildPipelineId()` 已按 `startedAt` 倒序返回。

## 决议链

### Q1：「保留最新 10 条」的范围维度？

两种读法：
- (a) 每个构建流程（pipeline）各自保留最新 10 条
- (b) 全局所有 pipeline 的历史加起来只保留最新 10 条

**决议：(a) 每个 pipeline 各留 10 条。**

理由：现有数据模型按 `buildPipelineId` 关联；查询接口
`findByBuildPipelineId` 与 spec「查询构建历史列表」都是按 pipeline 维度；
UI 上历史挂在每个 pipeline 下。按 pipeline 各自保留语义最自然，且
A pipeline 的清理不会误删 B pipeline 的历史。

### Q2：10 这个阈值是否可配置？

- (a) 硬编码常量 `MAX_RECORDS_PER_PIPELINE = 10`
- (b) 放 `application.yml` 可配

**决议：(a) 硬编码常量。**

理由：这是偏运维的小优化，当前没有"不同 pipeline 不同上限"的需求；
硬编码最简单、零配置成本。将来确有需要再抽成配置项。

### Q3：清理触发时机？

**决议：每次构建完成、`save(record)` 之后立即清理。**

两个入口（同步 `execute` / 异步 `executeWithProgress`）都要加，
满足「每次运行完都检查」。

### Q4：删除能力放在哪一层？

**决议：Repository 层加 `deleteById(String id)`，Service 层编排清理。**

- `BuildRecordRepository` 接口新增 `void deleteById(String id)`。
- `JsonFileBuildRecordRepository` 实现：`Files.deleteIfExists(
  recordDir.resolve(id + ".json"))`。
- `BuildPipelineServiceImpl` 新增私有方法 `pruneOldRecords(pipelineId)`：
  复用 `findByBuildPipelineId()`（已倒序），若 `size > 10` 则删除第 10 条
  之后的所有记录。

### Q5：是否豁免 RUNNING 状态的记录？

**决议：不特殊处理。**

保留最新 10 条天然不会碰到 RUNNING 记录——它 `startedAt` 最新、必在
前 10 之内。无需为边缘情况增加复杂度。

### Q6：并发安全？

`executeAsync` 用 `CompletableFuture.runAsync` 异步执行。极端情况下
同一 pipeline 两次构建并发完成，两个清理可能读到同样的 11 条、各删一条，
最终变成 9 条。

**决议：可接受，不加锁。**

无错误、无数据损坏，只是偶尔多删一条历史。为这种低频边缘加分布式/进程锁
属于过度设计。定时 cron 触发 + 手动触发的正常场景下不会重叠。

## 设计取舍

| 维度 | 选择 | 放弃的方案 | 原因 |
|------|------|-----------|------|
| 范围 | 按 pipeline 各 10 条 | 全局 10 条 | 贴合现有数据模型与 UI 语义 |
| 阈值 | 硬编码常量 | 可配置 | 当前无差异化需求，YAGNI |
| 触发 | 每次 save 后内联清理 | 定时任务批量清理 | 满足「每次运行完检查」，且延迟最低 |
| 删除 | Repository 加 deleteById | Service 直接操作文件 | 保持持久化细节封装在 Repository |
| 排序依据 | 复用 `startedAt` 倒序 | 新增查询 | `findByBuildPipelineId` 已排序，零成本 |
| 并发 | 不加锁 | 文件锁/数据库 | 边缘场景偶尔多删一条，可接受 |

## 已默认决定（未单独提问，如不同意可在 design 阶段推翻）

- RUNNING 状态不豁免（见 Q5）。
- 并发不加锁（见 Q6）。
- 两个执行入口（同步 + 异步）都接入清理。
- 清理失败（如文件删不掉）应记 warn 日志但不影响构建主流程。
