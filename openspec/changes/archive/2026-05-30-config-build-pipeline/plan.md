# 配置构建流程 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 实现 BuildPipeline 构建流程功能，支持订阅拉取、脚本处理、推送到 mihomo、手动/定时触发、构建历史记录。

**Architecture:** 新建 module-pipeline 模块，包含 BuildPipeline 和 BuildRecord 模型及服务。BuildPipelineService 编排完整构建链路（订阅 → Pipeline → 推送），SchedulerService 感知 cron 配置变更。前端新增构建流程管理页面。

**Tech Stack:** Java 21, Spring Boot, Maven 多模块, Vue 3 + Element Plus + TypeScript

---

## Task 1: Maven 模块搭建

- [ ] **Step 1:** 创建 `module-pipeline/pom.xml`，parent 指向根 pom，dependencies 包含 module-common、module-subscription、module-processor、module-mihomo
- [ ] **Step 2:** 创建目录结构 `module-pipeline/src/main/java/site/kael/clash/pipeline/{model,repository,service,service/impl}`
- [ ] **Step 3:** 在根 `pom.xml` 的 `<modules>` 中添加 `<module>module-pipeline</module>`
- [ ] **Step 4:** 在 `module-web/pom.xml` 中添加对 module-pipeline 的 dependency

**Commit:** `feat(pipeline): 初始化 module-pipeline Maven 模块`

---

## Task 2: 数据模型

- [ ] **Step 1:** 创建 `BuildPipeline.java` — 字段：id, name, primarySubscriptionId, additionalSubscriptionIds(List), scriptName, targetInstanceId, cronExpression, enabled, createdAt, updatedAt, lastRunAt, lastRunStatus
- [ ] **Step 2:** 创建 `BuildRecord.java` — 字段：id, buildPipelineId, startedAt, finishedAt, status, errorMessage, logs(List)

**Commit:** `feat(pipeline): 添加 BuildPipeline 和 BuildRecord 数据模型`

---

## Task 3: Repository 层

- [ ] **Step 1:** 创建 `BuildPipelineRepository.java` 接口（CRUD + findAll）
- [ ] **Step 2:** 创建 `JsonFileBuildPipelineRepository.java`，参考 JsonFileScheduledTaskRepository 实现，存储路径 `{data.path}/build-pipelines/`
- [ ] **Step 3:** 创建 `BuildRecordRepository.java` 接口（save、findById、findByBuildPipelineId）
- [ ] **Step 4:** 创建 `JsonFileBuildRecordRepository.java`，存储路径 `{data.path}/build-records/`

**Commit:** `feat(pipeline): 添加 BuildPipeline 和 BuildRecord Repository 层`

---

## Task 4: Service 层 - CRUD

- [ ] **Step 1:** 创建 `BuildPipelineService.java` 接口（CRUD + execute + findRecords + findRecordById）
- [ ] **Step 2:** 创建 `BuildPipelineServiceImpl.java`，实现 CRUD 方法（注入 BuildPipelineRepository，ID 生成使用 IdGenerator）

**Commit:** `feat(pipeline): 添加 BuildPipelineService CRUD 实现`

---

## Task 5: Service 层 - 构建执行

- [ ] **Step 1:** 在 `BuildPipelineServiceImpl` 中注入 SubscriptionService、PipelineService、MihomoService、BuildRecordRepository
- [ ] **Step 2:** 实现 `execute(pipelineId)` 方法：
  1. 加载 BuildPipeline
  2. 调用 SubscriptionService.fetch(primarySubscriptionId) 获取 ClashConfig
  3. 遍历 additionalSubscriptionIds，调用 fetch 并合并 ProxyNode
  4. 如 scriptName 不为空，生成 PipelineConfig（含 ScriptProcessor 步骤）并调用 PipelineService.execute
  5. 调用 MihomoService.pushConfig(targetInstanceId, config)
  6. 创建 BuildRecord 记录结果
- [ ] **Step 3:** 实现 findRecords(pipelineId) 和 findRecordById(recordId)

**Commit:** `feat(pipeline): 实现构建执行核心逻辑`

---

## Task 6: 定时调度集成

- [ ] **Step 1:** 修改 `SchedulerServiceImpl`，添加 BuildPipelineRepository 和 BuildPipelineService 依赖
- [ ] **Step 2:** 在 `@PostConstruct init()` 中加载 enabled BuildPipeline（cronExpression 不为空），注册 cron 任务调用 BuildPipelineService.execute
- [ ] **Step 3:** 在 BuildPipelineServiceImpl 的 create/update/delete 中调用 SchedulerService 同步 cron 任务

**Commit:** `feat(pipeline): 集成定时调度到 BuildPipeline`

---

## Task 7: REST API

- [ ] **Step 1:** 创建 `BuildPipelineController.java`（/api/build-pipelines），实现 CRUD + POST /{id}/execute + GET /{id}/records
- [ ] **Step 2:** 创建 `BuildRecordController.java`（/api/build-records/{id}），实现 GET 查询详情

**Commit:** `feat(pipeline): 添加构建流程 REST API`

---

## Task 8: 前端 - API 与路由

- [ ] **Step 1:** 创建 `src/api/build-pipeline.ts`，定义 BuildPipeline/BuildRecord TypeScript 接口和 API 调用方法
- [ ] **Step 2:** 在 `src/router/index.ts` 中添加 `/build-pipelines` 路由指向 BuildPipelineView
- [ ] **Step 3:** 在 `App.vue` 的 menuItems 中添加"构建流程"菜单项（路径 /build-pipelines，图标 SetUp）

**Commit:** `feat(web): 添加构建流程 API 层和路由`

---

## Task 9: 前端 - 构建流程管理页面

- [ ] **Step 1:** 创建 `BuildPipelineView.vue`，实现列表表格（名称、主订阅、脚本、目标实例、cron、启用状态、最近状态、操作）
- [ ] **Step 2:** 实现新建/编辑弹窗表单（名称、主订阅下拉、额外订阅多选、脚本下拉、实例下拉、cron 输入、启用开关）
- [ ] **Step 3:** 实现手动触发构建（调用 execute API，显示结果提示）
- [ ] **Step 4:** 实现构建历史抽屉（el-drawer 内展示 records 列表，支持展开查看日志）

**Commit:** `feat(web): 实现构建流程管理页面`
