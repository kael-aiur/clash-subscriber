# 脚本管理优化 Implementation Plan

> **For agentic workers:** Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** 将脚本编辑器从 textarea 替换为 Monaco Editor，并新增试运行功能。

**Architecture:** 前端用 Monaco Editor 替代 textarea 提供语法高亮和错误诊断；后端新增 try-run 端点，接收脚本内容和订阅源 ID，通过 ScriptEngine 执行并返回结果摘要。

**Tech Stack:** Vue 3, Monaco Editor, Element Plus, Spring Boot, GraalVM Polyglot

---

## Task 1: 后端 — ScriptController 注入 SubscriptionService

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java`

- [ ] **Step 1:** 修改 ScriptController 构造函数，增加 SubscriptionService 参数

```java
private final SubscriptionService subscriptionService;

public ScriptController(@Value("${data.path:data}") String dataPath,
                        SubscriptionService subscriptionService) {
    this.scriptsDir = Path.of(dataPath, "scripts");
    this.subscriptionService = subscriptionService;
}
```

- [ ] **Step 2:** 添加 import

```java
import site.kael.clash.subscription.service.SubscriptionService;
```

- [ ] **Step 3:** 更新测试文件 `ScriptControllerTest.java`，构造时传入 mock 的 SubscriptionService

```java
@BeforeEach
void setUp() {
    ScriptController controller = new ScriptController(tempDir.toString(), null);
    // ...
}
```

- [ ] **Step 4:** 运行测试验证

Run: `mvn test -pl module-web -Dtest=ScriptControllerTest`

- [ ] **Step 5:** 提交

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java
git add module-web/src/test/java/site/kael/clash/web/controller/ScriptControllerTest.java
git commit -m "refactor(script): ScriptController 注入 SubscriptionService"
```

---

## Task 2: 后端 — 新增 try-run 端点

**Files:**
- Modify: `module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java`

- [ ] **Step 1:** 在 ScriptController 中添加 try-run 端点

```java
/**
 * 试运行脚本：获取订阅源配置后执行脚本，返回结果摘要
 */
@PostMapping("/try-run")
public ResponseEntity<Map<String, Object>> tryRun(@RequestBody Map<String, String> body) {
    String scriptContent = body.get("scriptContent");
    String subscriptionId = body.get("subscriptionId");

    if (scriptContent == null || scriptContent.isBlank()) {
        throw new BusinessException(400, "脚本内容不能为空");
    }
    if (subscriptionId == null || subscriptionId.isBlank()) {
        throw new BusinessException(400, "请选择订阅源");
    }

    log.info("试运行脚本: subscriptionId={}", subscriptionId);

    try {
        ClashConfig config = subscriptionService.fetch(subscriptionId);
        int proxiesBefore = config.getProxies().size();
        int groupsBefore = config.getProxyGroups().size();
        int rulesBefore = config.getRules().size();

        ClashConfig result = scriptEngine.execute(scriptContent, config, "try-run");

        int proxiesAfter = result.getProxies().size();
        int groupsAfter = result.getProxyGroups().size();
        int rulesAfter = result.getRules().size();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("proxiesBefore", proxiesBefore);
        summary.put("proxiesAfter", proxiesAfter);
        summary.put("groupsBefore", groupsBefore);
        summary.put("groupsAfter", groupsAfter);
        summary.put("rulesBefore", rulesBefore);
        summary.put("rulesAfter", rulesAfter);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("summary", summary);
        response.put("config", result.getRaw());

        return ResponseEntity.ok(response);
    } catch (Exception e) {
        log.warn("试运行失败: {}", e.getMessage());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("error", e.getMessage());
        return ResponseEntity.ok(response);
    }
}
```

- [ ] **Step 2:** 添加必要的 import

```java
import site.kael.clash.common.model.ClashConfig;
import site.kael.clash.processor.engine.ScriptEngine;
import java.util.LinkedHashMap;
```

- [ ] **Step 3:** 注入 ScriptEngine（在构造函数中添加参数）

```java
private final ScriptEngine scriptEngine;

public ScriptController(@Value("${data.path:data}") String dataPath,
                        SubscriptionService subscriptionService,
                        ScriptEngine scriptEngine) {
    this.scriptsDir = Path.of(dataPath, "scripts");
    this.subscriptionService = subscriptionService;
    this.scriptEngine = scriptEngine;
}
```

- [ ] **Step 4:** 更新测试中的构造调用

```java
ScriptController controller = new ScriptController(tempDir.toString(), null, null);
```

- [ ] **Step 5:** 运行测试验证

Run: `mvn test -pl module-web -Dtest=ScriptControllerTest`

- [ ] **Step 6:** 提交

