# URL 掩码功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为订阅管理页面的 URL 和 Mihomo 实例页面的 API 地址添加掩码功能，防止敏感信息泄露

**Architecture:** 创建可复用的 MaskableText 组件，使用眼睛图标切换显示/隐藏状态

**Tech Stack:** Vue 3, Element Plus, TypeScript

---

## 文件结构

- `module-web/frontend/src/components/MaskableText.vue` — 可复用的掩码文本组件（新建）
- `module-web/frontend/src/views/SubscriptionView.vue` — 订阅管理页面（修改）
- `module-web/frontend/src/views/MihomoInstanceView.vue` — Mihomo 实例页面（修改）

---

### Task 1: 创建 MaskableText 组件

**Files:**
- Create: `module-web/frontend/src/components/MaskableText.vue`

- [ ] **Step 1: 创建 MaskableText 组件**

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { View, Hide } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  text: string
  masked?: boolean
}>(), {
  masked: true
})

const isMasked = ref(props.masked)

const displayText = computed(() => {
  if (!isMasked.value) return props.text
  
  try {
    const url = new URL(props.text)
    // 显示协议、主机名和端口，隐藏路径和参数
    const host = url.port ? `${url.hostname}:${url.port}` : url.hostname
    return `${url.protocol}//${host}/...`
  } catch {
    // 非 URL 格式，显示前 10 个字符 + ...
    if (props.text.length > 10) {
      return props.text.substring(0, 10) + '...'
    }
    return props.text
  }
})

const toggle = () => {
  isMasked.value = !isMasked.value
}
</script>

<template>
  <div class="maskable-text">
    <span class="text" :title="isMasked ? '点击眼睛图标查看完整内容' : text">{{ displayText }}</span>
    <el-icon class="toggle-btn" @click="toggle">
      <Hide v-if="isMasked" />
      <View v-else />
    </el-icon>
  </div>
</template>

<style scoped>
.maskable-text {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: 100%;
}

.text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
  min-width: 0;
}

.toggle-btn {
  cursor: pointer;
  color: #909399;
  flex-shrink: 0;
}

.toggle-btn:hover {
  color: #409eff;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/components/MaskableText.vue
git commit -m "feat(frontend): 创建 MaskableText 掩码文本组件"
```

---

### Task 2: 在订阅管理页面使用 MaskableText

**Files:**
- Modify: `module-web/frontend/src/views/SubscriptionView.vue:134`

- [ ] **Step 1: 修改 SubscriptionView.vue**

在 `<script setup>` 中添加导入：

```typescript
import MaskableText from '@/components/MaskableText.vue'
```

将第 134 行：
```vue
<el-table-column prop="url" label="URL" min-width="300" show-overflow-tooltip />
```

替换为：
```vue
<el-table-column label="URL" min-width="300">
  <template #default="{ row }">
    <MaskableText :text="row.url" />
  </template>
</el-table-column>
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/views/SubscriptionView.vue
git commit -m "feat(frontend): 订阅管理页面 URL 添加掩码功能"
```

---

### Task 3: 在 Mihomo 实例页面使用 MaskableText

**Files:**
- Modify: `module-web/frontend/src/views/MihomoInstanceView.vue:184`

- [ ] **Step 1: 修改 MihomoInstanceView.vue**

在 `<script setup>` 中添加导入：

```typescript
import MaskableText from '@/components/MaskableText.vue'
```

将第 184 行：
```vue
<el-table-column prop="apiUrl" label="API 地址" min-width="250" show-overflow-tooltip />
```

替换为：
```vue
<el-table-column label="API 地址" min-width="250">
  <template #default="{ row }">
    <MaskableText :text="row.apiUrl" />
  </template>
</el-table-column>
```

- [ ] **Step 2: 提交**

```bash
git add module-web/frontend/src/views/MihomoInstanceView.vue
git commit -m "feat(frontend): Mihomo 实例页面 API 地址添加掩码功能"
```
