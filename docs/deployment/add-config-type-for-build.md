# 构建流程配置类型功能部署文档

## 概述

本文档描述了为构建流程增加配置类型功能的部署步骤、验证方法和回滚方案。

**功能说明：**
- 为 BuildPipeline 增加 `configType` 字段（`subscription` 或 `config-profile`）
- 为 BuildPipeline 增加 `configProfileId` 字段（配置组合模式使用）
- 支持订阅源模式和配置组合模式两种配置来源
- 保持向后兼容性，现有构建流程自动迁移为订阅源模式

**变更版本：** V20240101_001

---

## 部署前准备

### 1. 备份数据

```bash
# 备份现有数据目录
cp -r data data_backup_$(date +%Y%m%d_%H%M%S)

# 或使用 tar 打包备份
tar -czf data_backup_$(date +%Y%m%d_%H%M%S).tar.gz data/
```

### 2. 确认当前版本

```bash
# 查看当前运行的容器版本
docker ps | grep clash-subscriber

# 查看当前构建流程数据
ls -la data/build-pipelines/
cat data/build-pipelines/*.json | jq '.name, .id'
```

### 3. 准备回滚脚本

确保回滚脚本已准备就绪：
- 代码回滚：`git stash` 或 `git checkout` 到上一个稳定版本
- 数据回滚：使用备份的数据目录

---

## 部署步骤

### 步骤 1：数据迁移

本项目使用文件存储（JSON），无需传统数据库迁移。新字段将在代码部署后自动生效。

**验证现有数据：**

```bash
# 检查现有构建流程数据
ls -la data/build-pipelines/

# 查看数据结构（确认没有 configType 和 configProfileId 字段）
cat data/build-pipelines/*.json | jq 'keys'
```

**预期结果：**
- 现有 JSON 文件中不包含 `configType` 和 `configProfileId` 字段
- 这是正常的，代码会自动处理向后兼容性

### 步骤 2：代码部署

#### 方式一：使用 Docker Compose（推荐）

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 停止现有服务
docker-compose down

# 3. 重新构建镜像
docker-compose build --no-cache

# 4. 启动服务
docker-compose up -d

# 5. 查看启动日志
docker-compose logs -f
```

#### 方式二：手动构建和部署

```bash
# 1. 拉取最新代码
git pull origin main

# 2. 编译项目
mvn clean package -DskipTests

# 3. 停止现有服务（如果使用 systemd）
sudo systemctl stop clash-subscriber

