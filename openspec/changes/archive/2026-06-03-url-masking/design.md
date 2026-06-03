# 设计文档：URL 掩码功能

## 背景

项目中订阅链接和 Mihomo 实例的 API 地址是敏感信息，在截图或演示时容易泄露。需要添加掩码功能保护这些 URL。

## 目标

- 默认隐藏敏感 URL，防止意外泄露
- 提供便捷的查看方式（眼睛图标切换）
- 组件可复用，统一交互体验

## 设计方案

### 交互设计

- URL 默认显示掩码（截断显示域名部分，隐藏完整路径和参数）
- 右侧显示眼睛图标（Element Plus 的 `View` / `Hide` 图标）
- 点击图标切换显示/隐藏状态
- 每个 URL 独立管理显示状态

### 掩码规则

- 显示协议和域名，隐藏路径和参数
- 示例：`https://316.sub987.top/weibo/ipx/client/dy?token=xxx` → `https://316.sub987.top/...`
- 示例：`https://clash.kael.site:8444/aws/` → `https://clash.kael.site:8444/...`

### 组件设计

创建 `MaskableText` 组件：

```vue
<template>
  <div class="maskable-text">
    <span class="text">{{ displayText }}</span>
    <el-icon class="toggle-btn" @click="toggle">
      <View v-if="masked" />
      <Hide v-else />
    </el-icon>
  </div>
</template>
```

**Props：**
- `text: string` — 原始文本
- `masked: boolean` — 初始状态是否掩码（默认 true）

**功能：**
- `displayText` — 根据 masked 状态返回掩码文本或原始文本
- `toggle()` — 切换 masked 状态

### 使用场景

1. **SubscriptionView.vue** — URL 列
2. **MihomoInstanceView.vue** — API 地址列

## 验证标准

- 组件正确显示掩码文本
- 点击眼睛图标切换显示/隐藏
- 两个页面的 URL 都正确应用掩码功能
- 掩码规则正确截断 URL
