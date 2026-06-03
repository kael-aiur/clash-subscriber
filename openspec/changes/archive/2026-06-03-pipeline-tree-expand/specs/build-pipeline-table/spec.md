## MODIFIED Requirements

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