# 4. 替换 JAR 文件
cp module-web/target/*.jar /opt/clash-subscriber/app.jar

# 5. 启动服务
sudo systemctl start clash-subscriber

# 6. 查看启动日志
sudo journalctl -u clash-subscriber -f
```

### 步骤 3：验证部署

#### 3.1 服务启动验证

```bash
# 检查服务是否正常启动
curl -s http://localhost:31192/actuator/health | jq .

# 预期响应
{
  "status": "UP"
}
```

#### 3.2 API 接口验证

```bash
# 获取所有构建流程列表
curl -s http://localhost:31192/api/build-pipelines | jq .

# 验证现有构建流程的 configType 字段
curl -s http://localhost:31192/api/build-pipelines | jq '.[] | {id, name, configType}'

# 预期结果：所有现有构建流程的 configType 应为 "subscription"
```

#### 3.3 数据迁移验证

```bash
# 检查现有构建流程数据是否已自动迁移
cat data/build-pipelines/*.json | jq '.configType'

# 预期结果：所有文件都显示 "subscription"
```

#### 3.4 功能验证

**测试订阅源模式：**

```bash
# 创建订阅源模式构建流程
curl -X POST http://localhost:31192/api/build-pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试订阅源模式",
    "configType": "subscription",
    "primarySubscriptionId": "your-subscription-id",
    "targetInstanceId": "your-instance-id",
    "enabled": true
  }' | jq .
```

**测试配置组合模式：**

```bash
# 创建配置组合模式构建流程
curl -X POST http://localhost:31192/api/build-pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试配置组合模式",
    "configType": "config-profile",
    "configProfileId": "your-config-profile-id",
    "targetInstanceId": "your-instance-id",
    "enabled": true
  }' | jq .
```

**验证向后兼容性：**

```bash
# 创建没有指定 configType 的构建流程（应自动设置为 subscription）
curl -X POST http://localhost:31192/api/build-pipelines \
  -H "Content-Type: application/json" \
  -d '{
    "name": "测试向后兼容",
    "primarySubscriptionId": "your-subscription-id",
    "targetInstanceId": "your-instance-id",
    "enabled": true
  }' | jq .

# 验证返回结果中的 configType 字段
# 预期结果：configType 应为 "subscription"
```

---

## 回滚方案

### 回滚场景

1. **服务无法启动**：代码部署失败
2. **API 接口异常**：功能验证失败
3. **数据损坏**：现有构建流程数据异常

### 回滚步骤

#### 方式一：使用 Docker Compose 回滚

```bash
# 1. 停止服务
docker-compose down

# 2. 切换到上一个稳定版本
git checkout <previous-stable-commit>

# 3. 恢复备份数据（如果需要）
rm -rf data/
cp -r data_backup_YYYYMMDD_HHMMSS data/

# 4. 重新构建并启动
docker-compose build --no-cache
docker-compose up -d

# 5. 验证回滚
docker-compose logs -f
curl -s http://localhost:31192/actuator/health | jq .
```

#### 方式二：手动回滚

```bash
# 1. 停止服务
sudo systemctl stop clash-subscriber

# 2. 切换到上一个稳定版本
git checkout <previous-stable-commit>

# 3. 重新编译
mvn clean package -DskipTests

# 4. 恢复备份数据（如果需要）
rm -rf data/
cp -r data_backup_YYYYMMDD_HHMMSS data/

# 5. 替换 JAR 文件
cp module-web/target/*.jar /opt/clash-subscriber/app.jar

# 6. 启动服务
sudo systemctl start clash-subscriber

# 7. 验证回滚
sudo journalctl -u clash-subscriber -f
curl -s http://localhost:31192/actuator/health | jq .
```

#### 数据回滚

如果数据已损坏，需要从备份恢复：

```bash
# 1. 停止服务
docker-compose down

# 2. 恢复备份数据
rm -rf data/
cp -r data_backup_YYYYMMDD_HHMMSS data/

# 3. 重启服务
docker-compose up -d

# 4. 验证数据恢复
ls -la data/build-pipelines/
cat data/build-pipelines/*.json | jq '.name, .id'
```

---

## 监控和告警

### 监控指标

1. **服务健康状态**
   ```bash
   # 定期检查服务健康
   curl -s http://localhost:31192/actuator/health
   ```

2. **构建流程执行状态**
   ```bash
   # 查看最近的构建记录
   curl -s http://localhost:31192/api/build-records | jq '.[] | {id, buildPipelineId, status, startedAt}'
   ```

3. **数据完整性**
   ```bash
   # 检查所有构建流程的 configType 字段
   cat data/build-pipelines/*.json | jq '.configType' | sort | uniq -c

   # 预期结果：所有记录都应有 configType 值（不应为 null）
   ```

### 告警规则

1. **服务不可用**
   - 条件：健康检查连续失败 3 次
   - 动作：发送告警通知，检查服务日志

2. **构建流程执行失败**
   - 条件：构建记录状态为 "FAILED"
   - 动作：检查错误日志，验证配置正确性

3. **数据异常**
   - 条件：构建流程的 configType 为 null
   - 动作：检查向后兼容性逻辑，可能需要手动修复数据

### 日志监控

```bash
# 查看服务日志
docker-compose logs -f

# 查看特定日志级别
docker-compose logs -f | grep -E "ERROR|WARN"

# 查看构建流程相关日志
docker-compose logs -f | grep -i "build\|pipeline"
```

---

## 部署检查清单

### 部署前

- [ ] 备份现有数据
- [ ] 确认当前版本和数据状态
- [ ] 准备回滚脚本
- [ ] 通知相关人员

### 部署中

- [ ] 停止现有服务
- [ ] 拉取最新代码
- [ ] 重新构建镜像/编译代码
- [ ] 启动服务
- [ ] 检查启动日志

### 部署后

- [ ] 验证服务健康状态
- [ ] 验证 API 接口响应
- [ ] 验证现有数据迁移
- [ ] 测试订阅源模式构建流程
- [ ] 测试配置组合模式构建流程
- [ ] 测试向后兼容性
- [ ] 监控服务运行状态

---

## 风险评估

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|----------|
| 服务无法启动 | 高 | 低 | 准备回滚脚本，备份数据 |
| 向后兼容性问题 | 中 | 低 | 自动迁移逻辑，充分测试 |
| 配置组合模式执行失败 | 中 | 中 | 详细日志记录，快速回滚 |
| 数据损坏 | 高 | 低 | 部署前备份，回滚方案 |

---

## 常见问题

### Q1：现有构建流程的 configType 字段为 null，是否正常？

**A：** 这是正常的。代码会自动处理向后兼容性，当访问到 configType 为 null 的构建流程时，会自动将其设置为 "subscription"。您也可以手动触发一次构建流程来验证自动迁移是否正常工作。

### Q2：如何验证配置组合模式是否正常工作？

**A：** 创建一个配置组合模式的构建流程，然后手动执行一次。检查构建记录中的步骤，应包含"获取配置组合"步骤。

### Q3：回滚后数据会丢失吗？

**A：** 如果回滚前已备份数据，回滚后数据不会丢失。回滚只会影响代码版本，不会影响数据文件。但如果新版本代码已修改了数据结构（如添加了新字段），回滚后这些新字段将被忽略。

### Q4：部署过程中服务不可用的时间有多长？

**A：** 使用 Docker Compose 部署时，服务不可用时间约为 1-2 分钟（取决于镜像构建速度）。建议在业务低峰期进行部署。

---

## 联系方式

如有问题或需要支持，请联系：
- 开发团队：[开发团队联系方式]
- 运维团队：[运维团队联系方式]

---

## 变更记录

| 日期 | 版本 | 变更内容 | 作者 |
|------|------|----------|------|
| 2026-06-06 | 1.0 | 初始版本 | 凌绝 |
