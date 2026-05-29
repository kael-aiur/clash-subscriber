---
name: git-push
description: 提交代码并推送到远程仓库。当用户说"提交代码"、"git push"、"帮我提交"、"提交并推送"、"commit and push"时触发。审阅全部代码变更，按逻辑分组生成 commit，确认无冲突后推送。
---

# Git Push

审阅工作区的所有代码变更，按逻辑分组生成清晰的 commit message，分次提交使仓库变回 clean 状态，最后询问用户是否需要 push。

## 流程

### 1. 收集变更信息

同时执行以下命令，全面了解当前状态：

```bash
git status
git diff --stat
git diff --cached --stat
git log --oneline -10
```

### 2. 审阅变更内容

对每个有变更的文件，读取 diff 内容并理解变更意图：

- `git diff` 查看未暂存的变更
- `git diff --cached` 查看已暂存的变更
- 如果有未跟踪的新文件，读取其内容

将变更按**逻辑相关性**分组。分组原则：
- 同一个功能/模块的变更放在一个 commit
- 不相关的变更拆成多个 commit
- 如果变更都围绕同一个目标，一个 commit 即可
- 临时文件、构建产物、IDE 配置等不应提交，提醒用户加入 .gitignore

### 3. 分次提交

对每个分组执行：

1. `git add <具体文件>` — 只暂存该分组的文件，不要用 `git add -A` 或 `git add .`
2. 生成 commit message 并提交：
   ```bash
   git commit -m "$(cat <<'EOF'
   <type>(<scope>): <简短描述>

   <详细说明（如果需要）>

   Co-Authored-By: Claude Opus 4.7 <noreply@anthropic.com>
   EOF
   )"
   ```
3. 提交后执行 `git status` 确认该分组的文件已 clean

Commit message 规范：
- `type` 使用：`feat`, `fix`, `refactor`, `chore`, `docs`, `test`, `style`, `perf`
- `scope` 用模块名或功能区域
- 中文描述变更内容，简洁明了
- 末尾添加 Co-Authored-By 行

### 4. 确认全部 clean

所有分组提交完毕后，执行 `git status` 确认工作区完全 clean。如果有残留：
- 不该提交的文件 → 提醒用户添加 .gitignore
- 遗漏的文件 → 补充提交

### 5. 询问是否 push

告知用户所有 commit 已完成，展示 commit 列表（`git log --oneline`），然后询问是否推送到远程。

### 6. 推送（用户确认后）

```bash
git push
```

如果推送失败（远程有新提交），执行 rebase 后重试：

```bash
git pull --rebase
git push
```

如果 rebase 产生冲突：
1. 展示冲突文件列表
2. 逐个读取冲突内容，帮助用户解决
3. 解决后 `git add <文件>` 并 `git rebase --continue`
4. 冲突全部解决后 `git push`

## 注意事项

- 绝不使用 `git add .` 或 `git add -A`，始终指定具体文件
- 如果用户有未完成的工作（半成品代码），提醒后再决定是否提交
- 如果变更涉及敏感信息（密钥、密码、token），立即提醒用户
- 提交前不运行测试 — 这是提交流程，不是 CI
- push 前必须得到用户确认，不要自动推送
