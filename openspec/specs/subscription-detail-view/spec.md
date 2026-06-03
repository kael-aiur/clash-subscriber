## ADDED Requirements

### Requirement: 订阅源详情入口

系统 SHALL 在订阅源列表的操作列提供"详情"按钮。

#### Scenario: 打开详情抽屉
- **WHEN** 用户点击订阅源列表中某行的"详情"按钮
- **THEN** 系统调用 `POST /api/subscriptions/{id}/fetch` 获取订阅配置，成功后打开右侧抽屉展示详情

#### Scenario: 获取失败提示
- **WHEN** 用户点击"详情"按钮但 fetch 接口返回失败
- **THEN** 系统显示错误提示，不打开抽屉

---

### Requirement: 基本信息标签页

详情抽屉 SHALL 包含"基本信息"标签页，展示订阅源的元数据。

#### Scenario: 展示基本信息
- **WHEN** 详情抽屉打开且数据加载完成
- **THEN** 基本信息标签页以描述列表展示：配置名称、订阅 URL、User-Agent、自定义 Headers、最后获取时间

---

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

### Requirement: 代理节点标签页

详情抽屉 SHALL 包含「代理节点」标签页，按地区分组展示代理节点，支持展开/折叠和搜索过滤。

#### Scenario: 按地区分组展示
- **WHEN** 用户切换到「代理节点」标签页
- **THEN** 系统按国旗 emoji 自动识别节点地区，以折叠面板分组展示，每组标题显示国旗 emoji、地区名和节点数量

#### Scenario: 展开地区分组
- **WHEN** 用户点击某个地区分组的标题
- **THEN** 系统展开该分组，以表格展示该地区下所有节点，列为：名称、类型（type）、服务器地址（server）、端口（port）

#### Scenario: 折叠地区分组
- **WHEN** 用户再次点击已展开的地区分组标题
- **THEN** 系统折叠该分组，隐藏内部节点表格

#### Scenario: 无地区标识节点归入「其他」
- **WHEN** 节点名称不包含国旗 emoji
- **THEN** 系统将该节点归入「其他」分组，排在所有地区分组之后

#### Scenario: 地区分组排序
- **WHEN** 代理节点标签页渲染完成
- **THEN** 各地区分组按节点数量降序排列，「其他」分组始终排在最后

#### Scenario: 搜索过滤节点
- **WHEN** 用户在搜索框输入关键词
- **THEN** 系统实时过滤节点名称，仅展示匹配的节点，空组自动隐藏

#### Scenario: 节点数量统计
- **WHEN** 代理节点标签页渲染完成
- **THEN** 标签页标题显示节点总数，格式为「代理节点 (N)」

---

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

---

### Requirement: 前端类型定义

系统 SHALL 为代理节点和节点组提供完整的 TypeScript 类型定义。

#### Scenario: ProxyNode 类型
- **WHEN** 开发者在前端代码中访问代理节点数据
- **THEN** TypeScript 编译器能够正确推断 name、type、server、port 等字段类型

#### Scenario: ProxyGroup 类型
- **WHEN** 开发者在前端代码中访问节点组数据
- **THEN** TypeScript 编译器能够正确推断 name、type、proxies、url、interval 等字段类型
