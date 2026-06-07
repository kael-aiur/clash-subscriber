## ADDED Requirements

### Requirement: 全屏编辑器页面路由

系统 SHALL 提供独立的全屏脚本编辑器页面，路由路径为 `/scripts/edit/:name`。

#### Scenario: 访问已有脚本
- **WHEN** 用户访问 `/scripts/edit/my-script`
- **THEN** 系统加载名为 `my-script` 的脚本内容，进入全屏编辑器页面

#### Scenario: 访问新建脚本
- **WHEN** 用户访问 `/scripts/edit/__new__`
- **THEN** 系统显示空编辑器，脚本名称为空，内容为默认模板

#### Scenario: 脚本不存在
- **WHEN** 用户访问 `/scripts/edit/non-existent`，且该脚本不存在
- **THEN** 系统提示"脚本不存在"，提供返回按钮跳转到 `/scripts`

---

### Requirement: 左右分栏布局

编辑器页面 SHALL 采用左右分栏布局，左侧为试运行面板，右侧为代码编辑器。

#### Scenario: 页面布局
- **WHEN** 用户进入全屏编辑器页面
- **THEN** 页面占据 100vw × 100vh，左侧显示试运行面板，右侧显示代码编辑器

#### Scenario: 左侧面板默认宽度
- **WHEN** 页面首次加载
- **THEN** 左侧面板默认宽度为 260px

---

### Requirement: 可拖动分隔条

编辑器页面 SHALL 提供可拖动的分隔条，允许用户调整左右面板宽度。

#### Scenario: 拖动调整宽度
- **WHEN** 用户按住分隔条并向右拖动
- **THEN** 左侧面板宽度增加，右侧面板宽度相应减少

#### Scenario: 宽度限制
- **WHEN** 用户拖动分隔条
- **THEN** 左侧面板宽度 SHALL 保持在 200px 至 500px 范围内

#### Scenario: 分隔条视觉提示
- **WHEN** 用户鼠标悬停在分隔条上
- **THEN** 分隔条 SHALL 显示视觉变化（如颜色变化）提示可拖动

---

### Requirement: 页面离开保护

编辑器页面 SHALL 在用户有未保存修改时提供离开保护。

#### Scenario: 未保存修改离开
- **WHEN** 用户修改了脚本内容但未保存，尝试离开页面
- **THEN** 系统 SHALL 显示确认框"脚本尚未保存，确定离开？"

#### Scenario: 已保存状态离开
- **WHEN** 用户已保存所有修改，尝试离开页面
- **THEN** 系统直接跳转，不显示确认框

#### Scenario: 确认离开
- **WHEN** 用户在确认框中点击"确定"
- **THEN** 系统允许离开页面

#### Scenario: 取消离开
- **WHEN** 用户在确认框中点击"取消"
- **THEN** 系统取消离开操作，保持在当前页面
