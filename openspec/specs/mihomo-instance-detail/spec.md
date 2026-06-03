## ADDED Requirements

### Requirement: 实例详情页路由

系统 SHALL 提供 Mihomo 实例详情页面，路由为 `/mihomo-instances/:id`。

#### Scenario: 访问实例详情页
- **WHEN** 用户访问 `/mihomo-instances/:id` 路由
- **THEN** 系统展示该实例的详情页面，包含三个标签页（实例信息、转发规则、推送历史）

#### Scenario: 实例不存在
- **WHEN** 用户访问一个不存在的实例 ID
- **THEN** 系统展示错误提示并返回列表页

---

### Requirement: 实例信息标签页

系统 SHALL 在详情页的实例信息标签页中展示实例基本信息并支持编辑。

#### Scenario: 展示实例信息
- **WHEN** 用户进入实例详情页
- **THEN** 系统展示实例的名称、API URL、API Secret、启用状态、健康状态、最后健康检查时间

#### Scenario: 编辑实例信息
- **WHEN** 用户点击编辑按钮并修改信息
- **THEN** 系统保存修改并更新展示

---

### Requirement: 转发规则标签页

系统 SHALL 在详情页的转发规则标签页中提供域名转发路径查询功能。

#### Scenario: 查询域名转发路径
- **WHEN** 用户输入域名并点击查询
- **THEN** 系统从 Mihomo 获取当前配置，解析规则匹配，用流程图展示该域名的完整转发路径

#### Scenario: 流程图展示
- **WHEN** 系统返回转发路径数据
- **THEN** 前端使用 Vue Flow 渲染流程图，包含域名节点、规则节点、代理组节点、代理节点、出口节点

#### Scenario: 代理组展开/折叠
- **WHEN** 用户点击代理组节点的展开按钮
- **THEN** 系统展示该代理组内部的子代理组和具体代理节点

#### Scenario: 代理组折叠
- **WHEN** 用户点击代理组节点的折叠按钮
- **THEN** 系统隐藏该代理组的子节点，只保留代理组节点

---

### Requirement: 推送历史标签页

系统 SHALL 在详情页的推送历史标签页中展示该实例的推送记录。

#### Scenario: 展示推送历史
- **WHEN** 用户进入推送历史标签页
- **THEN** 系统展示该实例的历史推送记录列表，包括推送时间、状态、关联的构建管线

---

### Requirement: 列表页跳转

系统 SHALL 在 Mihomo 实例列表页提供跳转到详情页的链接。

#### Scenario: 点击实例名称跳转
- **WHEN** 用户在列表页点击实例名称
- **THEN** 系统跳转到该实例的详情页
