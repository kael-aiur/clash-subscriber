# 订阅源节点采纳规则 - 设计文档

## Context

### 背景

配置组合（ConfigProfile）可引用多个订阅源。`ConfigGeneratorServiceImpl.generate` 的流程为：fetch 各订阅源 → `NodeMergeProcessor` 全量 addAll 合并 → `resolveProxyGroups` 在全局节点池里筛代理组 → `toYaml` 写出全部合并节点。

问题在于 `toYaml` 遍历的是合并后的全部节点，而不同订阅源常带有同名伪节点（到期、流量信息），导致 `proxies` 重名，Mihomo 拒收。

### 当前状态

- `ConfigProfile.subscriptionIds`：仅存订阅源 ID，无任何节点筛选信息
- `ProxyGroupConfig` 已具备 `includeAll / excludeKeywords / matchKeywords / nodeNames` 字段，且 `resolveProxyGroups` 已实现关键词匹配/排除逻辑——可复用
- 前端 `ConfigProfileEditView` 的代理组配置已有成熟的「mode 单选 + 条件渲染」UI 模式可参照

### 约束

- 复用现有 `ConfigGeneratorServiceImpl` 流程，仅在 fetch 与 merge 之间插入过滤
- 全部产出物使用简体中文
- 不引入新外部依赖

### 利益相关者

- 主要用户：组合多订阅源、并遭遇重名冲突的配置维护者

## Goals / Non-Goals

**Goals**
- 用户可为配置组合中每个订阅源单独配置节点采纳规则
- 默认规则（全部节点、无排除词）保持与现状完全一致的行为
- 两种模式：全部节点 + 排除关键词、关键词匹配（可叠加排除关键词）
- 老配置数据零改动自动兼容

**Non-Goals**
- 不做「手动选择」模式（需订阅源节点预览接口，本期不做）
- 不做跨订阅源去重（同名好节点冲突不在本期范围）
- 不做单订阅源内部去重
- 不改动 `toYaml` 的写出范围（过滤后 merge 的就是干净节点，天然只写命中节点）

## Decisions

### D1：采纳规则挂在 ConfigProfile（按订阅源维度），而非 Subscription 全局

- **选择**：规则作为 ConfigProfile 的属性，按订阅源维度存储
- **理由**：同一订阅源在不同配置组合中常需要不同取舍；与「一个配置组合多个订阅源」的语境一致
- **已考虑 alternative**：
  - 挂在 Subscription 全局：一次配置全局生效，但无法满足同一订阅源在不同配置中差异化采纳的需求

### D2：过滤时机为 fetch 之后、merge 之前

- **选择**：在 `ConfigGeneratorServiceImpl` 取回每个订阅源的 `ClashConfig` 后，立即按其 `nodePolicy` 过滤 `getProxies()`，再交给 `NodeMergeProcessor`
- **理由**：下游（merge、resolveProxyGroups、toYaml）全部无需改动；过滤后的 mergedConfig.proxies 天然只含命中节点，与「最终节点表只有命中规则节点」的用户心智一致
- **已考虑 alternative**：
  - 在 `NodeMergeProcessor` 内部过滤：会让通用合并器耦合配置语义，职责不清
  - 在 `toYaml` 阶段过滤：已无法区分节点来自哪个订阅源，丢失了按源配置规则的信息

### D3：本期不引入跨订阅源去重

- **选择**：仅靠采纳规则过滤，不增加按名去重兜底
- **理由**：用户的实际痛点是伪节点（到期/流量）冲突，采纳规则的「排除关键词」已可覆盖；去重会引入「保留哪一个」的额外语义
- **边界**：两个订阅源中同名的好节点（如各自的「香港 01」）、单订阅源内部重名，本期仍可能冲突。spec 与 design 中明确标注此边界
- **已考虑 alternative**：
  - 合并后按名去重保留首个：彻底杜绝重名，但改变了「同源内部也去重」的语义，且与本期目标（按需采纳）正交，留待后续按需引入

### D4：本期不做「手动选择」模式

