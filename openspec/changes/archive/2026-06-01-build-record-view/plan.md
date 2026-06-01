# 构建记录查看功能实现计划

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 在构建流程表格中增加展开行查看构建记录，并提供独立的构建记录详情页展示流程图和每个环节的输入输出。

**Architecture:** 后端扩展 BuildRecord 模型增加 steps 字段记录每个构建环节详情，前端改造表格为展开行交互并新增详情页使用 Steps 组件展示流程图。

**Tech Stack:** Java 21, Spring Boot, Vue 3, Element Plus, TypeScript

---

## Task 1: 后端模型扩展

- [ ] **Step 1:** 新建 `module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildStep.java`
  ```java
  package site.kael.clash.pipeline.model;

  import java.time.LocalDateTime;

  public class BuildStep {
      private String name;
      private String status; // SUCCESS, FAILED, SKIPPED
      private LocalDateTime startedAt;
      private LocalDateTime finishedAt;
      private Object input;
      private Object output;
      private String errorMessage;

      // getter/setter 省略
  }
  ```

- [ ] **Step 2:** 修改 `module-pipeline/src/main/java/site/kael/clash/pipeline/model/BuildRecord.java`
  - 增加字段: `private List<BuildStep> steps = new ArrayList<>();`
  - 增加 getter/setter

- [ ] **Step 3:** 修改 `module-web/frontend/src/api/build-pipeline.ts`
  - 增加 BuildStep 接口定义
  - 更新 BuildRecord 接口增加 steps 字段

---

## Task 2: 后端执行逻辑增强

- [ ] **Step 1:** 修改 `module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`
  - 在 execute() 方法开始时创建 steps 列表
  - 每个环节创建 BuildStep 对象，记录 name、startedAt、input
  - 环节完成后记录 finishedAt、status、output
  - 异常时记录 errorMessage 和 status=FAILED

- [ ] **Step 2:** 实现 4 个环节的记录逻辑
  - 环节1 "拉取主订阅": input=订阅ID, output=节点数
  - 环节2 "合并额外订阅": input=额外订阅ID列表, output=合并后节点总数
  - 环节3 "脚本处理": input=脚本名称, output=处理结果 (无脚本时 SKIPPED)
  - 环节4 "推送到 Mihomo": input=目标实例ID, output=推送结果

---

## Task 3: 前端构建流程表格改造

- [ ] **Step 1:** 修改 `module-web/frontend/src/views/BuildPipelineView.vue`
  - 移除历史抽屉相关代码 (drawerVisible, drawerRecords 等)
  - 移除 openHistory 方法
  - 移除操作列中的"历史"按钮

- [ ] **Step 2:** 修改表格为展开行
  - 第一列改为 `<el-table-column type="expand">`
  - 展开模板显示构建记录列表

- [ ] **Step 3:** 实现展开行模板
  - 显示最近 10 条构建记录
  - 每条记录显示：状态标签、开始时间、错误信息
  - 点击记录路由跳转到 `/build-records/{id}`

---

## Task 4: 构建记录详情页

- [ ] **Step 1:** 新建 `module-web/frontend/src/views/BuildRecordDetailView.vue`
  - 页面头部显示构建记录基本信息

- [ ] **Step 2:** 实现流程图展示
  - 使用 el-steps 组件展示 4 个环节
  - 根据 steps 数据设置每个环节的状态

- [ ] **Step 3:** 实现环节详情查看
  - 点击环节显示输入输出数据
  - 使用 JSON 格式化展示

- [ ] **Step 4:** 修改 `module-web/frontend/src/router/index.ts`
  - 添加路由 `/build-records/:id` 指向 BuildRecordDetailView

---

## 提交点

- Commit 1: 后端模型扩展 (BuildStep + BuildRecord.steps)
- Commit 2: 后端执行逻辑增强 (记录每个环节详情)
- Commit 3: 前端表格改造 (展开行替代抽屉)
- Commit 4: 前端详情页 (流程图 + 环节详情)
