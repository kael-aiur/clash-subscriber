## ADDED Requirements

### Requirement: Hash 路由模式

系统 SHALL 使用 Vue Router 的 hash 模式（`createWebHashHistory()`）进行前端路由。

#### Scenario: URL 格式
- **WHEN** 用户访问前端页面
- **THEN** URL 格式为 `/#/path`（如 `/#/subscriptions`）

#### Scenario: 页面刷新
- **WHEN** 用户在任意页面刷新浏览器
- **THEN** 页面正常加载，无需服务器配置重定向
