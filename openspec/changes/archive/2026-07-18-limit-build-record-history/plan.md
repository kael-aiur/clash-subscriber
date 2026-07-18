# 限制构建历史数量 Implementation Plan

> **For agentic workers:** 按 Task 顺序实现，每个 Task 内遵循 TDD（先红后绿）。
> superpowers:subagent-driven-development skill 在本环境不可用，直接按下列
> micro-steps 执行即可。

**Goal:** 每个构建流程保留最新 10 条 BuildRecord，超出按 `startedAt` 删除。

**Architecture:** 在 Repository 层新增 `deleteById` 封装文件删除；在
`BuildPipelineServiceImpl` 新增 `pruneOldRecords(pipelineId)` 复用既有倒序查询
做编排，并在同步/异步两个保存点之后调用。无 API/DB/依赖变更。

**Tech Stack:** Java 21、Maven、Spring Boot、Jackson、JUnit 5 + Mockito。

涉及文件：
- `module-pipeline/src/main/java/site/kael/clash/pipeline/repository/BuildRecordRepository.java`
- `module-pipeline/src/main/java/site/kael/clash/pipeline/repository/JsonFileBuildRecordRepository.java`
- `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`
- `module-pipeline/src/test/java/site/kael/clash/pipeline/repository/JsonFileBuildRecordRepositoryTest.java`（新建）
- `module-pipeline/src/test/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImplTest.java`（扩展）

---

## Task 1: Repository 新增 deleteById（TDD）

- [ ] **Step 1:** 新建
      `module-pipeline/src/test/java/site/kael/clash/pipeline/repository/JsonFileBuildRecordRepositoryTest.java`，
      用 `@TempDir` 构造一个指向临时目录的 repository（通过反射注入 `recordDir`，
      或直接 `new JsonFileBuildRecordRepository(new ObjectMapper(), tempDir.toString())`）。
      写两个用例：`deleteById_已存在记录_文件被删除`、`deleteById_不存在_不抛异常`。
      运行 `mvn test -Dtest=JsonFileBuildRecordRepositoryTest`（预期编译失败：接口无
      `deleteById`）。
- [ ] **Step 2:** 在 `BuildRecordRepository` 接口新增：
      ```java
      void deleteById(String id);
      ```
- [ ] **Step 3:** 在 `JsonFileBuildRecordRepository` 实现：
      ```java
      @Override
      public void deleteById(String id) {
          Path filePath = recordDir.resolve(id + ".json");
          try {
              Files.deleteIfExists(filePath);
              log.debug("删除构建记录: {}", filePath);
          } catch (IOException e) {
              log.warn("删除构建记录失败: {}, 原因: {}", id, e.getMessage());
          }
      }
      ```
      运行 `mvn test -Dtest=JsonFileBuildRecordRepositoryTest`（预期绿）。
- [ ] **Commit:** `feat(pipeline): BuildRecordRepository 支持按 id 删除记录`

## Task 2: Service 清理编排与接入（TDD）

- [ ] **Step 1:** 在 `BuildPipelineServiceImplTest` 新增 `@Nested` 测试类
      `PruneOldRecords`。Mock `recordRepository.findByBuildPipelineId(...)` 返回
      构造的记录列表，验证：
      - 9 条 → `deleteById` 从未被调用
      - 11 条 → `deleteById` 被调用 1 次，且参数是最旧那条（`startedAt` 最早）的 id
      - 失败构建路径同样触发（通过 `execute` 抛异常用例验证清理仍被调用）
      运行 `mvn test -Dtest=BuildPipelineServiceImplTest`（预期红：方法不存在）。
- [ ] **Step 2:** 在 `BuildPipelineServiceImpl` 顶部加常量：
      ```java
      private static final int MAX_RECORDS_PER_PIPELINE = 10;
      ```
- [ ] **Step 3:** 在 `BuildPipelineServiceImpl` 新增私有方法（放在 `findRecordById`
      附近的「构建历史」区块）：
      ```java
      private void pruneOldRecords(String pipelineId) {
          List<BuildRecord> records = recordRepository.findByBuildPipelineId(pipelineId);
          if (records.size() <= MAX_RECORDS_PER_PIPELINE) {
              return;
          }
          // findByBuildPipelineId 已按 startedAt 倒序，第 MAX 条之后均为待删除
          for (int i = MAX_RECORDS_PER_PIPELINE; i < records.size(); i++) {
              recordRepository.deleteById(records.get(i).getId());
          }
      }
      ```
- [ ] **Step 4:** 在同步入口 `execute()` 保存点（line 273
      `return recordRepository.save(record);`）改为先保存、再清理：
      ```java
      recordRepository.save(record);
      pruneOldRecords(pipelineId);
      return record;
      ```
- [ ] **Step 5:** 在异步入口 `executeWithProgress()` 保存点（line 571
      `recordRepository.save(record);`）之后追加：
      ```java
      pruneOldRecords(pipelineId);
      ```
- [ ] **Step 6:** 运行 `mvn test -Dtest=BuildPipelineServiceImplTest`（预期绿）。
- [ ] **Commit:** `feat(pipeline): 构建完成后按 pipeline 保留最新 10 条历史`

## Task 3: 全量回归与手动验证

- [ ] **Step 1:** 运行 `mvn test`，确认整个 `module-pipeline`（及依赖模块）全绿。
- [ ] **Step 2:** 启动应用，对同一构建流程连续手动触发构建 > 10 次，检查
      `data/build-records/` 下该 `buildPipelineId` 的记录收敛到 10 条，且最近一次
      （`startedAt` 最新）的记录仍在。
- [ ] **Step 3:** 触发另一个不同 pipeline 的构建，确认其历史不受第一个 pipeline
      清理影响。
- [ ] **Commit（可选）:** 无代码变更，仅验证。
