## Why

订阅供应商的 Clash 配置中包含了精心调校的规则集（域名分流、地理 IP 判断等），但当前系统只能整体使用订阅配置，无法单独提取和复用其中的规则部分。用户在构建自定义配置时，不得不手动复制粘贴规则，或者完全依赖脚本处理，效率低下且容易出错。规则组功能让用户可以从订阅中一键提取规则集合，并通过代理对象映射机制在不同配置场景中灵活复用。

## What Changes

**新增规则组管理能力**
- From: 订阅配置只能整体使用，无法单独提取规则
- To: 支持从订阅中提取规则组，也支持手动创建规则组
- Reason: 复用供应商规则，减少手动配置工作量
- Impact: 新增 API 端点和前端页面，不影响现有功能

**代理对象抽象机制**
- From: 规则中的代理名是硬编码的字符串
- To: 代理名被抽象为代理对象（占位符），使用时通过映射替换为实际名称
- Reason: 使规则组可在不同配置场景中复用，只需调整映射关系
- Impact: 规则存储格式变化，使用 `{{px-001}}` 占位符

**订阅列表页集成**
- From: 订阅列表只有编辑、删除、获取操作
- To: 增加"提取规则组"/"重新提取"按钮，显示规则组状态
- Reason: 提取操作与订阅紧密关联，放在订阅列表最直观
- Impact: 订阅列表页 UI 变化

## Capabilities

### New Capabilities
- `rule-group-management`: 规则组的 CRUD、从订阅提取、代理对象管理、规则结构化展示与编辑

### Modified Capabilities
- `subscription-management`: 订阅列表页增加规则组提取入口和状态展示

## Impact

**后端代码**
- module-processor: 新增 RuleGroup 模型、Repository、Service、Controller
- module-subscription: SubscriptionService 需暴露获取 ClashConfig 的方法供提取使用

**API**
- 新增 `/api/rule-groups` 系列端点（CRUD + extract）

**前端**
- 新增规则组列表页、详情/编辑页、创建表单
- 修改订阅列表页，增加规则组相关操作按钮

**数据存储**
- 新增 `data/rule-groups/` 目录，JSON 文件持久化

**依赖**
- 无新增外部依赖
