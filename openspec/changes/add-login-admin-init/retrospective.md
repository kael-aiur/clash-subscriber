# Retrospective: add-login-admin-init

> Written: 2026-05-31 (after verify passed)
> Commit range: `e8982bb86def3f0771b0e45c76097cdd7ad5b8c3..e8982bb86def3f0771b0e45c76097cdd7ad5b8c3`
> Worktree: `/Users/kael/workspace/github/kael-aiur/clash-subscriber/.claude/worktrees/add-login-admin-init`

---

## 0. Evidence

- **Commit range**: `e8982bb86def3f0771b0e45c76097cdd7ad5b8c3..e8982bb86def3f0771b0e45c76097cdd7ad5b8c3` (0 commits；实现仍在 working tree 中)
- **Diff size**: tracked working diff `+135 / -102` across 20 files；另有 39 个 untracked 文件，共 3087 行
- **Tasks done**: 29/29 (`grep -cE '^\s*- \[x\]' tasks.md` → 29；总 checkbox → 29)
- **Active hours**: n/a（无实现提交时间戳，无法仅由 `git log` 重建）
- **Subagent dispatches**: 0（当前可见证据中无 subagent 产出或调度记录）
- **New external dependencies**: none（无 `pom.xml`、`package.json`、lockfile 变更证据）
- **Bugs encountered post-merge**: none（尚未 merge）
- **OpenSpec validate state at archive**: pass（`openspec validate add-login-admin-init --strict` → `Change 'add-login-admin-init' is valid`）
- **Test coverage signal**: `mvn test` 通过；`mvn -pl module-web -Dtest=AdminAuthIntegrationTest test` 通过，11 tests；`npm run build --prefix module-web/frontend` 通过；浏览器手动验证通过

Commit chain（时序）:

```text
e8982bb docs(readme): 移除敏感截图引用 (base=head；本 change 尚无实现提交)
```

主要文件证据：

- 后端认证：`module-web/src/main/java/site/kael/clash/web/auth/**`
- 管理 API 门禁：`module-web/src/main/java/site/kael/clash/web/config/WebConfig.java`
- 后端测试：`module-web/src/test/java/site/kael/clash/web/auth/AdminAuthIntegrationTest.java`
- 前端认证：`module-web/frontend/src/api/auth.ts`、`module-web/frontend/src/auth/session.ts`、`module-web/frontend/src/views/AuthView.vue`
- 前端接入：`module-web/frontend/src/router/index.ts`、`module-web/frontend/src/api/index.ts`、`module-web/frontend/src/App.vue`、`module-web/frontend/src/style.css`
- 静态产物：`module-web/src/main/resources/static/**`
- 验证记录：`openspec/changes/add-login-admin-init/verify.md`

---

## 1. Wins

- [evidence: §0 + `verify.md` Validation Performed] 自动化验证和手动浏览器验证覆盖了后端、前端构建和真实登录流程，降低了只靠类型检查误判 UI 可用性的风险。
- [evidence: `brainstorm.md` Q1-Q8 + `verify.md` Design Adherence] 最终实现保持了早期决策链：统一 `/auth`、Session + Cookie、单管理员、JSON 文件、PBKDF2、初始化后不自动登录、Interceptor 门禁。
- [evidence: `tasks.md` 29/29 + `verify.md` Spec Coverage] 三个能力面 `admin-auth`、`rest-api`、`web-ui` 都有对应实现和验证证据，任务闭环完整。
- [evidence: `AdminAuthIntegrationTest` + `verify.md` Correctness/admin-auth] 损坏管理员文件不会被当作未初始化处理，避免“重新初始化覆盖管理员”的安全边界回退。

## 2. Misses

- 🔴 [blocking | evidence: §0] 无阻塞问题；`verify.md` Final Assessment 为 ready，且预检确认未勾选 FAIL。
- 🟡 [painful | evidence: §0 commit range] 实现仍是未提交 working tree，导致 retrospective 无法用 commit hash 串联 plan-vs-actual，只能引用文件路径、任务和验证记录。
- 🟡 [painful | evidence: §0 Subagent dispatches] `plan.md` 要求使用 `superpowers:subagent-driven-development` 或 `superpowers:executing-plans`，但当前可见证据没有 subagent 调度记录。
- 📌 [nit | evidence: §0 Diff size + `module-web/src/main/resources/static/**`] 前端构建更新了 hash 命名静态资源，diff 中包含旧资源删除和新资源新增，审查时需要把源码变更与构建产物分开看。

## 3. Plan deviations

| Plan task | What changed | Why |
|-----------|--------------|-----|
| Task 1-7 Commit steps | 计划中的多个分阶段 commit 没有出现；当前 commit range 为 0 commits | 当前工作保留在 worktree working tree 中，尚未进入提交/发布阶段 |
| Task 3 / Task 4 | `AuthInterceptor` 不只检查 Session，还在放行前确认管理员文件仍存在且可读 | `verify.md` 记录了管理员文件删除后旧 Session 访问 `/api/subscriptions` 返回 401，强化了未初始化/损坏状态下的安全边界 |
| Task 6 | Axios 401 处理通过 `setUnauthorizedHandler` 解耦 API 与 router，而不是直接在 `api/index.ts` 引入 router | `verify.md` 指出这样避免 API/router 循环依赖，属于实现层面的结构优化 |
| Task 7 | 手动验证端口为 `http://localhost:31193`，计划中预期为 `31192` | `verify.md` 以实际运行环境为准记录验证，端口差异不影响功能结论 |

