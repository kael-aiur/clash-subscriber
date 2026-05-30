# 配置构建流程 - 设计文档

## 背景

当前系统中，定时任务（ScheduledTask）直接编排整个流程：拉取所有订阅 → 合并 → 执行 Pipeline → 推送到目标实例。这种设计存在以下问题：

1. 订阅获取逻辑硬编码在 SchedulerServiceImpl 中，无法单独触发
2. 每次执行拉取所有订阅，无法按流程指定特定订阅
3. 定时任务与构建逻辑耦合，职责不清晰
4. 没有构建历史记录，只能看到最近一次执行状态

用户需要一个"配置构建流程"功能，将构建逻辑独立出来，支持手动和定时触发，并保留完整的构建历史。

## 核心决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 数据源 | 混合模式（主订阅 + 可选额外订阅） | 兼顾简单场景和合并需求 |
| 脚本处理 | 直接关联脚本 + 自动生成 PipelineConfig | 简化 UI，复用底层 Pipeline 能力 |
| 目标实例 | 单个 mihomo 实例 | 简单明确，一对一关系 |
| 定时调度 | 内嵌到构建流程 | 定时是构建流程的属性，无需独立实体 |
| 构建历史 | 完整记录（时间、状态、日志） | 便于排查问题和审计 |
| 实体命名 | BuildPipeline（构建流程） | 准确表达完整构建管道的含义 |
| 模块方案 | 新建独立模块 module-pipeline | 职责清晰，不破坏现有模块依赖 |

## 数据模型

### BuildPipeline（构建流程）

```java
public class BuildPipeline {
    private String id;
    private String name;

    // 数据源：主订阅 + 可选的额外订阅
    private String primarySubscriptionId;
    private List<String> additionalSubscriptionIds;

    // 处理：可选脚本
    private String scriptName;  // 关联脚本文件名，为空则不处理

    // 目标：单个 mihomo 实例
    private String targetInstanceId;

    // 定时：内嵌调度配置
    private String cronExpression;  // 为空则不定时执行
    private boolean enabled;

    // 时间戳
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private String lastRunStatus;  // SUCCESS / FAILED / RUNNING
}
```

### BuildRecord（构建历史）

```java
public class BuildRecord {
    private String id;
    private String buildPipelineId;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String status;        // SUCCESS / FAILED
    private String errorMessage;  // 失败时的错误信息
    private List<String> logs;    // 执行日志（ProcessingContext.logs）
}
```

**存储：** JSON 文件存储，路径为 `{data.path}/build-pipelines/` 和 `{data.path}/build-records/`，与现有实体存储方式一致。

## 执行流程

`BuildPipelineService.execute(pipelineId)` 的执行步骤：

```
1. 加载 BuildPipeline 配置
2. 拉取订阅配置
   ├── 拉取 primarySubscription → ClashConfig
   └── 遍历 additionalSubscriptionIds → 合并 ProxyNode 到 ClashConfig
3. 构建 PipelineConfig（自动生成）
   ├── 如果 scriptName 不为空 → 添加 ScriptProcessor 步骤
   └── 否则 → 空 Pipeline（直接透传）
4. 执行 PipelineService.execute(pipelineConfig, clashConfig)
5. 推送到 targetInstance → MihomoService.pushConfig()
6. 记录 BuildRecord（状态、日志、时间）
```

## 服务层设计

```java
public interface BuildPipelineService {
    // CRUD
    BuildPipeline create(BuildPipeline pipeline);
    BuildPipeline update(BuildPipeline pipeline);
    BuildPipeline findById(String id);
    List<BuildPipeline> findAll();
    void deleteById(String id);

    // 执行
    BuildRecord execute(String pipelineId);

    // 构建历史
    List<BuildRecord> findRecords(String pipelineId);
    BuildRecord findRecordById(String recordId);
}
```

## 定时调度集成

`SchedulerServiceImpl` 简化职责：

- `@PostConstruct init()` 加载所有 cronExpression 不为空且 enabled=true 的 BuildPipeline，注册 cron 任务
- cron 触发时调用 `BuildPipelineService.execute(pipelineId)`
- BuildPipeline 的 CRUD 操作中，同步更新/注册/取消 cron 任务

现有的 `ScheduledTask` 实体标记为废弃，后续版本清理。

## REST API

```
/api/build-pipelines
  GET            -- 列表
  POST           -- 创建
  GET    /{id}   -- 详情
  PUT    /{id}   -- 更新
  DELETE /{id}   -- 删除
  POST   /{id}/execute   -- 手动触发构建
  GET    /{id}/records   -- 获取构建历史列表

/api/build-records/{id}   -- 单条构建记录详情
```

遵循现有 REST 风格：无包装 envelope，删除返回 204，操作类端点用 POST + 动词。

## 前端设计

### 侧边栏

新增菜单项：`构建流程`（路径 `/build-pipelines`，图标 `SetUp`）

### 构建流程列表页 BuildPipelineView.vue

- 表格列：名称、主订阅、脚本、目标实例、定时表达式、启用状态、最近执行状态、操作
- 操作按钮：编辑、手动触发构建、查看历史、删除
- 新建/编辑弹窗表单：
  - 名称（文本输入）
  - 主订阅（下拉选择）
  - 额外订阅（多选，可选）
  - 脚本（下拉选择，可选）
  - mihomo 实例（下拉选择）
  - cron 表达式（文本输入，可选）
  - 启用开关

### 构建历史弹窗/抽屉

- 从列表页点击"查看历史"打开
- 展示该流程的所有 BuildRecord，按时间倒序
- 每条记录：开始时间、结束时间、状态、错误信息、日志（可展开）

### 定时任务菜单

现有"定时任务"菜单保留但标记废弃提示，或直接移除。

## 模块结构

新建 `module-pipeline` Maven 模块：

```
module-pipeline/
  src/main/java/site/kael/clash/pipeline/
    model/
      BuildPipeline.java
      BuildRecord.java
    repository/
      BuildPipelineRepository.java
      BuildRecordRepository.java
      JsonFileBuildPipelineRepository.java
      JsonFileBuildRecordRepository.java
    service/
      BuildPipelineService.java
      impl/BuildPipelineServiceImpl.java
```

依赖关系：`module-pipeline` → `module-subscription`, `module-processor`, `module-mihomo`, `module-common`

`module-web` 新增：
- `controller/BuildPipelineController.java`
- `controller/BuildRecordController.java`
- `frontend/src/views/BuildPipelineView.vue`
- `frontend/src/api/build-pipeline.ts`
