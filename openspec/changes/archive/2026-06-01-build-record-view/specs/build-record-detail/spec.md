## ADDED Requirements

### Requirement: 记录构建步骤详情

系统 SHALL 在每次构建执行过程中记录每个环节的详细信息，包含环节名称、状态、开始时间、结束时间、输入数据、输出数据、错误信息。

#### Scenario: 构建步骤记录
- **WHEN** 构建执行完成（成功或失败）
- **THEN** 系统创建的 BuildRecord 中 SHALL 包含 steps 列表，每个 BuildStep 包含 name、status、startedAt、finishedAt、input、output、errorMessage 字段

#### Scenario: 步骤状态定义
- **WHEN** 构建步骤执行
- **THEN** 步骤 status SHALL 为以下之一：SUCCESS（成功）、FAILED（失败）、SKIPPED（跳过）

#### Scenario: 向后兼容
- **WHEN** 查询历史构建记录（无 steps 字段）
- **THEN** 系统 SHALL 返回 steps 为空列表，不影响现有功能

---

### Requirement: 构建流程的固定环节定义

系统 SHALL 将构建流程定义为 4 个固定环节：拉取主订阅、合并额外订阅、脚本处理、推送到 Mihomo。

#### Scenario: 标准构建流程环节
- **WHEN** 执行构建流程
- **THEN** 系统 SHALL 按顺序执行以下环节：
  1. 拉取主订阅配置
  2. 合并额外订阅节点
  3. 脚本处理（如有配置）
  4. 推送到目标 Mihomo 实例

#### Scenario: 无脚本时的环节处理
- **WHEN** 构建流程未配置 scriptName
- **THEN** 系统 SHALL 将"脚本处理"环节状态设为 SKIPPED

#### Scenario: 环节失败中断
- **WHEN** 某个环节执行失败
- **THEN** 系统 SHALL 记录该环节为 FAILED，后续环节设为 SKIPPED，整体构建状态为 FAILED

---

### Requirement: 查询构建记录详情

系统 SHALL 支持按 ID 查询单条构建记录的完整信息，包含所有步骤详情。

#### Scenario: 查询构建记录详情
- **WHEN** 用户请求 GET /api/build-records/{id}
- **THEN** 系统返回该 BuildRecord 的完整信息，包含 steps 列表，每个 step 包含 name、status、input、output 等字段

#### Scenario: 查询不存在的记录
- **WHEN** 用户请求不存在的构建记录 ID
- **THEN** 系统返回 404 错误