## 4. Skill / workflow compliance

| Skill                                            | Used |
|--------------------------------------------------|------|
| superpowers:brainstorming                        | ✓（`brainstorm.md`） |
| superpowers:writing-plans                        | ✓（`plan.md`） |
| superpowers:using-git-worktrees                  | ✓（当前 worktree 路径和分支） |
| superpowers:subagent-driven-development          | ✗ |
| (transitive) superpowers:test-driven-development | ✗ |
| (transitive) superpowers:requesting-code-review  | ✓（`verify.md` 记录最终代码审查通过） |
| superpowers:finishing-a-development-branch       | ✗ |

> **Default expectation**: 全部 ✓。每个 skill 都是 schema 设计的一部分，跳过属于异常情境。任一项 ✗ 都必须在下方 `### Deliberately Skipped Skills` subsection 提出原因与预防方案。

### Deliberately Skipped Skills

- **`superpowers:subagent-driven-development`**
  - **What was skipped**: 跳过了按 plan task-by-task 分派独立 subagent 的执行方式。
  - **Why this cycle**: §0 显示 `Subagent dispatches: 0`，且 commit range 为 0 commits；当前可见产物只有单一 worktree working diff 和 `verify.md`，没有任何 subagent 输出或分支记录可引用。
  - **How to prevent recurrence**: `scope-judgment rule` — 对含 7 个实现 task、跨 Java 后端 + Vue 前端 + 浏览器验证的 change，执行阶段必须至少把“后端认证测试/前端接入/手动验证”拆成可审阅的独立执行单元；若不分派 subagent，必须在执行记录中明确选择 `superpowers:executing-plans` 并说明原因。

- **`(transitive) superpowers:test-driven-development`**
  - **What was skipped**: 跳过了先写失败测试、确认失败、再实现的红绿重构节奏；现有证据只能证明最终有 `AdminAuthIntegrationTest` 和全量测试通过。
  - **Why this cycle**: `plan.md` 将后端测试安排在 Task 4，位于后端模型、服务和门禁实现之后；`verify.md` 也只记录最终测试通过，没有失败测试先行的证据。
  - **How to prevent recurrence**: `schema graph fix` — 对包含安全边界的后端行为，应把关键 acceptance tests 提前到实现 task 前置依赖，至少先覆盖未初始化、重复初始化、错误登录、未登录访问 401、损坏管理员文件这些场景。

- **`superpowers:finishing-a-development-branch`**
  - **What was skipped**: 跳过了开发分支收尾中的提交整理、最终差异检查和发布前分支完成流程。
  - **Why this cycle**: §0 的 commit range 为 `base=head`，实现仍在 working tree；retrospective 是在 archive 前创建的 artifact，不是已提交分支的最终收尾产物。
  - **How to prevent recurrence**: `schema graph fix` — 如果 retrospective 需要评价 `finishing-a-development-branch`，schema 应将 retrospective 放在 finish skill 之后；如果 retrospective 必须在 archive 前生成，则 §4 不应把 finish skill 作为必需已用项。

> **与 §6 Promote candidates 的关系**: 多个 cycle 同 skill 同 `How to prevent` 答案 → 该模式应 promote 到 §6，直接触发 schema / skill PR，不可累积成“常态”。

## 5. Surprises

- [evidence: §0 commit range] OpenSpec retrospective 指令假定可以从 commit range 重建实现过程，但本 change 的实际实现尚未提交，导致证据主要来自 working tree、tasks 和 verify，而不是 commit chain。
- [evidence: `verify.md` Correctness/rest-api] 单靠 Session 认证标记不足以表达安全边界；管理员文件被删除或不可读时，旧 Session 也必须失效。
- [evidence: `verify.md` Correctness/web-ui] Axios 统一 401 处理容易和路由守卫形成循环依赖，最终需要用 handler 注入方式解耦。

## 6. Promote candidates → long-term learning

- [ ] 🟡 **Retrospective 不应假设实现已经提交** → **Promote to schema**
  > **Why**: 本 cycle 的 `git log <base>..HEAD` 为 0 commits，但实现和验证已经在 working tree 完成；强制 commit-chain 视角会弱化真实证据。
  > **How to apply**: 当 OpenSpec apply 允许未提交工作流时，retrospective schema 应支持“committed diff”和“working tree diff”两种 evidence mode。

- [ ] 🟡 **安全边界测试应先于实现骨架出现** → **Promote to schema**
  > **Why**: 管理员初始化、损坏文件、旧 Session 失效这类场景是安全行为，不适合只在实现后补充最终通过测试。
  > **How to apply**: 对认证、授权、凭证存储、数据损坏恢复等 change，tasks schema 应要求先列出并执行红灯 acceptance tests。

- [ ] 📌 **前端静态产物 diff 需要单独审查路径** → **Promote to CLAUDE.md**
  > **Why**: Vite hash 产物造成大量删除/新增，容易掩盖真正的源码逻辑变更。
  > **How to apply**: 审查包含 `module-web/src/main/resources/static/assets/**` 的 diff 时，先看 `module-web/frontend/src/**` 源码和构建命令结果，再把静态资源视为构建输出核对。
