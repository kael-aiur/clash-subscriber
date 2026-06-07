## 1. 数据库迁移

- [ ] 1.1 创建数据库迁移脚本，新增 config_type 和 config_profile_id 字段
- [ ] 1.2 创建索引 idx_build_pipeline_config_type 和 idx_build_pipeline_config_profile_id
- [ ] 1.3 编写数据迁移脚本，将现有数据的 config_type 设置为 'subscription'
- [ ] 1.4 创建回滚脚本，支持删除新增字段和索引

## 2. 模型层修改

- [ ] 2.1 修改 BuildPipeline 模型，增加 configType 和 configProfileId 字段
- [ ] 2.2 添加 ConfigType 枚举类
- [ ] 2.3 修改 BuildPipeline 模型的验证逻辑，支持配置类型验证
- [ ] 2.4 更新 BuildPipelineRepository，增加按配置类型查询的方法

## 3. 服务层修改

- [ ] 3.1 修改 BuildPipelineServiceImpl.execute() 方法，支持配置类型选择
- [ ] 3.2 实现 executeSubscriptionMode() 方法，处理订阅源模式
- [ ] 3.3 实现 executeConfigProfileMode() 方法，处理配置组合模式
- [ ] 3.4 实现向后兼容性逻辑，自动迁移 configType 为 null 的记录
- [ ] 3.5 修改 create() 和 update() 方法，支持配置类型验证

## 4. 控制层修改

- [ ] 4.1 修改 BuildPipelineController，支持新增字段的 API 请求和响应
- [ ] 4.2 更新 API 文档，说明新增字段和验证规则

## 5. 测试

- [ ] 5.1 编写单元测试，测试配置组合模式执行逻辑
- [ ] 5.2 编写单元测试，测试订阅源模式执行逻辑
- [ ] 5.3 编写单元测试，测试向后兼容性逻辑
- [ ] 5.4 编写单元测试，测试配置类型验证逻辑
- [ ] 5.5 编写集成测试，测试配置组合模式 API
- [ ] 5.6 编写集成测试，测试订阅源模式 API

## 6. 部署和验证

- [ ] 6.1 准备部署文档，说明部署步骤和回滚方案
- [ ] 6.2 配置监控告警，监控迁移状态和执行异常
- [ ] 6.3 执行数据库迁移脚本
- [ ] 6.4 部署新版本代码
- [ ] 6.5 验证订阅源模式构建流程
- [ ] 6.6 验证配置组合模式构建流程
- [ ] 6.7 验证定时任务正常执行
