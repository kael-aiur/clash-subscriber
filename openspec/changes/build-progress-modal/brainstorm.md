# 构建进度弹窗设计 - Brainstorm 记录

## 1. 项目上下文探索

### 1.1 当前实现分析

**构建流水线页面**：
- 主页面：`module-web/frontend/src/views/BuildPipelineView.vue`
- 记录详情：`module-web/frontend/src/views/BuildRecordDetailView.vue`
- API 层：`module-web/frontend/src/api/build-pipeline.ts`

**后端实现**：
- 控制器：`module-web/src/main/java/site/kael/clash/web/controller/BuildPipelineController.java`
- 服务层：`module-pipeline/src/main/java/site/kael/clash/pipeline/service/impl/BuildPipelineServiceImpl.java`
- 数据模型：`BuildRecord`、`BuildStep`

### 1.2 关键发现

1. **同步阻塞接口**：`POST /api/build-pipelines/{id}/execute` 是同步的，阻塞直到构建完成
2. **无实时进度**：没有 WebSocket/SSE/轮询机制
3. **现有模型支持**：`BuildStep` 已有状态（RUNNING/SUCCESS/FAILED/WAITING/SKIPPED）和日志结构
4. **构建流程**：
   - 订阅模式：拉取主订阅 → 合并额外节点 → 脚本处理 → 推送 Mihomo
   - 配置组合模式：获取配置组合 → 生成完整配置 → 脚本处理 → 推送 Mihomo

---

## 2. 澄清问题与答案

### 问题 1：进度展示形式
**选项**：
- 步骤条（推荐）
- 进度条 + 文字
- 日志流面板

**用户选择**：步骤条（推荐）

**理由**：复用现有 BuildStep 模型，每个步骤显示状态图标（等待/进行中/成功/失败），类似 BuildRecordDetailView 的垂直步骤条

### 问题 2：进度推送机制
**选项**：
- SSE（推荐）
- 轮询
- WebSocket

**用户选择**：SSE（推荐）

**理由**：后端单向推送，浏览器原生支持，实现简单，适合进度场景

### 问题 3：构建完成后的交互
**选项**：
- 状态摘要 + 确定按钮
- 保持同步，弹窗等待

**用户选择**：状态摘要 + 确定按钮

**理由**：显示成功/失败状态 + 构建摘要（耗时、步骤数）+ 确定按钮，简洁明了

### 问题 4：边界情况处理
**选项**：
- 后台继续（推荐）
- 强制等待

**用户选择**：后台继续（推荐）

**理由**：弹窗可关闭，构建继续后台运行，用户可从构建记录列表查看结果

---

## 3. 设计方案对比

### 方案 A：最小改动（轮询 + 弹窗等待）
- **思路**：保持同步接口不变，前端用遮罩弹窗包裹等待过程
- **优点**：改动最小，只需修改前端
- **缺点**：❌ 没有实时进度，用户只能看到 loading

### 方案 B：异步 + SSE 推送（推荐）⭐⭐⭐
- **思路**：execute 改为异步执行，立即返回 recordId，前端通过 SSE 订阅进度
- **优点**：✅ 实时进度，用户体验好；✅ 复用现有 BuildStep 模型；✅ 技术成熟
- **缺点**：需要改造后端 execute 为异步

### 方案 C：WebSocket 双向通信
- **思路**：用 WebSocket 替代 SSE
- **优点**：最实时，双向通信
- **缺点**：❌ 过度设计，实现复杂，需要额外依赖

**用户选择**：方案 B（异步 + SSE 推送）

---

## 4. 详细设计

### 4.1 整体架构设计

#### 数据流概览
```
用户点击构建
    ↓
前端调用 POST /api/build-pipelines/{id}/execute
    ↓
后端立即返回 { recordId: "xxx" }
    ↓
后端异步执行构建流程
    ↓
前端订阅 SSE: GET /api/build-records/{id}/progress
    ↓
后端推送步骤状态变更事件
    ↓
前端实时更新步骤条
    ↓
构建完成 → 推送完成事件 + 摘要
    ↓
前端显示摘要 + 确定按钮
```

