# 项目 README 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为开源项目创建专业的 README 文档和 Docker 部署配置

**Architecture:** 创建 README.md 主文档、docker-compose.yml 部署配置、docs/images/ 截图目录

**Tech Stack:** Markdown, Docker, Docker Compose

---

## 文件结构

- `README.md` — 主文档（修改）
- `docker-compose.yml` — Docker 部署配置（新建）
- `Dockerfile` — 应用镜像构建文件（新建）
- `docs/images/` — 截图目录（新建）

---

### Task 1: 创建 Dockerfile

**Files:**
- Create: `Dockerfile`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
# 构建阶段
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY module-common/pom.xml module-common/
COPY module-subscription/pom.xml module-subscription/
COPY module-processor/pom.xml module-processor/
COPY module-mihomo/pom.xml module-mihomo/
COPY module-scheduler/pom.xml module-scheduler/
COPY module-pipeline/pom.xml module-pipeline/
COPY module-web/pom.xml module-web/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B

# 运行阶段
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/module-web/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 验证 Dockerfile 语法**

Run: `docker build --check .`
Expected: 无语法错误

- [ ] **Step 3: 提交**

```bash
git add Dockerfile
git commit -m "feat(docker): 添加 Dockerfile 构建配置"
```

---

### Task 2: 创建 docker-compose.yml

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 1: 创建 docker-compose.yml**

```yaml
version: '3.8'

services:
  clash-subscriber:
    build: .
    container_name: clash-subscriber
    ports:
      - "8080:8080"
    volumes:
      - ./data:/app/data
    environment:
      - TZ=Asia/Shanghai
    restart: unless-stopped
```

- [ ] **Step 2: 验证 docker-compose.yml 语法**

Run: `docker-compose config`
Expected: 配置正确输出

- [ ] **Step 3: 提交**

```bash
git add docker-compose.yml
git commit -m "feat(docker): 添加 docker-compose.yml 部署配置"
```

---

### Task 3: 创建截图目录和占位文件

**Files:**
- Create: `docs/images/.gitkeep`

- [ ] **Step 1: 创建截图目录**

Run: `mkdir -p docs/images`

- [ ] **Step 2: 创建 .gitkeep 文件**

Run: `touch docs/images/.gitkeep`

- [ ] **Step 3: 提交**

```bash
git add docs/images/.gitkeep
git commit -m "docs: 创建截图目录"
```

---

### Task 4: 编写 README.md

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 编写 README 内容**

将以下内容写入 `README.md`：

```markdown
# Clash Subscriber

Clash/Mihomo 订阅管理中心 — 集中管理多个订阅源，自动同步到 Mihomo 实例

## 功能亮点

- 📦 **多订阅源管理** — 保存和管理多个服务商的 Clash 订阅链接
- 🔄 **自动同步** — 定时从订阅源获取配置，自动刷新 Mihomo 实例
- 🔀 **节点合并** — 多订阅源节点自动合并，统一管理
- 🛠️ **规则处理** — 支持出站规则修改、规则集过滤等前置处理
- 🖥️ **实例管理** — 支持多 Mihomo 节点的动态管理和健康检查
- 📊 **转发路径可视化** — Vue Flow 流程图展示域名转发路径
- 📜 **脚本引擎** — 支持自定义 JavaScript 脚本处理配置

## 截图

> 截图待补充

| 订阅管理 | 实例管理 | 转发路径 |
|---------|---------|---------|
| ![订阅管理](docs/images/subscription.png) | ![实例管理](docs/images/instance.png) | ![转发路径](docs/images/forwarding-path.png) |

## 快速开始

### Docker 部署（推荐）

1. 克隆项目

```bash
git clone https://github.com/your-username/clash-subscriber.git
cd clash-subscriber
```

2. 启动服务

```bash
docker-compose up -d
```

3. 访问应用

打开浏览器访问 http://localhost:8080

### 手动部署

需要 Java 21 环境：

```bash
# 克隆项目
git clone https://github.com/your-username/clash-subscriber.git
cd clash-subscriber

# 编译打包
mvn clean package -DskipTests

# 运行
java -jar module-web/target/*.jar
```

## 配置说明

### 端口配置

默认端口：`8080`

修改 `module-web/src/main/resources/application.yml`：

```yaml
server:
  port: 8080
```

或通过环境变量：

```bash
docker-compose run -e SERVER_PORT=9090 clash-subscriber
```

### 数据目录

应用数据存储在 `data/` 目录，包括：

- `subscriptions/` — 订阅源数据
- `mihomo-instances/` — Mihomo 实例配置
- `build-pipelines/` — 构建流程配置
- `build-records/` — 构建记录
- `scripts/` — 自定义脚本
- `scheduled-tasks/` — 定时任务配置

## 架构概览

```
┌─────────────────────────────────┐
│  REST API 层                     │  订阅管理、Mihomo 节点管理、配置推送
├─────────────────────────────────┤
│  订阅处理层                      │  订阅获取、解析、节点组合、规则处理
├─────────────────────────────────┤
│  Mihomo 对接层                   │  配置写入、实例管理、健康检查
├─────────────────────────────────┤
│  持久层                          │  订阅源存储、Mihomo 节点存储、配置历史
└─────────────────────────────────┘
```

### 模块说明

| 模块 | 说明 |
|------|------|
| `module-common` | 公共工具和基础类 |
| `module-subscription` | 订阅源管理 |
| `module-processor` | 配置处理和脚本引擎 |
| `module-mihomo` | Mihomo 实例对接 |
| `module-scheduler` | 定时任务调度 |
| `module-pipeline` | 构建流程管理 |
| `module-web` | REST API 和前端 |

## 常见问题

### Q: 如何添加订阅源？

A: 在「订阅管理」页面点击「新增」，填写订阅链接和请求参数即可。

### Q: 支持哪些订阅格式？

A: 支持 Clash YAML 配置格式和纯 base64 节点列表格式。

### Q: 如何查看转发路径？

A: 在「Mihomo 实例」详情页的「转发规则」标签页，输入域名即可查看转发路径流程图。

### Q: 数据存储在哪里？

A: 默认存储在项目根目录的 `data/` 文件夹，可通过配置修改。

## 贡献指南

### 提交 Issue

- 使用 GitHub Issues 报告 bug 或提出功能建议
- 请提供详细的复现步骤和环境信息

### 贡献代码

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'feat: 添加某功能'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 许可证。
```

- [ ] **Step 2: 验证 Markdown 格式**

Run: `cat README.md | head -50`
Expected: 格式正确，无语法错误

- [ ] **Step 3: 提交**

```bash
git add README.md
git commit -m "docs: 编写项目 README 文档"
```

---

### Task 5: 更新 .gitignore

**Files:**
- Modify: `.gitignore`

- [ ] **Step 1: 添加 Docker 相关忽略规则**

在 `.gitignore` 末尾添加：

```
# Docker
.docker/
```

- [ ] **Step 2: 提交**

```bash
git add .gitignore
git commit -m "chore: 更新 .gitignore 添加 Docker 忽略规则"
```
