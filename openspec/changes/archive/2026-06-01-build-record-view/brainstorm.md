# Brainstorm: 构建记录查看功能

## 背景

当前构建流程页面（BuildPipelineView.vue）已经有一个历史抽屉，点击"历史"按钮可以查看构建记录列表。但用户体验不够直观，需要：
1. 在表格行内直接展开查看构建记录（展开行）
2. 点击记录进入详情页，查看流程图和每个环节的输入输出

## 现有架构分析

### 数据模型
- **BuildPipeline**: 构建流程定义
- **BuildRecord**: 构建记录，包含 id, buildPipelineId, startedAt, finishedAt, status, errorMessage, logs

### 执行流程（BuildPipelineServiceImpl.execute）
当前构建流程有 4 个环节：
1. **拉取主订阅配置** - 从主订阅源获取 Clash 配置
2. **合并额外订阅节点** - 合并额外订阅源的代理节点
3. **脚本处理** - 执行自定义脚本处理配置
4. **推送到 Mihomo** - 将配置推送到目标 Mihomo 实例

### 当前问题
- BuildRecord 的 `logs` 字段只记录了文本日志，没有结构化的环节信息
- 无法区分每个环节的输入和输出
- 前端没有流程图展示

## 设计决策

### Q1: 如何记录每个环节的执行情况？

**方案 A**: 在 BuildRecord 中增加 `steps` 字段
```java
private List<BuildStep> steps = new ArrayList<>();
```

**方案 B**: 创建独立的 BuildStepRecord 表/文件

**决策**: 选择方案 A，因为：
- 环节数据与构建记录强关联，不需要独立查询
- JSON 文件存储方式下，嵌套结构更简单
- 减少文件数量和查询复杂度

### Q2: BuildStep 应该包含哪些信息？

```java
public class BuildStep {
    private String name;           // 环节名称
    private String status;         // SUCCESS/FAILED/SKIPPED
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Object input;          // 输入数据（序列化为 Map 或字符串）
    private Object output;         // 输出数据
    private String errorMessage;   // 错误信息
}
```

### Q3: 前端展开行如何实现？

使用 Element Plus 的 `el-table` 展开行功能：
- 表格第一列改为展开列（type="expand"）
- 展开后显示最近 N 条构建记录（如 5 条）
- 每条记录显示状态标签、时间、错误信息
- 点击记录跳转到详情页

### Q4: 详情页流程图如何设计？

**方案 A**: 使用 CSS + HTML 实现简单的步骤条
**方案 B**: 使用第三方库（如 vue-flow）实现真正的流程图
**方案 C**: 使用 Element Plus 的 Steps 步骤条组件

**决策**: 选择方案 C，因为：
- 构建流程是线性的 4 个步骤，不需要复杂的流程图
- Element Plus Steps 组件已集成，无需额外依赖
- 可以清晰展示每个环节的状态和进度

## 技术方案

### 后端改动
1. 新增 `BuildStep` 模型类
2. 修改 `BuildRecord`，增加 `steps` 字段
3. 修改 `BuildPipelineServiceImpl.execute()`，记录每个环节的输入输出
4. 确保 `BuildRecordController.get()` 返回完整的 steps 数据

### 前端改动
1. 修改 `BuildPipelineView.vue`：
   - 移除历史抽屉
   - 表格增加展开列
   - 展开后显示构建记录列表
2. 新建 `BuildRecordDetailView.vue`：
   - 使用 Steps 组件展示流程图
   - 每个环节可点击查看输入输出
   - 输入输出使用 JSON 格式化展示
3. 修改路由，增加 `/build-records/:id`
4. 修改 API 类型定义，增加 BuildStep 接口

## 风险和权衡

### 风险
1. **数据迁移**: 现有 BuildRecord 没有 steps 字段，需要兼容
   - 缓解：读取时 steps 为空则从 logs 解析（降级方案）
2. **存储大小**: 记录每个环节的输入输出会增加存储
   - 缓解：可配置是否记录详细数据，或限制记录数量

### 权衡
- **简单 vs 完整**: 选择线性步骤条而非复杂流程图，牺牲了一些灵活性但大大降低了实现复杂度
- **实时 vs 历史**: 详情页只展示历史数据，不支持实时查看执行过程（未来可扩展）