#### 核心组件变更

| 组件 | 变更类型 | 说明 |
|------|----------|------|
| `BuildPipelineController.execute()` | 修改 | 同步 → 异步，立即返回 recordId |
| `BuildRecordController` | 新增 | SSE 端点 `/api/build-records/{id}/progress` |
| `BuildPipelineServiceImpl.execute()` | 修改 | 异步化，推送进度事件 |
| `BuildProgressModal.vue` | 新增 | 遮罩弹窗 + 步骤条 + SSE 订阅 |
| `BuildPipelineView.vue` | 修改 | handleExecute 改为调用新接口 + 显示弹窗 |

#### 接口变更

**原接口（同步）**：
```http
POST /api/build-pipelines/{id}/execute
Response: BuildRecord (完整记录，阻塞等待)
```

**新接口（异步）**：
```http
POST /api/build-pipelines/{id}/execute
Response: { "recordId": "xxx" }  // 立即返回

GET /api/build-records/{id}/progress
Accept: text/event-stream
Response: SSE 事件流
```

### 4.2 SSE 事件格式设计

#### 事件类型定义

```typescript
// 步骤状态变更事件
interface StepStatusEvent {
  type: 'step-status'
  stepIndex: number      // 步骤索引（0-based）
  stepName: string       // 步骤名称，如 "拉取主订阅配置"
  status: 'RUNNING' | 'SUCCESS' | 'FAILED' | 'SKIPPED'
  timestamp: number      // 时间戳
}

// 构建完成事件
interface BuildCompleteEvent {
  type: 'build-complete'
  status: 'SUCCESS' | 'FAILED'
  duration: number       // 耗时（毫秒）
  totalSteps: number     // 总步骤数
  successSteps: number   // 成功步骤数
  failedSteps: number    // 失败步骤数
  errorMessage?: string  // 失败时的错误信息
}

// 构建错误事件
interface BuildErrorEvent {
  type: 'build-error'
  message: string
}
```

#### SSE 事件流示例

```
event: step-status
data: {"type":"step-status","stepIndex":0,"stepName":"拉取主订阅配置","status":"RUNNING","timestamp":1717833600000}

event: step-status
data: {"type":"step-status","stepIndex":0,"stepName":"拉取主订阅配置","status":"SUCCESS","timestamp":1717833602000}

event: step-status
data: {"type":"step-status","stepIndex":1,"stepName":"合并额外订阅节点","status":"RUNNING","timestamp":1717833602000}

event: step-status
data: {"type":"step-status","stepIndex":1,"stepName":"合并额外订阅节点","status":"SUCCESS","timestamp":1717833603000}

event: build-complete
data: {"type":"build-complete","status":"SUCCESS","duration":3500,"totalSteps":4,"successSteps":4,"failedSteps":0}
```

#### 前端事件处理逻辑

```typescript
const eventSource = new EventSource(`/api/build-records/${recordId}/progress`)

eventSource.addEventListener('step-status', (e) => {
  const data = JSON.parse(e.data)
  steps[data.stepIndex].status = data.status
})

eventSource.addEventListener('build-complete', (e) => {
  const data = JSON.parse(e.data)
  buildResult = data
  eventSource.close()
})

eventSource.addEventListener('build-error', (e) => {
  const data = JSON.parse(e.data)
  errorMessage = data.message
  eventSource.close()
})
```

### 4.3 弹窗组件设计

#### 组件结构

