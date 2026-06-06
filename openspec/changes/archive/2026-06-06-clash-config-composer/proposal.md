## Why

当前项目已具备订阅源管理、节点合并、代理组处理、规则组管理等功能，但缺少一个独立的配置生成器来支持用户分享配置给他人。用户需要一个统一的入口来组合多个订阅源的节点、代理组和规则组，生成标准的 Clash YAML 配置，并通过 URL 分享给朋友或团队成员。

## What Changes

**配置生成功能**
- From: 无独立的配置生成和分享功能
- To: 提供完整的配置生成器，支持动态组合订阅源、代理组、规则组，生成标准 Clash YAML 配置
- Reason: 用户需要分享配置给他人使用
- Impact: 新增功能，不影响现有系统

**Basic Auth 认证**
- From: 无配置访问认证
- To: 支持 Basic Auth 认证保护配置访问
- Reason: 保护配置安全，防止未授权访问
- Impact: 新增功能，不影响现有系统

## Capabilities

### New Capabilities

- `config-profile-management`: 配置组合管理，包括创建、编辑、删除配置组合，支持选择订阅源、配置代理组、排序规则组、设置基础信息
- `config-generation`: 配置生成服务，根据配置组合动态生成标准 Clash YAML 配置，支持节点合并、代理组构建、规则组合并
- `config-authentication`: 配置访问认证，支持 Basic Auth 认证方式保护配置访问，验证用户名密码

### Modified Capabilities

（无）

## Impact

**代码影响**
- 新增 module-web 中的 ConfigController
- 新增 module-processor 中的 ConfigGeneratorService
- 新增 ConfigProfile、ProxyGroupConfig、ClashBasicConfig 等模型
- 新增 JsonFileConfigProfileRepository 存储实现

**API 影响**
- 新增 REST API：/api/config/* 系列接口
- 新增配置获取接口：GET /api/config/{name}（带 Basic Auth）

**依赖影响**
- 无新增外部依赖，复用现有模块

**系统影响**
- 新增数据目录：data/config-profiles/
- 不影响现有功能
