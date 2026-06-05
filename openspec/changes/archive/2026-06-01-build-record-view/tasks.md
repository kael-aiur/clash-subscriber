## 1. 后端模型扩展

- [x] 1.1 新增 BuildStep.java 模型类，定义 name、status、startedAt、finishedAt、input、output、errorMessage 字段
- [x] 1.2 修改 BuildRecord.java，增加 List<BuildStep> steps 字段及其 getter/setter
- [x] 1.3 修改前端 API 类型定义 build-pipeline.ts，增加 BuildStep 接口并更新 BuildRecord 类型

## 2. 后端执行逻辑增强

- [x] 2.1 修改 BuildPipelineServiceImpl.execute()，在"拉取主订阅配置"环节记录输入（订阅ID）和输出（ClashConfig 摘要）
- [x] 2.2 修改 BuildPipelineServiceImpl.execute()，在"合并额外订阅节点"环节记录输入（额外订阅ID列表）和输出（合并后节点数）
- [x] 2.3 修改 BuildPipelineServiceImpl.execute()，在"脚本处理"环节记录输入（脚本名称）和输出（处理结果），无脚本时设为 SKIPPED
- [x] 2.4 修改 BuildPipelineServiceImpl.execute()，在"推送到 Mihomo"环节记录输入（目标实例ID）和输出（推送结果）

## 3. 前端构建流程表格改造

- [x] 3.1 修改 BuildPipelineView.vue，移除"历史"按钮和抽屉组件
- [x] 3.2 修改 BuildPipelineView.vue，表格第一列改为展开列（type="expand"）
- [x] 3.3 实现展开行模板，显示最近 10 条构建记录列表，包含状态标签、时间、错误信息
- [x] 3.4 实现点击构建记录跳转到详情页 /build-records/{id}

## 4. 构建记录详情页

- [x] 4.1 新建 BuildRecordDetailView.vue 组件
- [x] 4.2 实现页面头部，显示构建记录基本信息（ID、时间、状态）
- [x] 4.3 使用 el-steps 组件展示 4 个构建环节的流程图
- [x] 4.4 实现点击环节显示该环节的输入输出数据（JSON 格式化）
- [x] 4.5 实现返回按钮，跳转回构建流程列表页
- [x] 4.6 修改 router/index.ts，添加 /build-records/:id 路由