```vue
<!-- BuildProgressModal.vue -->
<template>
  <el-dialog
    v-model="visible"
    title="构建进度"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="false"
    width="500px"
    class="build-progress-modal"
  >
    <!-- 步骤条 -->
    <div class="steps-container">
      <el-steps direction="vertical" :active="activeStep" finish-status="success">
        <el-step
          v-for="(step, index) in steps"
          :key="index"
          :title="step.name"
          :status="getStepStatus(step.status)"
        >
          <template #description>
            <span v-if="step.status === 'RUNNING'" class="running-text">
              执行中...
            </span>
            <span v-else-if="step.status === 'FAILED'" class="failed-text">
              {{ step.errorMessage || '执行失败' }}
            </span>
          </template>
        </el-step>
      </el-steps>
    </div>

    <!-- 构建结果摘要 -->
    <div v-if="isCompleted" class="result-summary">
      <el-result
        :icon="resultIcon"
        :title="resultTitle"
        :sub-title="resultSubTitle"
      >
        <template #extra>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="总耗时">{{ formatDuration(result.duration) }}</el-descriptions-item>
            <el-descriptions-item label="总步骤">{{ result.totalSteps }}</el-descriptions-item>
            <el-descriptions-item label="成功">{{ result.successSteps }}</el-descriptions-item>
            <el-descriptions-item label="失败">{{ result.failedSteps }}</el-descriptions-item>
          </el-descriptions>
        </template>
      </el-result>
    </div>

    <!-- 底部按钮 -->
    <template #footer>
      <el-button v-if="!isCompleted" @click="handleCancel">取消</el-button>
      <el-button v-if="isCompleted" type="primary" @click="handleConfirm">确定</el-button>
    </template>
  </el-dialog>
</template>
```

#### 状态映射

| BuildStep.status | el-step status | 说明 |
|------------------|----------------|------|
| `WAITING` | `wait` | 等待执行 |
| `RUNNING` | `process` | 执行中（带动画） |
| `SUCCESS` | `finish` | 成功 |
| `FAILED` | `error` | 失败 |
| `SKIPPED` | `success` | 跳过 |

#### 交互流程

```
点击构建按钮
    ↓
显示弹窗（步骤条全部 wait 状态）
    ↓
订阅 SSE，实时更新步骤状态
    ↓
构建完成 → 显示结果摘要
    ↓
用户点击"确定" → 关闭弹窗，刷新列表
```

#### 取消/关闭行为

- **构建进行中**：点击"取消" → 关闭弹窗，构建继续后台运行
- **构建完成**：点击"确定" → 关闭弹窗，刷新构建记录列表
- **点击遮罩/按 ESC**：禁用（防止误操作）

### 4.4 后端实现设计

#### 接口变更

**BuildPipelineController.java**：

```java
@PostMapping("/{id}/execute")
public Map<String, String> execute(@PathVariable Long id) {
    BuildRecord record = buildPipelineService.execute(id);
    return Map.of("recordId", record.getId().toString());
}
```

**BuildRecordController.java**（新增）：

```java
@GetMapping("/{id}/progress")
public SseEmitter progress(@PathVariable Long id) {
    return buildRecordService.subscribeProgress(id);
}
```

#### 异步执行 + 进度推送

**BuildPipelineServiceImpl.java**：

```java
@Async
public BuildRecord execute(Long pipelineId) {
    BuildRecord record = createRecord(pipelineId, BuildStatus.RUNNING);
    List<BuildStep> steps = createSteps(record);
    
    publishEvent(record, steps);
    
    for (BuildStep step : steps) {
        step.setStatus(BuildStatus.RUNNING);
        publishEvent(record, step);
        
        try {
            executeStep(step);
            step.setStatus(BuildStatus.SUCCESS);
        } catch (Exception e) {
            step.setStatus(BuildStatus.FAILED);
            step.setErrorMessage(e.getMessage());
            record.setStatus(BuildStatus.FAILED);
        }
        
        publishEvent(record, step);
    }
    
    record.setStatus(BuildStatus.SUCCESS);
    record.setFinishedAt(LocalDateTime.now());
    publishCompleteEvent(record);
    
    return record;
}
```

