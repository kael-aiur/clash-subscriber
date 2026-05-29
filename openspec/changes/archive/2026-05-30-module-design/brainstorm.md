# Brainstorm: 模块设计

## 背景

clash-subscriber 是一个 Clash/Mihomo 订阅管理服务，处于极早期阶段（仅有 pom.xml，无源码）。需要从零开始设计模块划分。

## 技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 技术栈 | Spring Boot 3 + Java 21 | 生态成熟，快速开发 |
| 存储 | 目录 + JSON 文件 | 轻量，通过接口分层可切换 |
| Mihomo 对接 | HTTP API 推送 | 通过 external-controller API |
| 前端 | Vue 3 Web UI | 国内社区活跃，上手快 |
| 部署 | 单体应用 | 复杂度低，足够使用 |
| 脚本引擎 | GraalVM JS | Java 内置，零依赖 |

## 架构方案对比

### 方案 A：分层架构（传统 Spring Boot）
- controller → service → repository
- 优点：简单直接
- 缺点：service 层容易变成"上帝类"，职责边界模糊

### 方案 B：领域驱动模块架构（选定）
- 按业务领域划分模块，每个模块内聚
- 优点：模块边界清晰，存储可切换，脚本引擎天然适配
- 缺点：比方案 A 略复杂

### 方案 C：六边形架构（Ports & Adapters）
- 核心域逻辑完全隔离
- 优点：最灵活
- 缺点：对这个规模的项目过度设计

**结论**：选择方案 B。

## 模块结构

```
clash-subscriber/
├── pom.xml                          # 父 POM（多模块）
├── module-common/                   # 共享层
│   └── src/main/java/site/kael/clash/common/
│       ├── model/                   # 共享领域模型
│       ├── exception/               # 统一异常
│       └── util/                    # 工具类
├── module-subscription/             # 订阅源管理
│   └── src/main/java/site/kael/clash/subscription/
│       ├── api/                     # 对外接口
│       ├── service/                 # 业务逻辑
│       ├── repository/              # 存储接口 + JSON 实现
│       └── model/                   # 订阅源模型
├── module-processor/                # 配置处理 Pipeline
│   └── src/main/java/site/kael/clash/processor/
│       ├── api/                     # Pipeline 接口
│       ├── pipeline/                # 默认处理流程
│       ├── script/                  # GraalVM JS 脚本引擎
│       └── builtin/                 # 内置处理器
├── module-mihomo/                   # Mihomo 实例管理
│   └── src/main/java/site/kael/clash/mihomo/
│       ├── api/                     # 对外接口
│       ├── service/                 # 实例管理
│       ├── client/                  # Mihomo HTTP 客户端
│       ├── repository/              # 实例存储
│       └── model/                   # 实例模型
├── module-scheduler/                # 定时任务
│   └── src/main/java/site/kael/clash/scheduler/
│       ├── api/                     # 调度接口
│       └── service/                 # 定时逻辑
└── module-web/                      # Web 层
    ├── src/main/java/site/kael/clash/web/
    │   ├── controller/              # REST Controller
    │   └── config/                  # Web 配置
    └── src/main/resources/
        └── static/                  # Vue 3 构建产物
```

## 核心领域模型

### ProxyNode（代理节点）
- name, type, server, port, extra

### ClashConfig（Clash 配置）
- name, proxies, proxyGroups, rules, raw

### Subscription（订阅源）
- id, name, url, userAgent, headers, createdAt, updatedAt, lastFetchedAt

### MihomoInstance（Mihomo 实例）
- id, name, apiUrl, apiSecret, enabled, status

### ScheduledTask（定时任务）
- id, name, pipelineId, targetInstances, cronExpression, enabled, lastRunAt, lastRunStatus

## 存储设计

接口 + JSON 文件实现，通过 Spring Profile 切换。

```
data/
├── subscriptions/           # 订阅源数据
├── mihomo-instances/        # Mihomo 实例数据
├── scripts/                 # 用户自定义脚本
└── cache/                   # 订阅缓存
```

## 订阅获取流程

1. 根据 ID 查找订阅源
2. 构建 HTTP 请求（设置 User-Agent 和自定义请求头）
3. 判断响应：YAML 直接解析，Base64 解码后组装
4. 缓存结果到 data/cache/
5. 更新 lastFetchedAt

## 处理 Pipeline 设计

处理器链模式，每个处理器独立可组合：
- NodeMergeProcessor — 合并多源节点
- RuleModifyProcessor — 修改出站规则
- ProxyGroupProcessor — 重组代理组
- DuplicateRemoveProcessor — 去重
- ScriptProcessor — 执行 GraalVM JS 脚本

Pipeline 通过 JSON 配置定义执行步骤和顺序。

## Mihomo 集成

MihomoHttpClient 封装 HTTP API：
- PUT /configs — 推送配置
- GET /version — 健康检查
- POST /configs/reload — 重载

## REST API 端点

- /api/subscriptions — 订阅源 CRUD + 手动获取
- /api/pipelines — Pipeline CRUD + 手动执行
- /api/mihomo-instances — 实例 CRUD + 健康检查 + 推送
- /api/scheduled-tasks — 任务 CRUD + 手动触发
- /api/scripts — 脚本管理

## 模块依赖

```
common ← subscription ← processor
common ← mihomo
subscription + processor + mihomo ← scheduler
all ← web
```

## 实现顺序

1. module-common — 模型、工具类、异常
2. module-subscription — 订阅 CRUD + 获取解析
3. module-mihomo — 实例管理 + 配置推送
4. module-processor — 处理 Pipeline + 脚本引擎
5. module-scheduler — 定时任务
6. module-web — REST API + Vue 3 前端
