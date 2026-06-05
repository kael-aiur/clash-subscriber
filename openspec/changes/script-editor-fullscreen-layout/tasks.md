## 1. 后端 API 变更

- [ ] 1.1 在 ScriptController.java 中新增 `POST /api/scripts/preview-subscription` 端点，接收 subscriptionId，返回配置摘要和 YAML
- [ ] 1.2 修改 `POST /api/scripts/try-run` 端点，返回格式新增 inputSummary、inputYaml、outputSummary、outputYaml 字段

## 2. 前端 API 层

- [ ] 2.1 在 api/script.ts 中新增 `previewSubscription(subscriptionId)` API 方法
- [ ] 2.2 更新 api/script.ts 中的 TryRunResult 类型定义，新增 inputSummary、inputYaml、outputSummary、outputYaml 字段

## 3. 前端组件开发

- [ ] 3.1 创建 ScriptCodePanel.vue 右侧面板组件（工具栏 + Monaco Editor）
- [ ] 3.2 创建 ScriptTrialPanel.vue 左侧面板组件（名称输入、订阅源选择、试运行按钮、el-steps、输入/输出卡片）
- [ ] 3.3 创建 ScriptEditorView.vue 容器组件（全屏布局、可拖动分隔条、路由参数处理、面板通信）

## 4. 路由与列表页集成

- [ ] 4.1 在 router/index.ts 中新增 `/scripts/edit/:name` 路由
- [ ] 4.2 修改 ScriptView.vue 列表页，"编辑"按钮改为路由跳转，移除编辑对话框相关代码

## 5. 测试与验证

- [ ] 5.1 验证全屏编辑器页面路由跳转正常（编辑已有脚本、新建脚本）
- [ ] 5.2 验证可拖动分隔条功能（拖动调整宽度、最小/最大限制）
- [ ] 5.3 验证试运行分步执行流程（步骤 1→2→3、失败停止）
- [ ] 5.4 验证输入/输出 ConfigCard 卡片展示（统计标签、节点预览、YAML 展开）
- [ ] 5.5 验证页面离开保护（未保存修改确认框）
