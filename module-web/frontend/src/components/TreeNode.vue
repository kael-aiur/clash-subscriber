<script setup lang="ts">
interface TreeNode {
  name: string
  type: string
  members: string[]
  children: TreeNode[]
  isSpecial: boolean
}

defineProps<{
  node: TreeNode
  depth: number
  selected: string | null
}>()

const emit = defineEmits<{
  select: [name: string]
}>()

const groupTypeColor = (type: string): string => {
  const map: Record<string, string> = {
    'select': '#409EFF',
    'url-test': '#67C23A',
    'urltest': '#67C23A',
    'fallback': '#E6A23C',
    'load-balance': '#9B59B6',
    'special': '#909399',
  }
  return map[type?.toLowerCase()] || '#909399'
}
</script>

<template>
  <div class="tree-node-wrapper">
    <div
      class="tree-node"
      :class="{
        'tree-node--selected': selected === node.name && !node.isSpecial,
        'tree-node--direct': node.name === 'DIRECT',
        'tree-node--reject': node.name === 'REJECT',
        'tree-node--clickable': !node.isSpecial,
      }"
      @click="!node.isSpecial && emit('select', node.name)"
    >
      <span class="tree-node-name">{{ node.name }}</span>
      <el-tag
        :color="groupTypeColor(node.type)"
        effect="dark"
        size="small"
        style="color: #fff; border: none; margin-left: 6px;"
      >
        {{ node.type }}
      </el-tag>
      <span v-if="!node.isSpecial" class="tree-node-count">{{ node.members.length }}</span>
    </div>
    <div v-if="node.children.length > 0" class="tree-children">
      <div v-for="child in node.children" :key="child.name" class="tree-child-row">
        <div class="tree-line"></div>
        <TreeNode :node="child" :depth="depth + 1" :selected="selected" @select="(n: string) => emit('select', n)" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.tree-node-wrapper {
  display: flex;
  flex-direction: column;
}

.tree-node {
  display: inline-flex;
  align-items: center;
  padding: 6px 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fff;
  font-size: 13px;
  transition: all 0.2s;
  width: fit-content;
  max-width: 280px;
}

.tree-node--clickable {
  cursor: pointer;
}

.tree-node--clickable:hover {
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.15);
}

.tree-node--selected {
  border-color: #409eff;
  background: #ecf5ff;
}

.tree-node--direct {
  border-color: #67c23a;
  background: #f0f9eb;
}

.tree-node--reject {
  border-color: #f56c6c;
  background: #fef0f0;
}

.tree-node-name {
  font-weight: 500;
}

.tree-node-count {
  margin-left: 6px;
  font-size: 11px;
  color: #909399;
  background: #f0f2f5;
  border-radius: 10px;
  padding: 1px 6px;
}

.tree-children {
  margin-left: 24px;
  display: flex;
  flex-direction: column;
  gap: 4px;
  position: relative;
}

.tree-child-row {
  display: flex;
  align-items: flex-start;
  position: relative;
}

.tree-line {
  width: 20px;
  min-height: 20px;
  border-left: 2px solid #dcdfe6;
  border-bottom: 2px solid #dcdfe6;
  border-radius: 0 0 0 8px;
  margin-right: 4px;
  flex-shrink: 0;
  margin-top: 0;
}
</style>
