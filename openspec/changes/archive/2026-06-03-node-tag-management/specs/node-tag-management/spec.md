## ADDED Requirements

### Requirement: 节点标签 CRUD

系统 SHALL 提供节点标签的完整 CRUD 操作（创建、读取、更新、删除）。

#### Scenario: 创建标签
- **WHEN** 用户提交标签信息（名称、优先级、匹配规则字符串数组）
- **THEN** 系统生成唯一 ID，保存标签信息，设置 createdAt 和 updatedAt 时间戳，返回创建结果

#### Scenario: 查询标签列表
- **WHEN** 用户请求标签列表
- **THEN** 系统返回所有标签，按优先级升序排列（数字越小越靠前）

#### Scenario: 更新标签
- **WHEN** 用户提交标签 ID 和更新内容（名称、优先级、匹配规则）
- **THEN** 系统更新对应标签的信息，更新 updatedAt 时间戳，保留未修改的字段

#### Scenario: 删除标签
- **WHEN** 用户提交标签 ID 请求删除
- **THEN** 系统删除该标签

---

### Requirement: 标签数据存储

系统 SHALL 使用 JSON 文件持久化存储标签数据。

#### Scenario: 标签持久化
- **WHEN** 系统创建或更新标签
- **THEN** 标签数据以 JSON 格式保存到 `data/node-tags/{id}.json` 文件

#### Scenario: 标签加载
- **WHEN** 系统启动或收到标签查询请求
- **THEN** 系统从 `data/node-tags/` 目录加载所有标签文件并返回

---

### Requirement: 标签管理 API

系统 SHALL 在 `/api/node-tags` 路径下提供 RESTful API。

#### Scenario: GET /api/node-tags
- **WHEN** 客户端发送 GET 请求到 `/api/node-tags`
- **THEN** 系统返回所有标签列表（按优先级升序），HTTP 200

#### Scenario: POST /api/node-tags
- **WHEN** 客户端发送 POST 请求到 `/api/node-tags`，携带标签信息
- **THEN** 系统创建新标签，返回创建的标签对象，HTTP 201

#### Scenario: PUT /api/node-tags/{id}
- **WHEN** 客户端发送 PUT 请求到 `/api/node-tags/{id}`，携带更新内容
- **THEN** 系统更新对应标签，返回更新后的标签对象，HTTP 200

#### Scenario: DELETE /api/node-tags/{id}
- **WHEN** 客户端发送 DELETE 请求到 `/api/node-tags/{id}`
- **THEN** 系统删除对应标签，HTTP 204
