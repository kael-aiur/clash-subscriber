## 1. 项目基础搭建

- [x] 1.1 创建 Maven 多模块项目结构（父 POM + 6 个子模块）
- [x] 1.2 配置 Spring Boot 3 依赖和插件
- [x] 1.3 配置 GraalVM JS、Jackson、SnakeYAML、OkHttp 依赖
- [x] 1.4 创建 data/ 目录结构和 .gitkeep 文件

## 2. module-common 共享层

- [x] 2.1 创建 ProxyNode 模型类
- [x] 2.2 创建 ClashConfig 模型类
- [x] 2.3 创建统一异常定义（BusinessException 等）
- [x] 2.4 创建 YAML 解析工具类
- [x] 2.5 创建 Base64 解码工具类
- [x] 2.6 创建 ID 生成工具类

## 3. module-subscription 订阅源管理

- [x] 3.1 创建 Subscription 模型类
- [x] 3.2 定义 SubscriptionRepository 接口
- [x] 3.3 实现 JsonFileSubscriptionRepository
- [x] 3.4 定义 SubscriptionService 接口
- [x] 3.5 实现 SubscriptionService（CRUD 逻辑）
- [x] 3.6 实现订阅获取逻辑（HTTP 请求 + User-Agent）
- [x] 3.7 实现订阅解析逻辑（YAML / Base64 自动检测）
- [x] 3.8 实现订阅缓存逻辑

## 4. module-mihomo 实例管理

- [x] 4.1 创建 MihomoInstance 模型类
- [x] 4.2 定义 MihomoInstanceRepository 接口
- [x] 4.3 实现 JsonFileMihomoInstanceRepository
- [x] 4.4 创建 MihomoHttpClient（HTTP API 客户端）
- [x] 4.5 定义 MihomoService 接口
- [x] 4.6 实现 MihomoService（CRUD + 健康检查 + 配置推送）

## 5. module-processor 配置处理

- [x] 5.1 定义 ConfigProcessor 接口
- [x] 5.2 创建 ProcessingContext 模型
- [x] 5.3 实现 NodeMergeProcessor（节点合并）
- [x] 5.4 实现 RuleModifyProcessor（规则修改）
- [x] 5.5 实现 ProxyGroupProcessor（代理组重组）
- [x] 5.6 实现 DuplicateRemoveProcessor（去重）
- [x] 5.7 实现 ScriptProcessor（GraalVM JS 脚本引擎）
- [x] 5.8 定义 Pipeline 配置模型
- [x] 5.9 实现 Pipeline 执行引擎

## 6. module-scheduler 定时任务

- [x] 6.1 创建 ScheduledTask 模型类
- [x] 6.2 定义 SchedulerService 接口
- [x] 6.3 实现 SchedulerService（任务管理 + cron 调度）
- [x] 6.4 实现任务执行逻辑（获取订阅 → Pipeline → 推送）

## 7. module-web REST API

- [x] 7.1 创建 SubscriptionController
- [x] 7.2 创建 PipelineController
- [x] 7.3 创建 MihomoInstanceController
- [x] 7.4 创建 ScheduledTaskController
- [x] 7.5 创建 ScriptController
- [x] 7.6 配置 CORS 和全局异常处理

## 8. module-web Vue 3 前端

- [x] 8.1 初始化 Vue 3 项目（Vite + TypeScript）
- [x] 8.2 实现订阅源管理页面
- [x] 8.3 实现 Mihomo 实例管理页面
- [x] 8.4 实现定时任务管理页面
- [x] 8.5 实现脚本管理页面
- [x] 8.6 配置构建集成（前端构建产物输出到 static/）
