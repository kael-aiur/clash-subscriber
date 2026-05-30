## 1. Maven 模块搭建

- [x] 1.1 创建 module-pipeline 模块目录结构和 pom.xml，配置对 module-subscription、module-processor、module-mihomo、module-common 的依赖
- [x] 1.2 在根 pom.xml 中添加 module-pipeline 模块声明
- [x] 1.3 在 module-web 的 pom.xml 中添加对 module-pipeline 的依赖

## 2. 数据模型与存储

- [x] 2.1 创建 BuildPipeline 模型类（id、name、primarySubscriptionId、additionalSubscriptionIds、scriptName、targetInstanceId、cronExpression、enabled、时间戳）
- [x] 2.2 创建 BuildRecord 模型类（id、buildPipelineId、startedAt、finishedAt、status、errorMessage、logs）
- [x] 2.3 创建 BuildPipelineRepository 接口和 JsonFileBuildPipelineRepository 实现
- [x] 2.4 创建 BuildRecordRepository 接口和 JsonFileBuildRecordRepository 实现

## 3. 核心服务层

- [x] 3.1 创建 BuildPipelineService 接口（CRUD + execute + findRecords）
- [x] 3.2 实现 BuildPipelineServiceImpl 的 CRUD 方法
- [x] 3.3 实现 execute 方法：拉取主订阅 → 合并额外订阅 → 生成 PipelineConfig → 执行脚本 → 推送到目标实例 → 记录 BuildRecord
- [x] 3.4 实现 findRecords / findRecordById 方法

## 4. 定时调度集成

- [x] 4.1 修改 SchedulerServiceImpl，添加感知 BuildPipeline cron 配置的能力（启动时加载、增删改同步）
- [x] 4.2 BuildPipelineServiceImpl 的 create/update/delete 中同步注册/更新/取消 cron 任务

## 5. REST API

- [x] 5.1 创建 BuildPipelineController（CRUD + /{id}/execute + /{id}/records）
- [x] 5.2 创建 BuildRecordController（GET /{id}）

## 6. 前端 - API 层与路由

- [x] 6.1 创建 build-pipeline.ts API 模块（CRUD、execute、records）
- [x] 6.2 在 router/index.ts 中添加 /build-pipelines 路由
- [x] 6.3 在 App.vue 侧边栏中添加"构建流程"菜单项

## 7. 前端 - 构建流程管理页面

- [x] 7.1 创建 BuildPipelineView.vue 列表页（表格展示、操作按钮）
- [x] 7.2 实现新建/编辑弹窗（表单：名称、订阅选择、脚本选择、实例选择、cron、启用开关）
- [x] 7.3 实现手动触发构建功能
- [x] 7.4 实现构建历史抽屉（展示 BuildRecord 列表，支持查看日志详情）
