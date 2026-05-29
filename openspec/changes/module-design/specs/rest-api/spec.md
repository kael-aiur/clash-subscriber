## ADDED Requirements

### Requirement: 订阅源 API

系统 SHALL 提供订阅源的 REST API 端点。

#### Scenario: 订阅源 CRUD 端点
- **WHEN** 客户端调用 /api/subscriptions 端点
- **THEN** 系统提供 GET（列表）、POST（创建）、GET /{id}（详情）、PUT /{id}（更新）、DELETE /{id}（删除）操作

#### Scenario: 手动获取订阅
- **WHEN** 客户端调用 POST /api/subscriptions/{id}/fetch
- **THEN** 系统立即获取并解析该订阅源，返回 ClashConfig

---

### Requirement: Pipeline API

系统 SHALL 提供 Pipeline 的 REST API 端点。

#### Scenario: Pipeline CRUD 端点
- **WHEN** 客户端调用 /api/pipelines 端点
- **THEN** 系统提供 GET、POST、GET /{id}、PUT /{id}、DELETE /{id} 操作

#### Scenario: 执行 Pipeline
- **WHEN** 客户端调用 POST /api/pipelines/{id}/execute
- **THEN** 系统执行该 Pipeline，返回处理结果

---

### Requirement: Mihomo 实例 API

系统 SHALL 提供 Mihomo 实例的 REST API 端点。

#### Scenario: 实例 CRUD 端点
- **WHEN** 客户端调用 /api/mihomo-instances 端点
- **THEN** 系统提供 GET、POST、GET /{id}、PUT /{id}、DELETE /{id} 操作

#### Scenario: 健康检查端点
- **WHEN** 客户端调用 GET /api/mihomo-instances/{id}/health
- **THEN** 系统返回该实例的健康状态

#### Scenario: 推送配置端点
- **WHEN** 客户端调用 POST /api/mihomo-instances/{id}/push，附带配置内容
- **THEN** 系统将配置推送到该实例，返回推送结果

---

### Requirement: 定时任务 API

系统 SHALL 提供定时任务的 REST API 端点。

#### Scenario: 任务 CRUD 端点
- **WHEN** 客户端调用 /api/scheduled-tasks 端点
- **THEN** 系统提供 GET、POST、PUT /{id}、DELETE /{id} 操作

#### Scenario: 手动触发端点
- **WHEN** 客户端调用 POST /api/scheduled-tasks/{id}/trigger
- **THEN** 系统立即执行该任务，返回执行结果

---

### Requirement: 脚本管理 API

系统 SHALL 提供脚本的 REST API 端点。

#### Scenario: 脚本 CRUD 端点
- **WHEN** 客户端调用 /api/scripts 端点
- **THEN** 系统提供 GET（列表）、POST（上传）、GET /{name}（内容）、DELETE /{name}（删除）操作
