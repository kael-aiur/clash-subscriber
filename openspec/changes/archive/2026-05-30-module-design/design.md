# 设计文档：模块设计

## Context

clash-subscriber 是一个 Clash/Mihomo 订阅管理服务，当前处于极早期阶段（仅有 pom.xml，无源码）。

**核心需求**：
- 管理多个服务商的 Clash 订阅链接
- 通过 User-Agent 请求头获取完整 Clash 配置（非纯 base64）
- 对多订阅源的节点进行组合、规则修改等前置处理
- 将处理后的配置推送到 Mihomo 实例
- 支持多 Mihomo 节点的动态管理
- 支持定时刷新

**约束**：
- 个人/小团队使用，不需要高可用或分布式
- 存储使用目录 + JSON 文件，无外部数据库依赖
- 单体 Spring Boot 应用部署

## Goals / Non-Goals

**Goals:**
- 清晰的模块边界，每个模块单一职责
- 存储层通过接口抽象，可随时切换实现（JSON → 数据库）
- 配置处理支持脚本扩展（GraalVM JS）
- 提供完整的 REST API 供 Vue 3 前端调用
- 支持定时任务自动刷新订阅并推送到 Mihomo

**Non-Goals:**
- 不支持高可用/集群部署
- 不实现用户认证/权限管理（单用户场景）
- 不实现订阅源的自动发现
- 不实现 Mihomo 的远程安装/管理
- 不支持 Clash（非 Mihomo）的直接管理

## Decisions

### D1：领域驱动模块架构

- **选择**：按业务领域划分模块，每个模块内聚，通过接口通信
- **理由**：模块边界清晰，职责明确；存储可切换；脚本引擎作为处理器的扩展点天然适配
- **已考虑 alternative**：
  - 分层架构（controller → service → repository）：简单但 service 层容易变成上帝类
  - 六边形架构：最灵活但对这个规模过度设计

### D2：JSON 文件存储

- **选择**：目录 + JSON 文件，通过 Repository 接口抽象
- **理由**：轻量，无外部依赖，个人使用足够；接口抽象保证可切换
- **已考虑 alternative**：
  - SQLite：需要额外依赖，对配置类数据过于重量
  - H2：内嵌数据库，但 JSON 文件更直观

### D3：GraalVM JS 脚本引擎

- **选择**：使用 GraalVM JS 作为脚本引擎，用户通过 JS 脚本扩展处理逻辑
- **理由**：Java 内置，零外部依赖；JS 语法广泛接受；可直接操作 Java 对象
- **已考虑 alternative**：
  - Python 脚本：需要 Jython 或外部进程，依赖重
  - 自定义 DSL：灵活性差，学习成本高

### D4：处理器链模式

- **选择**：Pipeline 由多个有序的 Processor 组成，每个 Processor 独立可测试
- **理由**：关注点分离，内置处理器和脚本处理器统一接口；Pipeline 通过 JSON 配置定义
- **已考虑 alternative**：
  - 固定处理流程：简单但不可扩展
  - 事件驱动：复杂度过高

### D5：Mihomo HTTP API 对接

- **选择**：通过 Mihomo 的 external-controller HTTP API 推送配置
- **理由**：Mihomo 原生支持，标准 REST 接口，支持健康检查和配置重载
- **已考虑 alternative**：
  - 文件写入 + 文件监听：需要共享文件系统，部署约束大

### D6：Maven 多模块

- **选择**：Maven 多模块项目，父 POM 管理依赖版本
- **理由**：Java 生态标准做法，模块间依赖清晰，编译隔离
- **已考虑 alternative**：
  - 单模块按包划分：简单但模块边界容易模糊

## Risks / Trade-offs

- **[Risk] JSON 文件存储的并发问题** → Mitigation：单体应用，通过文件锁或 Spring 事务保证一致性；后续可切换数据库
- **[Risk] GraalVM JS 脚本安全性** → Mitigation：脚本在沙箱中执行，限制可访问的 API；脚本存储在本地，不接受远程提交
- **[Risk] Mihomo API 版本兼容性** → Mitigation：MihomoHttpClient 做版本检测，记录 API 版本，不兼容时给出明确提示
- **[Trade-off] JSON 文件 vs 数据库** → 接受理由：个人使用场景，数据量小，JSON 文件足够；接口抽象保证未来可切换
- **[Trade-off] 单体 vs 微服务** → 接受理由：项目规模不需要分布式，单体降低复杂度

## Migration Plan

N/A — 本 change 不涉及部署变更，是纯模块设计。

实际实施时的部署步骤：
1. 构建：`mvn clean package`
2. 运行：`java -jar module-web/target/clash-subscriber-web.jar`
3. 首次运行自动创建 `data/` 目录结构
4. 通过 REST API 配置订阅源和 Mihomo 实例

## Open Questions

- Mihomo 的 `PUT /configs` API 是否接受完整的 Clash YAML？还是需要特定格式？
- GraalVM JS 在 Spring Boot 3 + Java 21 下的兼容性是否需要额外配置？
- Vue 3 前端是作为 Spring Boot 静态资源部署，还是独立部署？
