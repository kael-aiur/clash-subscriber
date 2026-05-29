## Why

当前 clash-subscriber 项目仅有 pom.xml，无任何源码。需要从零搭建模块结构，为后续功能开发奠定基础。模块划分的清晰度直接影响后续开发效率和代码可维护性。

## What Changes

**模块结构**
- From: 空项目，无模块划分
- To: 6 个 Maven 子模块（common、subscription、processor、mihomo、scheduler、web），领域驱动架构
- Reason: 按业务领域划分模块，职责清晰，便于独立开发和测试
- Impact: 非破坏性变更，纯新增

**存储层**
- From: 无存储
- To: Repository 接口 + JSON 文件实现，数据目录结构化存储
- Reason: 轻量无依赖，接口抽象保证可切换
- Impact: 非破坏性变更

**处理 Pipeline**
- From: 无处理能力
- To: 处理器链模式 + GraalVM JS 脚本扩展
- Reason: 内置处理器覆盖常见场景，脚本引擎支持自定义逻辑
- Impact: 非破坏性变更

## Capabilities

### New Capabilities

- `subscription-management`: 订阅源的 CRUD、获取解析、缓存管理
- `config-processing`: 配置处理 Pipeline，包括节点合并、规则修改、代理组重组、脚本扩展
- `mihomo-instance`: Mihomo 实例的注册管理、健康检查、配置推送
- `scheduled-task`: 定时任务的创建管理、cron 调度、执行记录
- `rest-api`: 统一的 REST API 层，提供所有功能的 HTTP 接口
- `web-ui`: Vue 3 前端管理界面

### Modified Capabilities

无（全新项目）

## Impact

- **代码**: 新增 6 个 Maven 子模块，约 20+ 个 Java 类
- **依赖**: Spring Boot 3、GraalVM JS、Jackson（JSON）、SnakeYAML（YAML 解析）、OkHttp（HTTP 客户端）
- **API**: 新增 REST API 端点（订阅源、Pipeline、Mihomo 实例、定时任务、脚本）
- **部署**: 单体 Spring Boot 应用，首次运行自动创建 data/ 目录