```bash
git add module-web/src/main/java/site/kael/clash/web/controller/ScriptController.java
git add module-web/src/test/java/site/kael/clash/web/controller/ScriptControllerTest.java
git commit -m "feat(script): 新增 POST /api/scripts/try-run 试运行端点"
```

---

## Task 3: 前端 — 安装 Monaco Editor

**Files:**
- Modify: `module-web/frontend/package.json`

- [ ] **Step 1:** 安装依赖

```bash
cd module-web/frontend && npm install monaco-editor
```

- [ ] **Step 2:** 验证安装

```bash
cd module-web/frontend && npm ls monaco-editor
```

- [ ] **Step 3:** 提交

```bash
git add module-web/frontend/package.json module-web/frontend/package-lock.json
git commit -m "deps: 添加 monaco-editor 依赖"
```

---

## Task 4: 前端 — script.ts 新增 tryRun API

**Files:**
- Modify: `module-web/frontend/src/api/script.ts`

- [ ] **Step 1:** 添加 TryRunResult 接口和 tryRun 方法

```typescript
export interface TryRunResult {
  success: boolean
  summary?: {
    proxiesBefore: number
    proxiesAfter: number
    groupsBefore: number
    groupsAfter: number
    rulesBefore: number
    rulesAfter: number
  }
  config?: Record<string, unknown>
  error?: string
}

// 在 scriptApi 对象中添加:
tryRun(scriptContent: string, subscriptionId: string) {
  return api.post<TryRunResult>('/scripts/try-run', { scriptContent, subscriptionId })
},
```

- [ ] **Step 2:** 提交

```bash
git add module-web/frontend/src/api/script.ts
git commit -m "feat(script): 新增 tryRun API 方法"
```

---

## Task 5: 前端 — ScriptView.vue 替换 textarea 为 Monaco Editor

**Files:**
- Modify: `module-web/frontend/src/views/ScriptView.vue`

- [ ] **Step 1:** 在 `<script setup>` 中导入 Monaco Editor

```typescript
import * as monaco from 'monaco-editor'
import { nextTick, watch } from 'vue'
```

- [ ] **Step 2:** 添加 Monaco Editor 相关的 ref 和初始化逻辑

```typescript
const editorContainer = ref<HTMLElement>()
let editor: monaco.editor.IStandaloneCodeEditor | null = null

const viewEditorContainer = ref<HTMLElement>()
let viewEditor: monaco.editor.IStandaloneCodeEditor | null = null

const initEditor = (container: HTMLElement, content: string, readOnly = false) => {
  return monaco.editor.create(container, {
    value: content,
    language: 'javascript',
    theme: 'vs',
    readOnly,
    minimap: { enabled: false },
    lineNumbers: 'on',
    bracketPairColorization: { enabled: true },
    automaticLayout: true,
    scrollBeyondLastLine: false,
    fontSize: 14,
    tabSize: 2,
  })
}
```

- [ ] **Step 3:** 监听 dialogVisible，初始化/销毁编辑器

```typescript
watch(dialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    if (editorContainer.value) {
      editor = initEditor(editorContainer.value, form.value.content)
    }
  } else {
    editor?.dispose()
    editor = null
  }
})

watch(viewDialogVisible, async (visible) => {
  if (visible) {
    await nextTick()
    if (viewEditorContainer.value) {
      viewEditor = initEditor(viewEditorContainer.value, viewContent.value, true)
    }
  } else {
    viewEditor?.dispose()
    viewEditor = null
  }
})
```

- [ ] **Step 4:** 修改 handleSubmit 从编辑器获取内容

```typescript
const handleSubmit = async () => {
  const content = editor?.getValue() ?? form.value.content
  if (!form.value.name || !content) {
    ElMessage.warning('请填写脚本名称和内容')
    return
  }
  try {
    await scriptApi.save({ name: form.value.name, content })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    await loadScripts()
  } catch {
    ElMessage.error('保存失败')
  }
}
```

- [ ] **Step 5:** 替换模板中的 textarea 为 Monaco 容器

编辑对话框中：
```html
<el-form-item label="内容" required>
  <div ref="editorContainer" style="height: 400px; border: 1px solid #dcdfe6; border-radius: 4px;"></div>
</el-form-item>
```

查看对话框中：
```html
<div ref="viewEditorContainer" style="height: 500px; border: 1px solid #dcdfe6; border-radius: 4px;"></div>
```

- [ ] **Step 6:** 添加格式化按钮

```typescript
const handleFormat = () => {
  editor?.getAction('editor.action.formatDocument')?.run()
}
```

模板中（在 footer 前）：
```html
<el-button @click="handleFormat">格式化</el-button>
```

- [ ] **Step 7:** 启用 JS 语法诊断

```typescript
monaco.languages.typescript.javascriptDefaults.setDiagnosticsOptions({
  noSemanticValidation: true,
  noSyntaxValidation: false,
})
```

