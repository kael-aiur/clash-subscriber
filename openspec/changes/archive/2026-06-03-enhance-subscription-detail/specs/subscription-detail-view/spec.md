## ADDED Requirements

### Requirement: YAML 解析完整性

系统 SHALL 在解析 Clash YAML 配置时提取 `proxy-groups`、`rules` 和 `name` 字段。

#### Scenario: 提取代理组数据
- **WHEN** YAML 内容包含 `proxy-groups` 字段
- **THEN** 系统将 `proxy-groups` 解析为 `Map<String, Object>` 并存入 `ClashConfig.proxyGroups`

#### Scenario: 提取规则数据
- **WHEN** YAML 内容包含 `rules` 字段
- **THEN** 系统将 `rules` 解析为 `List<Object>` 并存入 `ClashConfig.rules`

#### Scenario: 提取配置名称
- **WHEN** YAML 内容包含 `name` 字段
- **THEN** 系统将 `name` 存入 `ClashConfig.name`

#### Scenario: 字段缺失时的防御
- **WHEN** YAML 内容不包含 `proxy-groups` 或 `rules` 字段
- **THEN** 系统保留 `ClashConfig` 中对应字段的默认空值（空 Map 或空 List），不抛出异常

---

### Requirement: 配置关系标签页

详情抽屉 SHALL 包含「配置关系」标签页，以树形关系图展示代理组的层级引用关系，并提供详情面板。

#### Scenario: 标签页结构
- **WHEN** 用户切换到「配置关系」标签页
- **THEN** 系统显示左右分栏布局：左侧为节点组树形关系图，右侧为详情面板（初始为空提示）

#### Scenario: 树形图根节点
- **WHEN** 配置关系标签页渲染
- **THEN** 系统自动选择根节点：优先选择名称为 `GLOBAL` 的代理组，若不存在则选择第一个 `select` 类型的组

#### Scenario: 树形图节点展示
- **WHEN** 树形图渲染完成
- **THEN** 每个节点显示：组名、类型标签（带颜色）、成员数量

#### Scenario: 组类型颜色区分
- **WHEN** 代理组类型为 `select`
- **THEN** 类型标签显示为蓝色

#### Scenario: url-test 类型颜色
- **WHEN** 代理组类型为 `url-test` 或 `urltest`
- **THEN** 类型标签显示为绿色

#### Scenario: fallback 类型颜色
- **WHEN** 代理组类型为 `fallback`
- **THEN** 类型标签显示为橙色

#### Scenario: load-balance 类型颜色
- **WHEN** 代理组类型为 `load-balance`
- **THEN** 类型标签显示为紫色

#### Scenario: 其他类型颜色
- **WHEN** 代理组类型不属于上述类型
- **THEN** 类型标签显示为灰色

#### Scenario: 特殊策略高亮
- **WHEN** 树形图中出现 `DIRECT` 或 `REJECT` 策略节点
- **THEN** `DIRECT` 显示为绿色标签，`REJECT` 显示为红色标签

#### Scenario: 点击节点组查看详情
- **WHEN** 用户点击树形图中的某个代理组节点
- **THEN** 右侧面板显示该组的详细信息：组名、类型、测速 URL（如有）、测速间隔（如有）、成员列表

#### Scenario: 成员列表区分类型
- **WHEN** 详情面板展示组内成员
- **THEN** 系统区分「代理节点」和「子代理组」两类成员，子代理组名称可点击跳转到树形图中对应节点

#### Scenario: 关联规则展示
- **WHEN** 用户点击某个代理组节点
- **THEN** 详情面板底部展示引用该组作为策略的规则列表

#### Scenario: 规则类型筛选
- **WHEN** 关联规则列表展示
- **THEN** 系统提供规则类型下拉筛选器（DOMAIN、DOMAIN-SUFFIX、IP-CIDR、GEOIP 等），用户可按类型过滤

#### Scenario: 查看原始配置
- **WHEN** 用户点击详情面板中的「查看原始配置」按钮
- **THEN** 系统弹窗展示该代理组的原始 YAML 文本

---

## MODIFIED Requirements

### Requirement: 节点组标签页

详情抽屉 SHALL 包含「节点组」标签页，以可展开表格展示所有节点组，并通过颜色区分类型。

#### Scenario: 展示节点组列表
- **WHEN** 用户切换到「节点组」标签页
- **THEN** 系统以表格展示所有节点组，列为：组名（name）、类型（type，带颜色标签）、节点数（proxies 长度）、测速间隔（interval，仅 url-test/fallback 显示）

#### Scenario: 展开查看组内节点详情
- **WHEN** 用户点击某节点组行的展开按钮
- **THEN** 系统在展开行中以 tag 列表展示该组包含的所有节点名称，节点名称后附加类型和服务器信息（如 `节点名 (type@server)`）

#### Scenario: 组类型颜色区分
- **WHEN** 节点组表格渲染
- **THEN** 类型列使用颜色标签：select(蓝)、url-test/urltest(绿)、fallback(橙)、load-balance(紫)、其他(灰)

#### Scenario: 特殊策略高亮
- **WHEN** 组内成员包含 `DIRECT` 或 `REJECT`
- **THEN** `DIRECT` 显示为绿色标签，`REJECT` 显示为红色标签

#### Scenario: 节点组数量统计
- **WHEN** 节点组标签页渲染完成
- **THEN** 标签页标题显示节点组总数，格式为「节点组 (N)」

---

### Requirement: 规则标签页

详情抽屉 SHALL 包含「规则」标签页，以表格展示所有规则，支持搜索、类型筛选和策略跳转。

#### Scenario: 展示规则列表
- **WHEN** 用户切换到「规则」标签页
- **THEN** 系统以表格展示所有规则，列为：序号、类型（如 DOMAIN / DOMAIN-SUFFIX / IP-CIDR）、匹配值、策略（如 DIRECT / 代理组名）

#### Scenario: 搜索规则
- **WHEN** 用户在规则搜索框输入关键词
- **THEN** 系统实时过滤表格，仅显示匹配值或策略包含该关键词的规则

#### Scenario: 按类型筛选规则
- **WHEN** 用户在规则类型下拉框中选择某个类型
- **THEN** 系统仅显示该类型的规则

#### Scenario: 策略跳转
- **WHEN** 用户点击规则行中的策略名称
- **THEN** 系统自动切换到「配置关系」标签页，定位并高亮对应的代理组节点

#### Scenario: 查看原始 YAML
- **WHEN** 用户点击规则行的「查看原始」按钮
- **THEN** 系统弹窗展示该规则的原始 YAML 文本

#### Scenario: 规则数量统计
- **WHEN** 规则标签页渲染完成
- **THEN** 标签页标题显示规则总数，格式为「规则 (N)」
