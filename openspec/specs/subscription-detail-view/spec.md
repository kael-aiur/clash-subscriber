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

### Requirement: 代理节点标签页

详情抽屉 SHALL 包含"代理节点"标签页，按地区分组展示代理节点，支持展开/折叠和搜索过滤。

#### Scenario: 按地区分组展示
- **WHEN** 用户切换到"代理节点"标签页
- **THEN** 系统按国旗 emoji 自动识别节点地区，以折叠面板分组展示，每组标题显示国旗 emoji、地区名和节点数量

#### Scenario: 展开地区分组
- **WHEN** 用户点击某个地区分组的标题
- **THEN** 系统展开该分组，以表格展示该地区下所有节点，列为：名称、类型（type）、服务器地址（server）、端口（port）

#### Scenario: 折叠地区分组
- **WHEN** 用户再次点击已展开的地区分组标题
- **THEN** 系统折叠该分组，隐藏内部节点表格

#### Scenario: 无地区标识节点归入"其他"
- **WHEN** 节点名称不包含国旗 emoji
- **THEN** 系统将该节点归入"其他"分组，排在所有地区分组之后

#### Scenario: 地区分组排序
- **WHEN** 代理节点标签页渲染完成
- **THEN** 各地区分组按节点数量降序排列，"其他"分组始终排在最后

#### Scenario: 搜索过滤节点
- **WHEN** 用户在搜索框输入关键词
- **THEN** 系统实时过滤节点名称，仅展示匹配的节点，空组自动隐藏

#### Scenario: 节点数量统计
- **WHEN** 代理节点标签页渲染完成
- **THEN** 标签页标题显示节点总数，格式为"代理节点 (N)"

---

### Requirement: 节点组标签页

详情抽屉 SHALL 包含"节点组"标签页，以可展开表格展示所有节点组。

#### Scenario: 展示节点组列表
- **WHEN** 用户切换到"节点组"标签页
- **THEN** 系统以表格展示所有节点组，列为：组名（name）、类型（type）、节点数（proxies 长度）、测速间隔（interval，仅 url-test/fallback 显示）

#### Scenario: 展开查看组内节点
- **WHEN** 用户点击某节点组行的展开按钮
- **THEN** 系统在展开行中以 tag 列表展示该组包含的所有节点名称

#### Scenario: 节点组数量统计
- **WHEN** 节点组标签页渲染完成
- **THEN** 标签页标题显示节点组总数，格式为"节点组 (N)"

---

### Requirement: 规则标签页

详情抽屉 SHALL 包含"规则"标签页，以表格展示所有规则并支持搜索。

#### Scenario: 展示规则列表
- **WHEN** 用户切换到"规则"标签页
- **THEN** 系统以表格展示所有规则，列为：序号、类型（如 DOMAIN / DOMAIN-SUFFIX / IP-CIDR）、匹配值、策略（如 DIRECT / 代理组名）

#### Scenario: 搜索规则
- **WHEN** 用户在规则搜索框输入关键词
- **THEN** 系统实时过滤表格，仅显示匹配值或策略包含该关键词的规则

#### Scenario: 规则数量统计
- **WHEN** 规则标签页渲染完成
- **THEN** 标签页标题显示规则总数，格式为"规则 (N)"

---

### Requirement: 前端类型定义

系统 SHALL 为代理节点和节点组提供完整的 TypeScript 类型定义。

#### Scenario: ProxyNode 类型
- **WHEN** 开发者在前端代码中访问代理节点数据
- **THEN** TypeScript 编译器能够正确推断 name、type、server、port 等字段类型

#### Scenario: ProxyGroup 类型
- **WHEN** 开发者在前端代码中访问节点组数据
- **THEN** TypeScript 编译器能够正确推断 name、type、proxies、url、interval 等字段类型