- **选择**：mode 仅支持 `all` 与 `keyword`
- **理由**：订阅源层的「手动选择」需先拉取该订阅源的节点列表做勾选 UI，否则只能退化为自由输入、面对几百节点不实用。预览接口工作量较大，与核心目标正交
- **已考虑 alternative**：
  - 复用代理组的 allow-create 自由输入：实现快，但实用性差，且易诱导用户填错节点名

### D5：抽出共用的 NodeFilter

- **选择**：将 `resolveProxyGroups` 中已有的「matchKeywords / excludeKeywords」匹配逻辑抽成共用工具，代理组筛选与订阅源采纳复用同一份实现
- **理由**：避免关键词匹配逻辑复制两份导致行为漂移
- **已考虑 alternative**：
  - 在采纳过滤处复制一份逻辑：短期快，长期维护风险

### D6：老数据读取兼容

- **选择**：`ConfigProfile` 保留对旧字段 `subscriptionIds` 的读取兼容；读取时若存在 `subscriptionIds` 而 `subscriptionRefs` 为空，则映射为「全部节点、排除词为空」的默认规则
- **理由**：升级零摩擦，老配置行为不变
- **已考虑 alternative**：
  - 写一次性数据迁移脚本：过度设计，读取兼容已足够

### D7：getEffectiveSubscriptionRefs 标注 @JsonIgnore

- **选择**：给 `ConfigProfile.getEffectiveSubscriptionRefs()` 加 `@JsonIgnore`
- **理由**：该方法符合 JavaBean getter 命名，默认会被 Jackson 序列化为 `effectiveSubscriptionRefs` 字段写入磁盘；反序列化时该字段会让 `subscriptionRefs` 翻倍，导致每个订阅源被 fetch 两次、节点重复。该 bug 由端到端测试（真实 Jackson 往返）发现，单元测试用 stub 未覆盖。加 `@JsonIgnore` 阻止序列化，并补一个序列化往返回归测试
- **已考虑 alternative**：
  - 改名为非 getter 形式（如 `computeEffectiveRefs()`）：破坏「getEffective」的语义可读性

## Risks / Trade-offs

### 风险

- **[风险] 同名好节点仍冲突** → 本期不解决。用户需通过为两个源配置不同关键词规避，或等待后续去重能力。spec 与 design 已明确标注
- **[风险] 关键词误排除正常节点** → 缓解：前端排除关键词输入框给出示例（到期、剩余、流量、余额），且默认为空（不排除任何节点）
- **[风险] 排除关键词与代理组的排除关键词语义混淆** → 缓解：两者作用层不同（订阅源层先过滤全局节点池，代理组层再从池中选），UI 上分区呈现并给出说明文案

### 权衡

- **[权衡] 治标 vs 治本** → 选择先做治标的采纳规则（覆盖实际痛点），把治本的去重留作后续可控引入
- **[权衡] 模式完整度 vs 交付节奏** → 选择本期只做两种高价值模式，避免被预览接口拖慢

## Migration Plan

### 部署步骤

1. **模型与过滤**
   - 新增 `SubscriptionRef` / `NodePolicy` 模型，`ConfigProfile` 增 `subscriptionRefs` 并兼容读取 `subscriptionIds`
   - 抽出共用 `NodeFilter`
2. **生成流程**
   - `ConfigGeneratorServiceImpl` 改为按 `subscriptionRefs` fetch 并过滤
3. **前端**
   - `ConfigProfileEditView` 订阅源卡片扩展采纳规则 UI；`api/config-profile.ts` 类型同步
4. **测试**
   - 过滤逻辑单元测试（含默认规则、排除、匹配、排除+匹配叠加）
   - 老数据兼容读取测试
   - 前端编辑→保存→生成端到端验证

### 回滚策略

- 保留上一版本 jar；读取兼容保证即使前端未升级，老配置仍可正常生成

### 验收条件

- 新建配置时可为每个订阅源配置采纳规则并保存
- 默认规则生成的配置与升级前完全一致
- 配置「排除到期/流量关键词」后，生成配置的 proxies 不再包含相应伪节点，Mihomo 推送成功
- 老 ConfigProfile 数据无需改动即可正常加载与生成

## Open Questions

- 暂无（关键决策已在探索阶段与用户确认）