- [ ] **Step 8:** 验证前端编译

```bash
cd module-web/frontend && npm run build
```

- [ ] **Step 9:** 提交

```bash
git add module-web/frontend/src/views/ScriptView.vue
git commit -m "feat(script): 替换 textarea 为 Monaco Editor，支持语法高亮和格式化"
```

---

## Task 6: 前端 — 试运行 UI

**Files:**
- Modify: `module-web/frontend/src/views/ScriptView.vue`

- [ ] **Step 1:** 添加试运行相关的 ref

```typescript
import { subscriptionApi } from '@/api/subscription'
import type { Subscription } from '@/api/subscription'
import type { TryRunResult } from '@/api/script'

const subscriptions = ref<Subscription[]>([])
const selectedSubId = ref('')
const tryRunLoading = ref(false)
const tryRunResult = ref<TryRunResult | null>(null)
```

- [ ] **Step 2:** 加载订阅源列表

```typescript
const loadSubscriptions = async () => {
  try {
    const res = await subscriptionApi.list()
    subscriptions.value = res.data
  } catch {
    ElMessage.error('加载订阅源列表失败')
  }
}

// 在 onMounted 中调用
onMounted(() => {
  loadScripts()
  loadSubscriptions()
})
```

- [ ] **Step 3:** 实现试运行方法

```typescript
const handleTryRun = async () => {
  const content = editor?.getValue() ?? form.value.content
  if (!content || !selectedSubId.value) return

  tryRunLoading.value = true
  tryRunResult.value = null
  try {
    const res = await scriptApi.tryRun(content, selectedSubId.value)
    tryRunResult.value = res.data
  } catch {
    ElMessage.error('试运行请求失败')
  } finally {
    tryRunLoading.value = false
  }
}
```

- [ ] **Step 4:** 在编辑对话框模板中添加订阅源选择和试运行按钮

在编辑器 div 下方、footer 上方：
```html
<el-form-item label="试运行">
  <div style="display: flex; gap: 12px; align-items: center;">
    <el-select v-model="selectedSubId" placeholder="选择订阅源" style="width: 240px;">
      <el-option
        v-for="sub in subscriptions"
        :key="sub.id"
        :label="sub.name"
        :value="sub.id"
      />
    </el-select>
    <el-button
      type="success"
      :loading="tryRunLoading"
      :disabled="!selectedSubId"
      @click="handleTryRun"
    >
      ▶ 试运行
    </el-button>
  </div>
</el-form-item>
```

- [ ] **Step 5:** 实现结果面板

```html
<el-form-item v-if="tryRunResult" label="运行结果">
  <div v-if="tryRunResult.success" style="width: 100%;">
    <el-alert type="success" :closable="false" show-icon>
      <template #title>
        执行成功 —
        代理节点: {{ tryRunResult.summary!.proxiesBefore }} → {{ tryRunResult.summary!.proxiesAfter }}，
        代理分组: {{ tryRunResult.summary!.groupsBefore }} → {{ tryRunResult.summary!.groupsAfter }}，
        规则: {{ tryRunResult.summary!.rulesBefore }} → {{ tryRunResult.summary!.rulesAfter }}
      </template>
    </el-alert>
    <el-collapse style="margin-top: 8px;">
      <el-collapse-item title="查看完整输出 config">
        <pre style="max-height: 300px; overflow: auto; font-size: 12px;">{{ JSON.stringify(tryRunResult.config, null, 2) }}</pre>
      </el-collapse-item>
    </el-collapse>
  </div>
  <div v-else style="width: 100%;">
    <el-alert type="error" :closable="false" show-icon>
      <template #title>执行失败</template>
      <template #default>
        <pre style="font-size: 12px; white-space: pre-wrap;">{{ tryRunResult.error }}</pre>
      </template>
    </el-alert>
  </div>
</el-form-item>
```

- [ ] **Step 6:** 打开编辑对话框时重置试运行状态

```typescript
const openEditDialog = async (name: string) => {
  dialogTitle.value = '编辑脚本'
  tryRunResult.value = null
  selectedSubId.value = ''
  try {
    const res = await scriptApi.get(name)
    form.value = { name, content: res.data }
    dialogVisible.value = true
  } catch {
    ElMessage.error('获取脚本内容失败')
  }
}

const openAddDialog = () => {
  dialogTitle.value = '添加脚本'
  form.value = { name: '', content: '' }
  tryRunResult.value = null
  selectedSubId.value = ''
  dialogVisible.value = true
}
```

- [ ] **Step 7:** 验证前端编译

```bash
cd module-web/frontend && npm run build
```

- [ ] **Step 8:** 提交

```bash
git add module-web/frontend/src/views/ScriptView.vue
git commit -m "feat(script): 新增试运行功能，支持选择订阅源执行脚本查看结果"
```
