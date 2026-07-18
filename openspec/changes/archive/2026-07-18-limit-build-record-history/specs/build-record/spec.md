<!--
Delta spec：为 build-record capability 新增「限制构建历史数量」需求。
对齐目录：openspec/specs/build-record/spec.md
-->

## ADDED Requirements

### Requirement: 限制构建历史数量

系统 SHALL 在每次构建执行完成并保存 BuildRecord 后，按 `buildPipelineId`
维度对该构建流程的历史记录数量进行约束：按 `startedAt` 时间倒序，仅保留
最新的 10 条，删除更旧的记录。成功与失败的构建记录 SHALL 同等纳入计数与
清理，不同构建流程之间的历史计数与清理 SHALL 相互独立。

#### Scenario: 历史数量未超过上限

- **WHEN** 某构建流程完成第 N 次构建（N ≤ 10）并保存 BuildRecord
- **THEN** 系统保留该流程全部 N 条历史记录，不删除任何记录

#### Scenario: 历史数量超过上限

- **WHEN** 某构建流程完成构建并保存记录后，该流程历史总数为 11 条
- **THEN** 系统按 `startedAt` 倒序保留最新 10 条，删除 `startedAt` 最早
  的那条记录，使历史总数收敛为 10

#### Scenario: 失败构建同样触发清理

- **WHEN** 某构建流程执行失败并保存一条 `status=FAILED` 的记录后，该流程
  历史总数超过 10 条
- **THEN** 系统按 `startedAt` 倒序保留最新 10 条并删除超量记录，失败记录
  与成功记录同等对待

#### Scenario: 不同构建流程独立约束

- **WHEN** 构建流程 A 与构建流程 B 各自累积超过 10 条历史，且仅 A 完成
  一次构建
- **THEN** 系统仅将 A 的历史清理至 10 条，B 的历史记录不受影响

#### Scenario: 已删除记录不可再查询

- **WHEN** 某条 BuildRecord 因超量被删除
- **THEN** 该记录从持久层移除，后续 `findById` 返回不存在，按流程查询
  历史列表不再包含该记录
