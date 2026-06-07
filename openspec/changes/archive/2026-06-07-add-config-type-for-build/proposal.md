## Why

当前构建流程只支持订阅源模式，用户需要手动选择主订阅和额外订阅，配置流程繁琐。用户希望支持直接选择配置管理对象（ConfigProfile），简化配置流程，并确保每次构建都使用最新的配置。这个功能可以提高配置效率，减少配置错误，提升用户体验。

## What Changes

**配置类型选择**
- From: 构建流程只支持订阅源模式，需要选择主订阅和额外订阅
- To: 构建流程支持两种配置类型：订阅源模式和配置组合模式
- Reason: 简化配置流程，支持直接选择配置管理对象
- Impact: 非破坏性变更，现有构建流程自动迁移为订阅源模式

**配置组合模式执行**
- From: 无配置组合模式支持
- To: 配置组合模式下，直接调用 ConfigGeneratorService.generate() 生成完整配置
- Reason: 配置组合已经包含完整的配置信息，避免重复处理
- Impact: 新增功能，不影响现有功能

**缓存策略**
- From: 无明确缓存策略
- To: 每次构建都重新生成配置，不使用缓存
- Reason: 确保每次构建都使用最新的配置
- Impact: 非破坏性变更，配置生成逻辑不变

## Capabilities

### New Capabilities
- `build-pipeline-config-type`: 构建流程配置类型功能，支持订阅源模式和配置组合模式选择

### Modified Capabilities
- `build-pipeline-execution`: 构建流程执行逻辑，需要支持配置组合模式的配置获取和处理

## Impact

**代码变更**
- BuildPipeline 模型：增加 configType 和 configProfileId 字段
- BuildPipelineServiceImpl：修改 execute() 方法，支持配置组合模式
- BuildPipelineRepository：增加按配置类型查询的方法

**API 变更**
- POST /api/build-pipelines：支持新增字段
- PUT /api/build-pipelines/{id}：支持新增字段
- 其他 API 接口响应包含新增字段

**数据库变更**
- build_pipeline 表：新增 config_type 和 config_profile_id 字段
- 新增索引：idx_build_pipeline_config_type, idx_build_pipeline_config_profile_id

**依赖变更**
- 无新增依赖，使用现有的 ConfigGeneratorService

**系统影响**
- 向后兼容：现有构建流程自动迁移为订阅源模式
- 定时任务：支持配置组合模式的定时执行
- 监控告警：需要监控迁移状态和执行异常