#### SSE 订阅管理

**BuildRecordServiceImpl.java**：

```java
private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

public SseEmitter subscribeProgress(Long recordId) {
    SseEmitter emitter = new SseEmitter(0L);
    
    emitters.computeIfAbsent(recordId, k -> new CopyOnWriteArrayList<>()).add(emitter);
    
    emitter.onCompletion(() -> removeEmitter(recordId, emitter));
    emitter.onTimeout(() -> removeEmitter(recordId, emitter));
    emitter.onError(e -> removeEmitter(recordId, emitter));
    
    return emitter;
}

public void publishEvent(Long recordId, Object event) {
    List<SseEmitter> emitters = this.emitters.get(recordId);
    if (emitters != null) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                    .name(event.getClass().getSimpleName())
                    .data(event));
            } catch (IOException e) {
                removeEmitter(recordId, emitter);
            }
        }
    }
}
```

#### 数据库变更

**无需变更**。现有模型已支持所有必要字段。

### 4.5 前端实现设计

#### API 层变更

**build-pipeline.ts**：

```typescript
export interface ExecuteResponse {
  recordId: number
}

export function subscribeProgress(recordId: number): EventSource {
  return new EventSource(`/api/build-records/${recordId}/progress`)
}
```

#### BuildPipelineView.vue 变更

```typescript
const handleExecute = async (pipeline: TreeRow) => {
  try {
    const res = await buildPipelineApi.execute(pipeline.id)
    const recordId = res.data.recordId
    
    currentRecordId.value = recordId
    showProgressModal.value = true
    
    await loadPipelines()
  } catch (error) {
    ElMessage.error('构建启动失败')
  }
}

const handleProgressClose = () => {
  showProgressModal.value = false
  currentRecordId.value = null
  loadPipelines()
}
```

#### BuildProgressModal.vue 核心逻辑

```typescript
const subscribe = () => {
  const eventSource = buildPipelineApi.subscribeProgress(props.recordId)
  
  eventSource.addEventListener('step-status', (e) => {
    const data = JSON.parse(e.data) as StepStatusEvent
    steps.value[data.stepIndex].status = data.status
    activeStep.value = data.stepIndex
  })
  
  eventSource.addEventListener('build-complete', (e) => {
    const data = JSON.parse(e.data) as BuildCompleteEvent
    result.value = data
    isCompleted.value = true
    eventSource.close()
  })
  
  eventSource.addEventListener('build-error', (e) => {
    const data = JSON.parse(e.data) as BuildErrorEvent
    ElMessage.error(data.message)
    eventSource.close()
  })
  
  eventSource.onerror = () => {
    ElMessage.error('连接中断，请刷新页面查看结果')
    eventSource.close()
  }
}

onMounted(() => {
  if (props.recordId) {
    subscribe()
  }
})

onUnmounted(() => {
  eventSource?.close()
})
```

#### 步骤初始化

```typescript
const initSteps = (pipelineType: 'subscription' | 'config-profile') => {
  if (pipelineType === 'subscription') {
    steps.value = [
      { name: '拉取主订阅配置', status: 'WAITING' },
      { name: '合并额外订阅节点', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  } else {
    steps.value = [
      { name: '获取配置组合', status: 'WAITING' },
      { name: '生成完整配置', status: 'WAITING' },
      { name: '脚本处理', status: 'WAITING' },
      { name: '推送到 Mihomo', status: 'WAITING' }
    ]
  }
}
```

### 4.6 错误处理设计

#### 错误场景分类

| 场景 | 触发时机 | 处理方式 |
|------|----------|----------|
| 构建启动失败 | execute 接口返回错误 | 弹窗不显示，ElMessage 提示 |
| 步骤执行失败 | 单个步骤抛出异常 | 步骤标记为 FAILED，继续执行后续步骤 |
| 构建整体失败 | 关键步骤失败 | 推送 build-complete(status=FAILED) |
| SSE 连接中断 | 网络问题/服务重启 | 弹窗提示"连接中断"，建议刷新 |
| SSE 连接超时 | 长时间无事件 | 自动重连（3次），失败后提示 |

