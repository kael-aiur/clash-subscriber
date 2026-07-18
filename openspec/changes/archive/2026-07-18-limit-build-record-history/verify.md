# Verification Report

> 此檔案由 `openspec-verify-change` skill 在 apply 完成後產生，用以確認實作
> 與 specs / design / tasks 的一致性。失敗的檢查須返回對應 artifact 修正後
> 再重跑 verify。

**Change**: `limit-build-record-history`
**Verified at**: `2026-07-18`
**Verifier**: Claude Code（手动实现路径——本环境未安装 Superpowers 插件，`superpowers:*` skills 不可用，故未走 worktree / subagent-driven-development / TDD-subagent / code-review-subagent 流程，改为按 plan.md 手动落地 + mvn test 验证）

---

## 1. Structural Validation (`openspec validate --all --json`)

- [x] 本 change `limit-build-record-history`：`valid=True`

**结果**：

```text
limit-build-record-history: valid=True   ← 本 change
```

`openspec validate --all` 同时报告大量**既有**主 specs `valid=False`（admin-auth、
build-pipeline、build-record、…）。经核查（`openspec validate build-record`），
根因是项目所有主 specs 采用 delta 格式（`## ADDED Requirements`）而缺少标准
`## Purpose` / `## Requirements` 段——这是**系统性的历史遗留格式问题**，
git status 确认本次改动未触碰任何主 spec 文件。**非本 change 引入，不阻塞本 change
的 archive**（archive 校验的是 change 自身 valid=True）。建议作为独立项目议题跟进。

| Item | Type | Issues |
|---|---|---|
| limit-build-record-history | change | ✓ valid |
| （其他主 specs） | 既有 spec | 缺 `## Purpose`/`## Requirements`（历史遗留，与本 change 无关） |

---

## 2. Task Completion (`tasks.md`)

- [x] 所有 `- [ ]` 已变为 `- [x]`（10/10）

**未完成任務**（若有）：

| Task | 未完成原因 | 是否阻塞 archive |
|---|---|---|
| — | 全部完成 | — |

注：Task 4.2（原计划「启动应用连续触发 execute」的部署期 dogfood）已用等价自动化
测试覆盖并勾选，详见 §7。

---

## 3. Delta Spec Sync State

| Capability | Sync 状态 | 備註 |
|---|---|---|
| build-record | 待 sync（尚未 archive） | delta 位于 `changes/limit-build-record-history/specs/build-record/spec.md`；archive 时将 append 到 `specs/build-record/spec.md` |

---

## 4. Design / Specs Coherence Spot Check

design.md 的 7 项决策（D1–D7）逐项比对 specs 与实现：

| 抽樣項 | design 描述 | specs/实现 对应 | 差距 |
|---|---|---|---|
| D1 范围维度 | 每个 pipeline 各 10 条 | spec Requirement「按 buildPipelineId 维度」；实现 `pruneOldRecords(pipelineId)` | 无 |
| D2 阈值 | 硬编码 10 | `MAX_RECORDS_PER_PIPELINE = 10`（BuildPipelineServiceImpl:45） | 无 |
| D3 触发 | 每次 save 后 | execute():276、executeWithProgress():576 | 无 |
| D4 删除位置 | Repository deleteById | 接口:13、实现:90 | 无 |
| D5 RUNNING 豁免 | 不特殊处理 | pruneOldRecords 无状态分支 | 无 |
| D6 并发 | 不加锁 | 实现无锁 | 无 |
| D7 排序复用 | 复用 findByBuildPipelineId 倒序 | pruneOldRecords 直接调用 | 无 |

**漂移警告**（非阻塞）：

- 无

---

## 5. Implementation Signal

- [x] 改动范围干净：`git status` 仅 4 modified + 2 untracked（repository 测试目录、change 目录），未触碰主 spec / docs
- [ ] **未 commit**：按约定（"Commit or push only when the user asks"）等待用户确认后提交

**改动文件**：

