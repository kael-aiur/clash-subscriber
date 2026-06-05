## Context

当前构建流程页面（BuildPipelineView.vue）提供了一个"历史"按钮，点击后以抽屉形式展示构建记录列表。该方案存在以下问题：
- 需要额外点击才能查看历史记录，不够直观
- 构建记录详情展示不够结构化，只有文本日志
- 无法直观看到每个构建环节的执行情况和数据流转

项目使用 Vue 3 + Element Plus + Java Spring Boot + JSON 文件存储。

## Goals / Non-Goals

**Goals:**
- 在构建流程表格中直接展开查看构建记录，无需跳转或打开抽屉
- 每条构建记录展示成功/失败状态标签
- 点击构建记录进入详情页，展示线性流程图
- 流程图标记每个环节的执行状态（成功/失败/跳过）
- 支持查看每个环节的输入和输出数据

**Non-Goals:**
- 不支持实时查看构建执行过程（仅历史数据）
- 不支持从流程图中重新执行某个环节
- 不改变现有的构建流程执行逻辑

## Decisions

### D1: 使用表格展开行而非抽屉
- **选择**: 使用 Element Plus 的 `el-table` 展开行（type="expand"）替代抽屉
- **理由**: 展开行可以直接在表格内查看，交互更流畅，符合用户"左边小三角"的期望
- **已考虑 alternative**: 保留抽屉方案 — 需要额外点击，不够直观

### D2: BuildStep 嵌入 BuildRecord
- **选择**: 在 BuildRecord 中增加 `List<BuildStep> steps` 字段
- **理由**: 环节数据与构建记录强关联，JSON 文件存储下嵌套结构更简单
- **已考虑 alternative**: 独立的 BuildStepRecord 文件 — 增加查询复杂度，无独立查询需求

### D3: 使用 Element Plus Steps 组件展示流程图
- **选择**: 使用 `el-steps` 组件展示线性构建流程
- **理由**: 构建流程是线性 4 步，Steps 组件已集成，无需额外依赖
- **已考虑 alternative**: vue-flow 等流程图库 — 过度设计，线性流程不需要复杂图形

### D4: 构建流程的 4 个固定环节
- **选择**: 定义 4 个固定环节：拉取主订阅、合并额外订阅、脚本处理、推送到 Mihomo
- **理由**: 与 BuildPipelineServiceImpl.execute() 的实际步骤一致
- **已考虑 alternative**: 动态定义环节 — 增加复杂度，当前流程固定

### D5: 环节输入输出的序列化格式
- **选择**: 使用 JSON 格式存储输入输出数据
- **理由**: 结构化数据便于前端格式化展示，支持折叠/展开
- **已考虑 alternative**: 纯文本日志 — 无法区分输入输出，展示不够直观

## Risks / Trade-offs

- [Risk] 现有 BuildRecord 数据没有 steps 字段 → Migration: 读取时 steps 为空则显示"无环节数据"提示
- [Risk] 记录输入输出增加存储大小 → Mitigation: 可配置记录详细级别，或限制记录数量
- [Trade-off] 使用固定 4 步而非动态步骤 → 接受理由：当前构建流程确实固定为 4 步，动态化增加不必要的复杂度

## Migration Plan

1. 后端：新增 BuildStep 类，修改 BuildRecord 增加 steps 字段（向后兼容，旧数据 steps 为空）
2. 前端：修改 BuildPipelineView.vue，替换抽屉为展开行
3. 前端：新增 BuildRecordDetailView.vue 详情页
4. 路由：增加 `/build-records/:id` 路由
5. 部署：无需特殊迁移步骤，新数据自动包含 steps 字段

## Open Questions

- 是否需要限制展开后显示的记录数量？（建议最近 10 条，更多使用分页或"查看更多"）
- 是否需要在展开行中显示简化的流程进度条？