#### 前端错误处理

```typescript
eventSource.onerror = (e) => {
  if (reconnectCount < MAX_RECONNECT) {
    reconnectCount++
    console.warn(`SSE 连接中断，第 ${reconnectCount} 次重连...`)
    eventSource.close()
    setTimeout(subscribe, 1000 * reconnectCount)
  } else {
    ElMessage.error('连接中断，请刷新页面查看构建结果')
    eventSource.close()
  }
}

eventSource.addEventListener('build-complete', (e) => {
  const data = JSON.parse(e.data)
  if (data.status === 'FAILED') {
    ElMessage.error(`构建失败：${data.errorMessage || '未知错误'}`)
  }
})
```

#### 后端错误处理

```java
@Async
public BuildRecord execute(Long pipelineId) {
    BuildRecord record = createRecord(pipelineId, BuildStatus.RUNNING);
    
    try {
        executeSteps(record);
        record.setStatus(BuildStatus.SUCCESS);
    } catch (Exception e) {
        record.setStatus(BuildStatus.FAILED);
        record.setErrorMessage(e.getMessage());
        publishEvent(record.getId(), new BuildErrorEvent(e.getMessage()));
    } finally {
        record.setFinishedAt(LocalDateTime.now());
        publishCompleteEvent(record);
    }
    
    return record;
}
```

#### 资源清理

```typescript
onUnmounted(() => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
})

const handleClose = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
  emit('close')
}
```

### 4.7 测试策略设计

#### 后端测试

**单元测试**：
- `BuildPipelineServiceImplTest`：测试异步执行逻辑
- `BuildRecordServiceImplTest`：测试 SSE 订阅和事件推送

**集成测试**：
- 测试完整构建流程：execute → subscribe → receive events
- 测试并发场景：多个用户同时构建同一 pipeline

#### 前端测试

**组件测试**：
- `BuildProgressModal.spec.ts`：
  - 测试步骤状态渲染
  - 测试 SSE 事件处理
  - 测试完成状态显示
  - 测试取消/确定按钮交互

**E2E 测试**：
- 测试完整流程：点击构建 → 弹窗显示 → 进度更新 → 完成 → 关闭

#### 测试用例示例

```typescript
describe('BuildProgressModal', () => {
  it('should render steps with initial WAITING status', () => {
    // ...
  })
  
  it('should update step status on SSE event', () => {
    // 模拟 SSE 事件
    // 验证步骤状态更新
  })
  
  it('should show result summary on build-complete', () => {
    // 模拟完成事件
    // 验证摘要显示
  })
  
  it('should close modal on confirm click', () => {
    // 点击确定
    // 验证弹窗关闭
  })
})
```

---

## 5. 决策记录

| 决策点 | 选择 | 理由 |
|--------|------|------|
| 进度展示形式 | 步骤条 | 复用现有 BuildStep 模型，直观清晰 |
| 进度推送机制 | SSE | 单向推送，浏览器原生支持，实现简单 |
| 完成交互 | 状态摘要 + 确定按钮 | 简洁明了，信息充分 |
| 边界处理 | 后台继续 | 不阻塞用户操作，可从记录列表查看 |
| 执行模式 | 异步 | 支持实时进度推送 |

---

## 6. 风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| SSE 连接中断 | 用户无法看到进度 | 自动重连 + 提示刷新 |
| 后端异步化改造 | 可能影响现有定时任务 | 保持定时任务接口不变，只改手动触发 |
| 并发构建 | 资源竞争 | 使用 ConcurrentHashMap 管理订阅 |
| 长时间构建 | SSE 连接超时 | 设置合理的超时时间 + 心跳机制 |
