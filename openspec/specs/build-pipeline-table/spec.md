### Requirement: 查询构建流程

系统 SHALL 支持查询所有构建流程列表和按 ID 查询单个构建流程详情，并支持在表格中直接展开查看构建记录。

#### Scenario: 查询所有构建流程
- **WHEN** 用户请求 GET /api/build-pipelines
- **THEN** 系统返回所有 BuildPipeline 列表

#### Scenario: 查询单个构建流程
- **WHEN** 用户请求 GET /api/build-pipelines/{id}
- **THEN** 系统返回对应 BuildPipeline；若不存在返回 404

---

### Requirement: 构建流程表格展开行

系统 SHALL 使用 el-table 的树状展开功能展示构建流程和构建记录，父节点为构建流程，子节点为构建历史。

#### Scenario: 树状展开显示构建记录
- **WHEN** 用户点击构建流程行左侧的展开图标
- **THEN** 系统 SHALL 懒加载该流程的构建记录，作为子节点显示在父行下方

#### Scenario: 叶子节点展示
- **WHEN** 构建记录作为子节点显示
- **THEN** 每条记录 SHALL 显示开始时间、状态标签、错误信息（如有），且不可再展开

#### Scenario: 叶子节点样式区分
- **WHEN** 子节点（构建记录）显示在表格中
- **THEN** 子节点 SHALL 与父节点（构建流程）有视觉区分（如缩进、字体大小、背景色）

#### Scenario: 点击叶子节点跳转详情
- **WHEN** 用户点击构建记录子节点
- **THEN** 系统 SHALL 路由跳转到构建记录详情页 /build-records/{id}

#### Scenario: 无构建记录时的展开
- **WHEN** 某构建流程没有任何构建记录
- **THEN** 展开后 SHALL 显示"暂无构建记录"提示或不显示展开图标

---

### Requirement: 构建记录详情页

系统 SHALL 提供独立的构建记录详情页，展示该次构建的完整流程图和每个环节的详细信息。

#### Scenario: 流程图展示
- **WHEN** 用户访问 /build-records/{id}
- **THEN** 系统 SHALL 使用步骤条组件展示 4 个构建环节，每个环节显示名称和状态图标（成功/失败/跳过）

#### Scenario: 环节详情查看
- **WHEN** 用户点击流程图中的某个环节
- **THEN** 系统 SHALL 展示该环节的输入数据和输出数据，使用 JSON 格式化显示

#### Scenario: 环节输入输出数据
- **WHEN** 查看环节详情
- **THEN** 系统 SHALL 显示：
  - 输入：该环节接收的数据（如订阅配置、节点列表）
  - 输出：该环节产出的数据（如处理后的配置）
  - 错误信息：如有错误则显示错误详情

#### Scenario: 返回构建流程列表
- **WHEN** 用户在详情页点击返回
- **THEN** 系统 SHALL 路由跳转回构建流程列表页 /build-pipelines

---

### Requirement: 移除历史抽屉

系统 SHALL 移除构建流程页面中的"历史"按钮和抽屉组件，改用展开行展示构建记录。

#### Scenario: 移除历史按钮
- **WHEN** 用户访问构建流程页面
- **THEN** 操作列中 SHALL 不再显示"历史"按钮

#### Scenario: 移除抽屉组件
- **WHEN** 构建流程页面加载
- **THEN** 页面 SHALL 不包含 el-drawer 组件
