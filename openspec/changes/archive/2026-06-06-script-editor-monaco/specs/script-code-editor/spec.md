## ADDED Requirements

### Requirement: 脚本编辑器 SHALL 使用 Monaco Editor 组件

脚本管理页面的编辑区域 SHALL 使用 Monaco Editor 替代原生 textarea，提供专业的代码编辑体验。

#### Scenario: 打开脚本编辑对话框
- **WHEN** 用户点击"新建"或"编辑"按钮
- **THEN** 编辑对话框中 SHALL 显示 Monaco Editor 组件，而非 textarea

#### Scenario: 查看脚本内容
- **WHEN** 用户点击"查看"按钮
- **THEN** 对话框中 SHALL 以只读模式显示 Monaco Editor，展示脚本内容

---

### Requirement: 编辑器 SHALL 提供 JavaScript 语法高亮

Monaco Editor SHALL 对脚本内容进行 JavaScript 语法高亮显示，包括关键字、字符串、注释、函数名等不同颜色区分。

#### Scenario: 编辑 JavaScript 脚本
- **WHEN** 用户在编辑器中输入 JavaScript 代码
- **THEN** 代码 SHALL 以 JavaScript 语法高亮模式显示

---

### Requirement: 编辑器 SHALL 显示行号和括号匹配

Monaco Editor SHALL 显示行号，并在光标位于括号附近时高亮匹配的括号对。

#### Scenario: 查看行号
- **WHEN** 编辑器加载脚本内容
- **THEN** 编辑器左侧 SHALL 显示行号

#### Scenario: 括号匹配高亮
- **WHEN** 光标位于 `(`、`)`、`{`、`}`、`[`、`]` 附近
- **THEN** 匹配的括号对 SHALL 高亮显示

---

### Requirement: 编辑器 SHALL 提供 JavaScript 语法错误诊断

Monaco Editor SHALL 使用 TypeScript/JavaScript 语言服务对脚本进行实时语法检查，在有语法错误的位置显示红色波浪线和错误提示。

#### Scenario: 输入有语法错误的代码
- **WHEN** 用户输入包含语法错误的 JavaScript 代码（如缺少括号、未闭合字符串）
- **THEN** 错误位置 SHALL 显示红色波浪线

#### Scenario: 悬停查看错误详情
- **WHEN** 用户将鼠标悬停在红色波浪线上
- **THEN** SHALL 显示错误描述信息

---

### Requirement: 编辑器 SHALL 支持代码格式化

编辑器工具栏 SHALL 提供"格式化"按钮，点击后对当前编辑器内容进行 JavaScript 代码格式化。

#### Scenario: 格式化代码
- **WHEN** 用户点击"格式化"按钮
- **THEN** 编辑器中的 JavaScript 代码 SHALL 按照标准格式重新缩进和排版
