# 订阅源节点采纳规则 - 验证报告

## 验证范围

对照 specs / design / tasks，从 Completeness、Correctness、Coherence 三个维度验证实现，并执行真实环境端到端验证。

## Completeness

- tasks：20/20 完成
- requirements：2 个全部实现
  - `config-profile-management` 新增「订阅源节点采纳规则」
  - `config-generation` 修改「合并订阅源节点」

## Correctness（逐 scenario）

**订阅源节点采纳规则（4 scenario）**
| Scenario | 实现 | 测试 |
|---|---|---|
| 全部节点+排除关键词 | `NodeFilter.java:34-39` | `isAccepted_allMode_excludesByKeyword` ✅ |
| 关键词匹配（可叠加排除） | `NodeFilter.java:34-39` | `isAccepted_keywordMode_matchesAndExcludes` ✅ |
| 默认采纳规则 | `ConfigProfile.getEffectiveSubscriptionRefs` | `generate_oldSubscriptionIdsCompat` ✅ |
| 关键词匹配为空回退 | `NodeFilter.java:34` | `isAccepted_keywordMode_emptyMatchKeywords_fallsBackToAll` ✅ |

**合并订阅源节点（3 scenario）**
| Scenario | 实现 | 测试 |
|---|---|---|
| 按采纳规则过滤后合并 | `ConfigGeneratorServiceImpl.java:95` | `generate_excludeKeywords` + 端到端 ✅ |
| 默认采纳规则下的合并 | `getEffectiveSubscriptionRefs` | `generate_defaultPolicy` + 端到端 ✅ |
| 订阅源获取失败 | `ConfigGeneratorServiceImpl.java:101` catch 跳过 | 仅实现，无专门测试 ⚠️ |

## Coherence

design D1–D7 决策全部遵循：规则挂 ConfigProfile（D1）、fetch 后 merge 前过滤（D2）、不加去重（D3）、不做手动选择（D4）、共用 NodeFilter（D5，`resolveProxyGroups` 复用）、老数据兼容（D6）、`@JsonIgnore`（D7）。代码模式与项目一致。

## 端到端验证

mock 两个订阅源，各含同名伪节点「套餐到期：长期有效」：
- 不配排除词：生成配置出现重名 → 复现用户的 HTTP 400 `duplicate name` 场景
- 配排除词「到期」：两个伪节点被干净过滤，proxies 仅剩正常节点

过程中发现并修复 `getEffectiveSubscriptionRefs` 被 Jackson 序列化导致 `subscriptionRefs` 翻倍的 bug（`@JsonIgnore` + 序列化往返回归测试）。

## 测试

全量 58 通过。其中 NodeFilterTest 7、ConfigGeneratorServiceImplTest 5（含序列化回归测试）。

## 结论

All checks passed. Ready for archive.

可选改进：补「订阅源获取失败」专门测试（实现已存在，仅缺测试用例）。
