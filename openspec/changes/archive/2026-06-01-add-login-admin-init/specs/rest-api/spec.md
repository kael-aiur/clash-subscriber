## ADDED Requirements

### Requirement: 管理 API 认证门禁
系统 SHALL 对除 `/api/auth/**` 之外的 `/api/**` 管理接口执行管理员 Session 认证检查。

#### Scenario: 未登录访问管理 API
- **WHEN** 客户端未登录且调用除 `/api/auth/**` 之外的 `/api/**` 接口
- **THEN** 系统 MUST 拒绝请求并返回 401 未认证错误

#### Scenario: 已登录访问管理 API
- **WHEN** 客户端已通过管理员登录且调用除 `/api/auth/**` 之外的 `/api/**` 接口
- **THEN** 系统 SHALL 允许请求继续进入原有业务处理流程

#### Scenario: 认证接口不被门禁拦截
- **WHEN** 客户端调用 `/api/auth/status`、`/api/auth/setup`、`/api/auth/login` 或 `/api/auth/logout`
- **THEN** 系统 SHALL 允许请求进入认证控制器处理，不因缺少登录 Session 被认证拦截器拒绝

#### Scenario: 未初始化不能绕过管理 API 认证
- **WHEN** 系统未初始化且客户端调用除 `/api/auth/**` 之外的 `/api/**` 接口
- **THEN** 系统 MUST 拒绝请求，且不得允许客户端直接访问管理数据
