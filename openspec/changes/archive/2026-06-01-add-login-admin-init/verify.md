# Verification Report: add-login-admin-init

### Summary

| Dimension | Status |
|-----------|--------|
| Completeness | 29/29 tasks complete；3 个 delta spec 均有实现证据 |
| Correctness | 13/13 requirements covered；全部场景有实现或测试/手动验证证据 |
| Coherence | 符合 design.md 的轻量 Session + JSON 单管理员方案 |

## Completeness

### Task Completion

`openspec instructions apply --change add-login-admin-init --json` 显示：

- Total tasks: 29
- Complete tasks: 29
- Remaining tasks: 0

`openspec/changes/add-login-admin-init/tasks.md` 中所有 checkbox 均已勾选。

### Spec Coverage

- `admin-auth`：由 `module-web/src/main/java/site/kael/clash/web/auth/**`、`AdminAuthIntegrationTest` 覆盖。
- `rest-api`：由 `AuthInterceptor`、`WebConfig` 和 `AdminAuthIntegrationTest` 覆盖。
- `web-ui`：由 `AuthView.vue`、`router/index.ts`、`api/index.ts`、`App.vue` 和浏览器手动验证覆盖。

## Correctness

### admin-auth

- 管理员初始化状态：`AdminAuthService.status` 通过 `AdminAccountRepository.find()` 判断初始化状态，并结合 Session 返回认证状态；`AdminAuthIntegrationTest` 覆盖未初始化、已初始化未登录、已登录状态。
- 单管理员初始化：`AdminAuthService.setup` 校验请求体、用户名、密码长度和确认密码；重复初始化返回 409；setup 不写入 Session；测试覆盖初始化成功、短密码、重复初始化和初始化后未登录。
- 管理员登录：`AdminAuthService.login` 校验请求体、用户名和密码；未初始化返回 409；错误用户名或密码统一 401；成功后调用 `changeSessionId()` 并写入认证标记；测试覆盖登录成功、错误密码、未初始化登录。
- 管理员登出：`AdminAuthService.logout` 在存在 Session 时使当前 Session 失效；测试覆盖登出后状态变为未认证。
- 密码安全存储：`PasswordHasher` 使用 `PBKDF2WithHmacSHA256`、随机 salt、迭代次数和 key length 参数，使用 `MessageDigest.isEqual` 比较哈希；管理员模型只保存 `PasswordHash`。
- 管理员文件异常保护：`JsonFileAdminAccountRepository.find()` 在文件存在但无法解析时抛出 `AdminAccountReadException`；测试覆盖损坏文件 status 返回 500，setup 不覆盖损坏文件。

### rest-api

- 管理 API 认证门禁：`WebConfig` 注册 `AuthInterceptor` 到 `/api/**` 并排除 `/api/auth/**`。
- 未登录访问管理 API：`AuthInterceptor` 使用 `request.getSession(false)`，未认证返回 401 JSON；测试覆盖未登录访问 `/api/subscriptions` 返回 401。
- 已登录访问管理 API：Session 中存在认证标记且管理员文件仍可读时放行；测试覆盖登录后访问 `/api/subscriptions` 返回 200。
- 认证接口放行：`/api/auth/**` 在拦截器注册和拦截器逻辑中均放行；认证测试覆盖 status/setup/login/logout 无需预登录即可进入认证控制器。
- 未初始化不能绕过：拦截器放行前会调用 `adminAccountRepository.find().isPresent()`；测试覆盖管理员文件删除后旧 Session 访问 `/api/subscriptions` 返回 401。

### web-ui

- 统一认证页面：`AuthView.vue` 在挂载时请求认证状态，根据 `initialized` 切换初始化/登录模式。
- 初始化表单：包含用户名、密码、确认密码；前端校验用户名非空、密码至少 8 位、确认密码匹配；初始化成功提示并切换登录模式，不设置已登录态。
- 登录表单：登录成功后更新 `authSession` 并跳转安全 redirect 或 `/subscriptions`；登录失败留在表单并显示错误。
- 后台退出入口：`App.vue` 后台 header 显示 `authSession.username` 和“退出登录”按钮；退出无论服务端请求是否成功都会清理前端状态并导航 `/auth`。
- 前端 401 处理：`api/index.ts` 通过 `setUnauthorizedHandler` 避免 API/router 循环依赖；非认证接口 401 触发跳转；认证接口自身 401 留在表单。
- 路由守卫异常处理：`router/index.ts` 捕获 `authApi.status()` 异常，清理本地状态并回到 `/auth`，避免导航 rejected。
- 浏览器手动验证：在 `http://localhost:31193` 使用干净 `data.path` 验证首次初始化、初始化后登录、登录进入后台、刷新保持会话、退出后回到登录页、退出后 `/api/subscriptions` 返回 401。

## Coherence

### Design Adherence

实现遵循 `design.md` 的关键决策：

- 使用统一 `/auth` 页面，而不是独立 setup 页面或后台弹窗。
- 使用服务端 Session + 浏览器会话 Cookie，不引入 JWT 或“记住我”。
- 仅支持单管理员账号，保存到 `data/admin/admin.json`。
- 使用 JDK PBKDF2 保存密码哈希，不引入 Spring Security 或数据库。
- 使用 Spring MVC `HandlerInterceptor` 保护管理 API。
- 初始化成功后不自动登录。

### Pattern Consistency

- 后端 JSON repository 风格与现有 `JsonFileSubscriptionRepository`、`JsonFileMihomoInstanceRepository` 一致。
- 后端控制器沿用 Spring MVC `@RestController` + `ResponseEntity` 风格。
- 前端沿用 Vue 3 `<script setup>`、Vue Router、Axios 和 Element Plus。
- `.superpowers/` 已加入 `.gitignore`，避免提交 brainstorming 临时产物。

## Validation Performed

- `mvn -pl module-web -Dtest=AdminAuthIntegrationTest test`：通过，11 tests，0 failures，0 errors。
- `mvn test`：通过，包含 `AdminAuthIntegrationTest` 11 tests；全量 BUILD SUCCESS。
- `npm run build --prefix module-web/frontend`：通过；仅有第三方 `@vueuse/core` pure annotation 和 chunk size warning。
- `mvn -pl module-web -am package -DskipTests`：通过，用于生成手动验证 jar。
- 手动浏览器验证：通过，覆盖首次初始化、初始化后登录、刷新保持会话、退出登录、退出后 API 401。
- 最终代码审查：通过，无阻塞问题。

## Issues

### CRITICAL

无。

### WARNING

无。

### SUGGESTION

无。

## Final Assessment

All checks passed. Ready for archive.
