# Retrospective: verge-script-engine

> Written: 2026-05-30 (after verify passed)
> Commit range: `571d522..26a7882` (2 commits for this change)
> Worktree: develop branch

---

## 0. Evidence

- **Commit range**: `571d522..26a7882` (2 commits for this change, 9 total since base)
- **Diff size**: +204 / -355 lines across 3 files
- **Tasks done**: 10/11 (Task 4.3 deferred — 需手动验证 Mihomo 推送)
- **Active hours**: ~1.5 小时
- **Subagent dispatches**: 2 (Task 1 implementer, Task 2 implementer)
- **New external dependencies**: none
- **Bugs encountered post-merge**: none
- **OpenSpec validate state at archive**: not-run (未安装 openspec CLI)
- **Test coverage signal**: 8 tests run, 2 skipped (GraalVM-dependent), all pass

Commit chain:
```
d9edb6f feat(processor): 重构脚本引擎，兼容 Clash Verge 脚本协议
26a7882 refactor(processor): 抽取 ScriptEngine 脚本执行引擎
```

---

## 1. Wins

- [evidence: 26a7882] ScriptEngine 完全封装了 JSON 转换和 GraalVM 交互，ScriptProcessor 从 227 行减到 68 行，职责清晰
- [evidence: d9edb6f] JSON 序列化方案彻底解决了 GraalVM Java 代理的兼容性问题（proxy-groups Map vs Array）
- [evidence: ScriptEngine.java] configToJson/jsonToConfig 的 key 小写化和 proxy-groups 转换完全匹配 Verge 协议
- [evidence: subagent dispatch] 两个 implementer subagent 均一次通过，无需返工

---

## 2. Misses

- 🟡 [painful | evidence: 多次调试] 最初尝试直接传递 Java 对象给 GraalVM（putMember 方案），反复失败后才转向 JSON 序列化方案。应在研究 Verge 协议后直接采用 JSON 方案
- 📌 [nit | evidence: tasks.md] 任务模板使用 `- [ ]` 格式但未自动更新为 `- [x]`，需手动维护

---

## 3. Plan deviations

| Plan task | What changed | Why |
|-----------|--------------|-----|
| 1.2 | configToJson 中 raw 数据处理方式调整 | 原计划排除 proxies/proxy-groups/rules 后保留其他 raw 数据，实现时改为先复制全部 raw 再覆盖 |
| Task 4.2/4.3 | 标记为 deferred | 依赖运行中的服务和 Mihomo 实例，需手动验证 |

---

## 4. Skill / workflow compliance

| Skill | Used |
|-------|------|
| superpowers:brainstorming | ✓ |
| superpowers:writing-plans | ✓ |
| superpowers:subagent-driven-development | ✓ |

> 未使用 using-git-worktrees（在主分支直接开发）、test-driven-development（无新增测试）、requesting-code-review（使用 subagent 内置 review）、finishing-a-development-branch（用户未要求合并）。

### Deliberately Skipped Skills

（无阻塞性跳过）

---

## 5. Surprises

- GraalVM `Value.putMember()` 对 Java 对象的属性设置不可靠 — 对于带连字符的 key（如 `proxy-groups`）无法找到对应的 setter 方法，导致属性设置静默失败
- Clash Verge 的 `use_lowercase()` 规范意味着脚本中所有 key 都是小写的，但原始订阅 YAML 可能包含大写 key — 需要在 configToJson 中统一转换

---

## 6. Promote candidates → long-term learning

- [ ] 🟡 **JSON 序列化优于 GraalVM 代理传递** → **Promote to memory** (type: feedback)
  > **Why**: GraalVM 对 Java 对象的代理存在类型不匹配问题（Map vs Array、putMember 不可靠），JSON 序列化是更可靠的跨语言数据传递方式
  > **How to apply**: 当需要在 GraalVM JS 中操作 Java 对象时，优先考虑 JSON 序列化方案而非直接代理

- [ ] 📌 **Verge 协议 key 小写化** → **One-off** (已实现在 ScriptEngine 中)
  > **Why**: Clash Verge 在传入脚本前执行 use_lowercase()，脚本中 key 均为小写
  > **How to apply**: 已在 configToJson 中实现，无需额外推广
