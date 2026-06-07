# 构建流程配置类型 - 头脑风暴记录

## 背景

当前构建流程（BuildPipeline）只支持订阅源模式，需要选择主订阅和额外订阅。用户希望支持直接选择配置管理对象（ConfigProfile），简化配置流程，并确保每次构建都使用最新的配置。

## 需求澄清

### 问题 1：配置类型的选择方式
**问题**：当用户创建构建流程时，是否应该提供一个配置类型选择：
- **订阅源模式**（当前方式）：选择主订阅和额外订阅
- **配置组合模式**（新方式）：选择一个 ConfigProfile 对象

**回答**：对

### 问题 2：配置组合的执行方式
**问题**：当选择"配置组合模式"时，执行构建流程应该如何工作：
- **选项 A**：直接调用 `ConfigGeneratorService.generate(profile)` 直接生成完整的 Clash 配置
- **选项 B**：从 ConfigProfile 中提取 `subscriptionIds` 列表，然后按照现有的订阅源模式处理

**回答**：A（直接生成完整配置）

### 问题 3：缓存问题
**问题**：用户提到"每次构建都重新运行配置更新，不要使用缓存"。这里的"缓存"是指：
- **选项 A**：订阅源配置缓存
- **选项 B**：配置组合生成缓存
- **选项 C**：两者都需要

**回答**：B（配置组合生成缓存）

### 问题 4：向后兼容性
**问题**：现有的 BuildPipeline 都是使用"订阅源模式"。当新增"配置组合模式"后：
- **选项 A**：自动迁移，将现有的 BuildPipeline 自动标记为"订阅源模式"
- **选项 B**：不迁移，现有的 BuildPipeline 保持原样

**回答**：A（自动迁移）

### 问题 5：配置组合模式下的脚本处理
**问题**：当选择"配置组合模式"时，`ConfigGeneratorService.generate()` 已经会生成完整的 Clash 配置。此时：
- **选项 A**：跳过脚本处理
- **选项 B**：仍然支持脚本处理

**回答**：B（仍然支持脚本处理）

## 设计方案

### 方案 1：扩展 BuildPipeline 模型（推荐）
**改动范围**：最小改动

**实现方式**：
- 在 BuildPipeline 模型中增加 `configType` 字段（enum: `"subscription"` 或 `"config-profile"`）
- 增加 `configProfileId` 字段
- 修改 `BuildPipelineServiceImpl.execute()` 方法，根据 `configType` 选择不同的配置来源
- 现有 BuildPipeline 自动迁移为 `configType = "subscription"`

**优点**：
- 改动最小，保持现有架构
- 向后兼容性好
- 实现简单，风险低

**缺点**：
- BuildPipeline 模型变得复杂
- 执行逻辑中有条件分支

### 方案 2：策略模式
**改动范围**：中等改动

**实现方式**：
- 定义 `BuildPipelineStrategy` 接口
- 实现 `SubscriptionBuildStrategy` 和 `ConfigProfileBuildStrategy`
- 在 `BuildPipelineServiceImpl` 中根据 `configType` 选择策略

**优点**：
- 代码结构清晰，职责分离
- 易于扩展新的配置类型

**缺点**：
- 改动较大，需要重构

### 方案 3：组合模式
**改动范围**：较大改动

**实现方式**：
- 将配置来源抽象为 `ConfigSource` 接口
- 实现 `SubscriptionConfigSource` 和 `ConfigProfileConfigSource`
- BuildPipeline 模型包含 `ConfigSource` 对象

**优点**：
- 灵活性最高
- 易于扩展和测试

**缺点**：
- 改动最大，需要重构

## 设计决策

**选择方案 1：扩展 BuildPipeline 模型**

理由：
1. 改动最小，风险最低
2. 向后兼容性好
3. 实现简单，易于理解和维护
4. 满足当前需求，未来需要时可以再重构

## 执行流程对比

| 步骤 | 订阅源模式 | 配置组合模式 |
|------|-----------|-------------|
| 1 | 拉取主订阅配置 | 获取配置组合并生成完整配置 |
| 2 | 合并额外订阅节点 | （跳过） |
| 3 | 脚本处理（可选） | 脚本处理（可选） |
| 4 | 推送到 Mihomo | 推送到 Mihomo |

## 关键设计点

1. **数据模型**：BuildPipeline 增加 configType 和 configProfileId 字段
2. **执行逻辑**：根据 configType 选择不同的配置来源
3. **向后兼容**：configType 为 null 时自动迁移为 "subscription"
4. **缓存策略**：每次构建都重新生成配置，不使用缓存
5. **脚本处理**：两种模式都支持脚本处理
