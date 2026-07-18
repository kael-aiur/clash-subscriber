# Retrospective: limit-build-record-history

> Written: 2026-07-18 (after verify passed)
> Commit range: `0b333bf..1c0c30c` (1 commit)
> Worktree: 主 checkout（develop 分支；手动路径，未用 worktree）

---

## 0. Evidence

> 量化前置数据 — 后续 Wins / Misses bullets 直接引用。

- **Commit range**: `0b333bf..1c0c30c` (1 commit, `1c0c30c feat(pipeline): 限制构建历史数量…`)
- **Diff size**: +808 / -1 across 13 files（4 代码改动 + 1 新测试类 + 8 OpenSpec 制品）
- **Tasks done**: 10/10（`grep -cE '^\s*- \[x\]' tasks.md` → 10）
- **Active hours**: 单次会话
- **Subagent dispatches**: n/a（手动路径；初始 Explore agent 因所选模型不可用而失败，全程改为手动实现）
- **New external dependencies**: none
- **Bugs encountered post-merge**: none（未 merge；全项目 `mvn test` BUILD SUCCESS）
- **OpenSpec validate state at archive**: pass（change `limit-build-record-history` valid=True）
- **Test coverage signal**: 新增 6 条测试（`JsonFileBuildRecordRepositoryTest` 3 + `HistoryPruningTests` 3）；全项目 `mvn test` 绿（module-pipeline 22 + module-web 58）

Commit chain（时序）:

```
1c0c30c feat(pipeline): 限制构建历史数量，每个流程保留最新 10 条
```

---

## 1. Wins

- [evidence: BuildPipelineServiceImpl:814 `pruneOldRecords`] 复用既有 `findByBuildPipelineId` 的 `startedAt` 倒序查询，清理逻辑零额外查询成本（design D7 落地）
- [evidence: JsonFileBuildRecordRepository:90–99] 删除异常捕获记 warn 不外抛，清理失败不阻断构建主流程（design Risk 落地）
- [evidence: BuildPipelineServiceImpl:276, :576] 同步 `execute` 与异步 `executeWithProgress` 两个入口都接入清理，覆盖 cron 触发与手动触发
- [evidence: `mvn test` Reactor Summary] 全项目零回归（module-web 58 + module-pipeline 22，含 `BuildRecordControllerIntegrationTest`/`BuildPipelineControllerIntegrationTest`）
- [evidence: HistoryPruningTests] 收敛（11→删最旧 1）、未超量不删、失败也触发，三类核心行为单测覆盖
- [evidence: verify 阶段] 发现 spec scenario 4「多 pipeline 独立」缺根基测试，主动补 `findByBuildPipelineId_onlyReturnsMatchingPipeline`，5 个 scenario 全覆盖

## 2. Misses

- 🟡 [painful | evidence: 全程] **superpowers 工作流不可用**：`brainstorming`/`writing-plans`/`using-git-worktrees`/`subagent-driven-development`（含 TDD、code-review subagent）/`finishing-a-development-branch` 全部改手动路径；未做 worktree 隔离、subagent TDD 与代码审查。质量由 `mvn test` + verify 三维度检查兜底。
- 🟡 [painful | evidence: tasks.md 4.2 / verify.md §7] **4.2 字面 dogfood 未做真实端到端**：`execute` 依赖真实订阅源/Mihomo，无法在测试环境纯自动化；改用等价单测覆盖核心断言。部署期仍需一次真实连续触发确认。
- 📌 [nit | evidence: verify.md §1] **既有主 specs `valid=False`** 系统性遗留（delta 格式缺 `## Purpose`/`## Requirements`），本 change 未修复，建议作为独立项目议题。

## 3. Plan deviations

| Plan task | 改变 | 原因 |
|-----------|------|------|
| 4.2 | 「手动 dogfood」→「等价自动化覆盖」 | `execute` 依赖真实外部服务无法纯自动化；其核心断言（超量收敛 / 真实文件删除 / 最新保留）已被 `HistoryPruningTests` + 真实文件 repository 测试**超集覆盖** |
| 新增（非 plan） | 补 scenario 4 隔离测试 | verify 发现「不同构建流程独立约束」缺根基测试（repository 按 pipelineId 隔离） |

## 4. Skill / workflow compliance

| Skill                                            | Used |
|--------------------------------------------------|------|
| superpowers:brainstorming                        | ✗（手动写 brainstorm.md，捕捉 openspec-explore 决议链）|
| superpowers:writing-plans                        | ✗（手动写 plan.md，按 TDD micro-steps 结构）|
| superpowers:using-git-worktrees                  | ✗（主 checkout develop 分支）|
| superpowers:subagent-driven-development          | ✗（手动按 plan 实现）|
| (transitive) superpowers:test-driven-development | △（测试与实现同批落地，非严格 RED→GREEN 分步）|
| (transitive) superpowers:requesting-code-review  | ✗（无 subagent 代码审查）|
| superpowers:finishing-a-development-branch       | ✗（未开 PR，待用户决定）|

> **异常情境说明**：全部 superpowers skills 因本环境未安装 Superpowers 插件而 ✗。
> 这属于环境配置问题，非 change 质量问题——通过 `mvn test` 全绿 + verify 三维度
> （Completeness / Correctness / Coherence）检查兜底。若后续安装插件，建议对类似
> 小型 change 仍优先走完整 superpowers 流程以获得 worktree 隔离与独立代码审查。

## 5. Follow-ups

- 部署环境对 4.2 做一次真实连续触发 dogfood（>10 次构建 → 确认收敛到 10 条）
- 独立议题：修复既有主 specs 的 `## ADDED Requirements` → `## Purpose`/`## Requirements` 标准格式（影响 `openspec validate --all`）
- （可选）若安装 Superpowers 插件，后续 change 走完整 worktree + subagent TDD + code-review 流程
