## Context

当前系统可以管理多个 Clash 订阅源，获取完整配置（proxies、proxy-groups、rules），通过构建流水线合并处理后推送到 Mihomo。但规则只能作为订阅配置的一部分整体使用，无法单独提取和复用。

现有代码结构：
- **module-processor**: 包含 `ConfigProcessor` 接口和 5 个内置处理器（DuplicateRemove、NodeMerge、RuleModify、ProxyGroup、Script），以及 `PipelineConfig` 的 JSON 持久化
- **module-subscription**: 订阅 CRUD + HTTP 获取 + YAML 解析，`SubscriptionService.fetch()` 返回 `ClashConfig`
- **module-web**: REST 控制器层，NodeTag 等业务模型也放在此处
- **持久化**: 全部基于 JSON 文件（`data/{collection}/{id}.json`），使用 Jackson ObjectMapper

规则格式为 Clash 标准：`类型,参数,代理名`（如 `DOMAIN-SUFFIX,google.com,Proxy`），MATCH 等无参数规则为 `MATCH,代理名`。

## Goals / Non-Goals

**Goals:**
- 支持从订阅中自动提取规则组（扫描规则引用的代理名，排除内置名）
- 支持手动创建规则组
- 规则组 CRUD（创建、查看、编辑、删除）
- 代理对象的管理（增删改）
- 规则的结构化展示和编辑
- 订阅列表页集成提取入口

**Non-Goals:**
- 规则组与 BuildPipeline 的集成（后续单独设计）
- 规则组的导入/导出
- 规则语法校验
- 从订阅自动同步（按需手动触发，完全覆盖）

## Decisions

### D1：模型放在 module-processor

- **选择**: RuleGroup 和 RuleProxyObject 模型放在 module-processor，Repository/Service/Controller 也放在 module-processor
- **理由**: 规则组是规则处理的一种形式，与 RuleModifyProcessor 等同属处理层。module-processor 已有 PipelineConfig 的 JSON 持久化模式，可直接复用
- **已考虑 alternatives**:
  - 放在 module-subscription：虽然和订阅关联，但规则组是独立概念，且 module-subscription 职责已明确（订阅获取解析）
  - 新建 module-rule-group：对当前项目规模过度设计
  - 跨模块分布（模型放 common，逻辑放 subscription）：增加模块间耦合

### D2：JSON 文件持久化，复用现有 Repository 模式

- **选择**: 使用 `data/rule-groups/{id}.json` 存储，实现与 NodeTag、PipelineConfig 相同的 Repository 模式
- **理由**: 项目统一使用 JSON 文件持久化，无 ORM，保持一致性
- **已考虑 alternatives**: 数据库（SQLite/H2）：引入新依赖，与项目风格不符

### D3：占位符格式 `{{px-001}}`

- **选择**: 规则中代理对象引用使用 `{{id}}` 格式，如 `DOMAIN-SUFFIX,google.com,{{px-001}}`
- **理由**: 双大括号在模板语言中常见，可读性好，与 Clash 规则中的正常代理名不会冲突（正常代理名不会包含 `{{` 和 `}}`）
- **已考虑 alternatives**:
  - `[px-001]`：方括号可能与某些代理名冲突
  - `__RG_px-001__`：可读性差

### D4：提取逻辑 — 自动扫描规则中的代理名

- **选择**: 解析每条规则的第 3 个字段（代理名），收集唯一值，过滤掉 DIRECT/REJECT/PASS 等内置名，为每个生成代理对象
- **理由**: 全自动，用户无需手动选择；排除内置名因为它们不需要映射
- **已考虑 alternatives**:
  - 只提取 proxy-groups 中定义的组名：会遗漏直接引用节点名的规则
  - 手动选择：增加用户操作负担

### D5：同步策略 — 按需完全覆盖

- **选择**: 订阅列表页提供"提取"/"重新提取"按钮，每次调用完全重新生成规则组，覆盖所有用户编辑
- **理由**: 逻辑简单，无增量合并的复杂性；用户明确知道重新提取会覆盖
- **已考虑 alternatives**:
  - 自动同步：用户手动编辑会被静默覆盖，体验差
  - 增量合并：实现复杂，规则的增删改难以正确合并

### D6：规则结构化解析 — 逗号分割

- **选择**: 用逗号分割规则字符串为 2-3 段：`[类型, 参数, 代理名]` 或 `[类型, 代理名]`（MATCH 等无参数规则）
- **理由**: Clash 规则格式标准且简单，逗号分割即可正确解析
- **已考虑 alternatives**: 正则匹配规则类型：过度设计，规则格式已固定

## Risks / Trade-offs

**[Risk]** 代理对象 ID 在手动编辑时可能产生冲突 → Mitigation: ID 使用自动生成（`IdGenerator.generate()`），编辑时不可修改 ID，只可修改 sourceName

**[Risk]** 规则中代理名包含逗号时解析错误 → Mitigation: Clash 规则标准中代理名不含逗号，无需特殊处理；如遇异常在前端提示

**[Risk]** 重新提取覆盖用户编辑导致数据丢失 → Mitigation: 前端在用户点击"重新提取"时弹出确认对话框，明确提示将覆盖所有修改

**[Trade-off]** 规则组暂不与 BuildPipeline 集成 → 接受理由：先做好管理功能，集成方案需要更多设计思考（映射配置的 UI 交互、执行时机等），分步实现更稳妥

## Migration Plan

N/A — 本 change 不涉及部署变更，纯新增功能。新增 `data/rule-groups/` 目录，无需数据库迁移。

## Open Questions

- 内置名的完整列表：除了 DIRECT、REJECT、PASS，是否还有其他 Clash 内置名需要排除？
- 手动创建规则组时，代理对象 ID 是否需要支持用户自定义（而非自动生成）？