```
M  module-pipeline/.../repository/BuildRecordRepository.java          （+deleteById 接口）
M  module-pipeline/.../repository/JsonFileBuildRecordRepository.java  （+deleteById 实现）
M  module-pipeline/.../service/impl/BuildPipelineServiceImpl.java     （+常量/+pruneOldRecords/+2 接入点）
M  module-pipeline/.../service/impl/BuildPipelineServiceImplTest.java （+HistoryPruningTests 3 条）
?? module-pipeline/.../repository/JsonFileBuildRecordRepositoryTest.java（+3 条）
?? openspec/changes/limit-build-record-history/                        （全部制品）
```

**测试结果**：全项目 `mvn test` BUILD SUCCESS（module-pipeline 22 + module-web 58，零回归）。

> 本环境未使用 worktree（手动路径），故无 "worktree 内未 staged" 检查。

---

## 6. Front-Door Routing Leak Detector（warning,非阻塞）

```bash
ls docs/superpowers/specs/*.md
```

- [x] 存在的文件均为 schema 安装前的合法存留

**洩漏清單**（若有）：

| 檔案 | 內容是否已 captured 進 change | 建議動作 |
|---|---|---|
| `docs/superpowers/specs/2026-06-02-group-proxy-nodes-by-region-design.md` | 与本 change 无关（commit f3842ea 既有） | 保留 |
| `docs/superpowers/specs/2026-06-05-add-config-type-for-build-design.md` | 与本 change 无关（既有） | 保留 |

> 两文件均早于本 change，属合法存留，非本次泄漏。本 change 的 brainstorm 已正确写入
> `openspec/changes/limit-build-record-history/brainstorm.md`。

---

## 7. Deferred Manual Dogfood vs Automated Test Equivalence

plan.md 中 Task 4.2 为「手动 dogfood」（连续触发 execute >10 次），逐项评估等价自动化覆盖：

| Deferred dogfood (plan §) | Equivalent automated test | Coverage assessment | 真正 gap? |
|---|---|---|---|
| 4.2 启动应用连续触发同一 pipeline 构建 >10 次 → `data/build-records/` 收敛到 10 条且最新保留 | (a) `HistoryPruningTests.shouldPruneOldestWhenExceedLimit`：11 条 → 删最旧 1 条、保留最新 10、verify 仅 `rec-oldest` 被删<br>(b) `JsonFileBuildRecordRepositoryTest.deleteById_existingRecord_fileRemoved`：真实文件系统 save→delete→文件消失 + findById 返回空<br>(c) `shouldNotPruneWhenWithinLimit` / `shouldPruneEvenWhenBuildFailed`：边界与失败路径 | 收敛算法（service 层）+ 真实文件 I/O（repository 层）+ 失败/边界路径均已覆盖；assertion 集合是 4.2 预期断言的超集 | ❌ 已等价覆盖 |

> 4.2 的字面动作「连续真实 execute 12 次」依赖真实订阅源 / 配置组合 / Mihomo 实例，
> 无法在无外部服务的测试环境自动化（既有集成测试 `BuildPipelineControllerIntegrationTest`
> 也仅覆盖创建，未覆盖 execute）。其核心断言（超量收敛、真实文件删除、最新保留）已被
> 上述三项测试等价覆盖。部署期仍建议做一次真实 dogfood 作为额外确认。

---

## Overall Decision

- [ ] ✅ PASS — 可进入 finishing-a-development-branch 與 archive
- [x] ⚠️ PASS WITH WARNINGS — 可进入后续步骤但需注意：
      **(1) 未 commit**——按约定待用户确认后提交；
      **(2) 既有主 specs `valid=False`**——系统性历史遗留（delta 格式缺 Purpose/Requirements），
      与本 change 无关、不阻塞 archive，建议作为独立项目议题；
      **(3) 部署期 dogfood**——4.2 已等价自动化覆盖，仍建议部署后做一次真实连续触发确认。
- [ ] ❌ FAIL

**下一步**：

1. 用户确认后提交代码（可交由 git-push skill）
2. （可选）部署环境对 4.2 做一次真实 dogfood
3. `/openspec-archive-change limit-build-record-history` —— sync delta spec 到主 specs 并归档
   （archive 不受既有主 spec 格式遗留阻塞，因校验对象是 change 自身 valid=True）
