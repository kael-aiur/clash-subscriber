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
- **THEN** 展示所有实例的列表，包括名称、URL、健康状态、最后检查时间，实例名称可点击跳转到详情页

#### Scenario: 一键推送配置
- **WHEN** 用户点击推送按钮
- **THEN** 选择 Pipeline 和目标实例，执行推送并展示结果

---

### Requirement: Mihomo 实例详情页

系统 SHALL 提供 Mihomo 实例的详情管理页面。

#### Scenario: 详情页访问
- **WHEN** 用户访问 `/mihomo-instances/:id` 路由
- **THEN** 系统展示该实例的详情页面，包含实例信息、转发规则、推送历史三个标签页

#### Scenario: 从列表页跳转
- **WHEN** 用户在实例列表页点击实例名称
- **THEN** 系统跳转到该实例的详情页

---

### Requirement: 定时任务管理页面

系统 SHALL 提供定时任务的 Web 管理界面。

#### Scenario: 任务列表展示
- **WHEN** 用户访问任务管理页面
- **THEN** 展示所有任务的列表，包括名称、关联 Pipeline、目标实例、cron 表达式、执行状态

#### Scenario: 任务编辑
- **WHEN** 用户点击编辑按钮
- **THEN** 展示编辑表单，支持修改任务配置

---

### Requirement: Hash 路由模式

系统 SHALL 使用 Vue Router 的 hash 模式（`createWebHashHistory()`）进行前端路由。

#### Scenario: URL 格式
- **WHEN** 用户访问前端页面
- **THEN** URL 格式为 `/#/path`（如 `/#/subscriptions`）

#### Scenario: 页面刷新
- **WHEN** 用户在任意页面刷新浏览器
- **THEN** 页面正常加载，无需服务器配置重定向

---

### Requirement: 统一认证页面
系统 SHALL 提供统一认证页面，根据认证状态显示首次初始化表单或登录表单。

#### Scenario: 首次打开显示初始化表单
- **WHEN** 用户打开前端且认证状态表示系统未初始化
- **THEN** 前端 SHALL 导航到认证页面并显示管理员初始化表单

#### Scenario: 已初始化未登录显示登录表单
- **WHEN** 用户打开前端且认证状态表示系统已初始化但当前会话未认证
- **THEN** 前端 SHALL 导航到认证页面并显示登录表单

#### Scenario: 已登录进入后台
- **WHEN** 用户打开前端且认证状态表示当前会话已认证
- **THEN** 前端 SHALL 允许用户进入原有管理后台页面

---

### Requirement: 初始化表单交互
系统 SHALL 在认证页面提供管理员初始化表单，并在初始化成功后切换到登录表单。

#### Scenario: 初始化表单字段
- **WHEN** 认证页面处于初始化模式
- **THEN** 前端 SHALL 展示用户名、密码和确认密码输入项

#### Scenario: 初始化成功后要求登录
- **WHEN** 用户成功创建管理员账号
- **THEN** 前端 SHALL 显示成功提示并切换到登录表单，且不得把用户视为已登录

#### Scenario: 初始化表单校验
- **WHEN** 用户提交少于 8 位密码或不匹配的确认密码
- **THEN** 前端 SHALL 阻止提交或展示明确的表单错误

---

### Requirement: 登录表单交互
系统 SHALL 在认证页面提供管理员登录表单，并在登录成功后进入目标后台页面。

#### Scenario: 登录表单字段
- **WHEN** 认证页面处于登录模式
- **THEN** 前端 SHALL 展示用户名和密码输入项

#### Scenario: 登录成功跳转
- **WHEN** 用户登录成功
- **THEN** 前端 SHALL 跳转到用户原本尝试访问的后台路径，若没有目标路径则跳转到订阅源管理页面

#### Scenario: 登录失败提示
- **WHEN** 用户登录失败
- **THEN** 前端 SHALL 在认证页面展示错误提示，且不得进入后台页面

---

### Requirement: 后台退出入口
系统 SHALL 在管理后台展示当前管理员信息和退出登录入口。

#### Scenario: 显示当前管理员
- **WHEN** 用户已登录并进入管理后台
- **THEN** 前端 SHALL 在后台 header 中展示当前管理员用户名

#### Scenario: 退出登录
- **WHEN** 用户点击退出登录
- **THEN** 前端 SHALL 调用登出接口并导航回认证页面

---

### Requirement: 前端未认证响应处理
系统 SHALL 对管理 API 的 401 响应执行统一未登录处理，但认证接口自身的登录失败 SHALL 保留在表单内展示。

#### Scenario: 管理 API 返回 401
- **WHEN** 前端调用非认证管理 API 收到 401 响应
- **THEN** 前端 SHALL 导航到认证页面

#### Scenario: 登录接口返回 401
- **WHEN** 前端调用登录接口收到 401 响应
- **THEN** 前端 SHALL 在登录表单展示错误提示，且不得触发重复跳转循环
