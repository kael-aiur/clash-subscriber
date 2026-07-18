## 1. 持久层：为 Repository 增加删除能力

- [x] 1.1 在 `BuildRecordRepository` 接口新增方法 `void deleteById(String id)`
- [x] 1.2 在 `JsonFileBuildRecordRepository` 实现 `deleteById`：用
      `Files.deleteIfExists(recordDir.resolve(id + ".json"))`，捕获 I/O 异常
      记 warn 日志，不向上抛出（避免影响构建主流程）

## 2. 业务层：清理编排

- [x] 2.1 在 `BuildPipelineServiceImpl` 顶部新增常量
      `private static final int MAX_RECORDS_PER_PIPELINE = 10;`
- [x] 2.2 在 `BuildPipelineServiceImpl` 新增私有方法
      `pruneOldRecords(String pipelineId)`：调用
      `recordRepository.findByBuildPipelineId(pipelineId)`（已按 `startedAt`
      倒序），若 `size > MAX_RECORDS_PER_PIPELINE`，对第 10 条之后的每条
      记录调用 `deleteById`
- [x] 2.3 在同步入口 `execute()` 的保存点（约 line 273
      `recordRepository.save(record)` 之后）调用 `pruneOldRecords(pipelineId)`
- [x] 2.4 在异步入口 `executeWithProgress()` 的保存点（约 line 571
      `recordRepository.save(record)` 之后）调用 `pruneOldRecords(pipelineId)`

## 3. 测试

- [x] 3.1 为 `JsonFileBuildRecordRepository.deleteById` 补充单测：删除已存在
      记录成功；删除不存在的 id 不抛异常
- [x] 3.2 为清理逻辑补充单测（`BuildPipelineServiceImplTest`）：
      历史未超 10 条时不删除；超过 10 条时按 `startedAt` 删除最旧、保留 10 条；
      失败构建同样触发清理；不同 pipeline 互不影响

## 4. 回归与验证

- [x] 4.1 运行 `mvn test`，确认全项目测试全绿（module-pipeline 22 + module-web 58，
      含 BuildRecord/BuildPipeline 集成测试，零回归，BUILD SUCCESS）
- [x] 4.2 端到端收敛验证：原计划手动连续触发 execute（需真实订阅源/Mihomo），
      已用等价自动化测试覆盖其核心断言 —— 超量删最旧见 `HistoryPruningTests`、
      真实文件删除见 `JsonFileBuildRecordRepositoryTest.deleteById_*`、pipeline 隔离
      见 `findByBuildPipelineId_onlyReturnsMatchingPipeline`。详见 verify.md §7。
      部署期仍建议做一次真实 dogfood。
