## Why

当前管理后台没有认证机制，任何能访问服务地址的用户都可以打开页面并调用管理 API。该项目通常以自托管方式部署，订阅链接、Mihomo 实例地址、脚本和定时任务都属于敏感管理数据，因此需要在不改变现有部署方式的前提下增加登录保护。首次打开时自动引导创建管理员，可以让新部署实例安全地完成初始化，并避免要求用户预先手写配置文件。

## What Changes

**后台访问保护**
- From: 浏览器打开应用后可直接访问管理页面并调用 `/api/**` 管理接口。
- To: 除认证接口外，管理 API 需要已登录的服务端 Session；未登录访问返回 401。
- Reason: 防止未授权用户查看或修改订阅、Mihomo 实例、构建流程、脚本和定时任务等管理数据。
- Impact: Breaking；部署后用户首次需要初始化管理员并登录后才能使用后台。

**首次管理员初始化**
- From: 系统没有管理员账号和初始化流程。
- To: 当 `data/admin/admin.json` 不存在时，前端统一认证页显示初始化表单，创建单管理员账号；初始化成功后切回登录表单，不自动登录。
- Reason: 支持新部署实例通过 Web UI 完成安全初始化。
- Impact: Non-breaking for clean deployments；已有数据目录会在首次升级后进入初始化流程，直到管理员创建完成。

**会话登录与登出**
- From: 前端没有登录态，也没有退出入口。
- To: 登录成功后后端写入浏览器会话级 Session；后台 header 显示当前管理员并提供退出登录；关闭浏览器会话后需要重新登录。
- Reason: 提供明确的管理后台身份边界和退出机制。
- Impact: 新增认证交互，不改变登录后的业务功能。

**密码安全存储**
- From: 无管理员密码存储。
- To: 管理员密码只保存带随机 salt 的 PBKDF2 哈希，密码最小长度为 8 位。
- Reason: 避免明文保存密码，同时不引入额外数据库或安全框架依赖。
- Impact: 新增 `data/admin/admin.json` 持久化文件。

## Capabilities

### New Capabilities
- `admin-auth`: 管理员初始化、登录、登出、认证状态查询、Session 门禁和密码安全存储。

### Modified Capabilities
- `rest-api`: 现有管理 API 需要通过管理员 Session 认证后才能访问，认证接口除外。
- `web-ui`: 现有管理页面需要通过统一认证页完成初始化或登录后才能访问，并新增退出登录入口。

## Impact

- 后端：影响 `module-web`，新增认证 controller、service、model、repository、interceptor 和密码哈希工具；更新 Web MVC 配置以注册认证拦截器。
- 前端：影响 `module-web/frontend`，新增认证 API、`/auth` 页面、路由守卫、Axios 401 处理、后台 header 用户信息和登出按钮。
- 数据：新增 `data/admin/admin.json`，保存单管理员账号和密码哈希信息。
- API：新增 `/api/auth/status`、`/api/auth/setup`、`/api/auth/login`、`/api/auth/logout`；除 `/api/auth/**` 外的 `/api/**` 管理接口新增 Session 认证要求。
- 依赖：不新增数据库、Spring Security 或前端状态管理库。
- 测试：需要新增后端认证测试，并运行前端构建与手动浏览器验证。
