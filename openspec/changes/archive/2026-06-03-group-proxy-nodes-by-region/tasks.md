## 1. 基础设施：国旗映射表与类型定义

- [x] 1.1 在 `SubscriptionView.vue` 的 `<script setup>` 中添加 `FLAG_REGION_MAP` 常量（国旗 emoji → 中文地区名映射，约 20-30 个常见地区）
- [x] 1.2 添加 `RegionGroup` 接口定义（region、flag、nodes、count）

## 2. 分组与过滤逻辑

- [x] 2.1 添加 `proxySearchKeyword` ref 和 `filteredProxies` computed（按节点名称搜索过滤）
- [x] 2.2 添加 `regionGroups` computed 属性（从 filteredProxies 中按国旗 emoji 分组，无标识归入"其他"，按数量降序排列，"其他"排最后）

## 3. UI 模板改造

- [x] 3.1 在代理节点 tab 顶部添加搜索框（`el-input`，绑定 `proxySearchKeyword`）
- [x] 3.2 将现有 `el-table` 替换为 `el-collapse` + `el-collapse-item` 结构，每个 item 代表一个地区分组
- [x] 3.3 实现分组头部模板（国旗 emoji + 地区名 + 节点数量 tag）
- [x] 3.4 实现分组内容模板（`el-table` 展示节点详情，列保持不变）

## 4. 展开状态管理

- [x] 4.1 添加 `expandedRegions` ref（默认空数组，全部折叠）

## 5. 边缘情况处理

- [x] 5.1 处理代理节点列表为空时的空状态展示
- [x] 5.2 处理搜索无结果时的空状态提示
