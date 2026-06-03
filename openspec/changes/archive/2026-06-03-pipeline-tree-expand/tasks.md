## 1. 前端类型和数据结构

- [x] 1.1 在 build-pipeline.ts 中新增 TreeRow 接口，定义 id、type（pipeline/record）、name 以及 pipeline 和 record 的可选字段
- [x] 1.2 在 BuildPipelineView.vue 中移除嵌套表格相关的状态（expandedRecords、expandedLoading）和方法（loadRecords、handleExpandChange）

## 2. 表格改为树状展开

- [x] 2.1 修改 el-table，添加 lazy 和 load 属性，load 回调实现懒加载构建记录
- [x] 2.2 移除 type="expand" 列，改为普通列展示 pipeline 字段，通过 row 的 hasChildren 字段控制展开图标
- [x] 2.3 实现 loadTreeChildren 方法：调用 buildPipelineApi.getRecords 获取记录，转换为 TreeRow 并通过 resolve 回返
- [x] 2.4 叶子节点（type=record）显示开始时间、状态标签、错误信息，点击跳转详情页
- [x] 2.5 通过 CSS 区分叶子节点样式（缩进、字体、背景色）
