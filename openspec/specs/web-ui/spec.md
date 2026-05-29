## ADDED Requirements

### Requirement: 前端构建集成

系统 SHALL 将 Vue 3 前端构建产物作为 Spring Boot 静态资源部署。

#### Scenario: 前端静态资源部署
- **WHEN** Spring Boot 应用启动
- **THEN** 系统从 classpath:static/ 目录加载 Vue 3 构建产物，提供前端页面访问

---

### Requirement: 订阅源管理页面

系统 SHALL 提供订阅源的 Web 管理界面。

#### Scenario: 订阅源列表展示
- **WHEN** 用户访问订阅源管理页面
- **THEN** 展示所有订阅源的列表，包括名称、URL、状态、最后获取时间

#### Scenario: 订阅源编辑
- **WHEN** 用户点击编辑按钮
- **THEN** 展示编辑表单，支持修改名称、URL、User-Agent、自定义请求头

---

### Requirement: Mihomo 实例管理页面

系统 SHALL 提供 Mihomo 实例的 Web 管理界面。

#### Scenario: 实例列表展示
- **WHEN** 用户访问实例管理页面
- **THEN** 展示所有实例的列表，包括名称、URL、健康状态、最后检查时间

#### Scenario: 一键推送配置
- **WHEN** 用户点击推送按钮
- **THEN** 选择 Pipeline 和目标实例，执行推送并展示结果

---

### Requirement: 定时任务管理页面

系统 SHALL 提供定时任务的 Web 管理界面。

#### Scenario: 任务列表展示
- **WHEN** 用户访问任务管理页面
- **THEN** 展示所有任务的列表，包括名称、关联 Pipeline、目标实例、cron 表达式、执行状态

#### Scenario: 任务编辑
- **WHEN** 用户点击编辑按钮
- **THEN** 展示编辑表单，支持修改任务配置
