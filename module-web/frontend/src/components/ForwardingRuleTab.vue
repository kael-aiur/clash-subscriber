<script setup lang="ts">
import { ref } from 'vue'
import { VueFlow, useVueFlow } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import dagre from 'dagre'
import { getForwardingPath } from '../api/mihomo'
import type { FlowNode, FlowEdge } from '../api/mihomo'

import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'

const props = defineProps<{
  instanceId: string
}>()

const domain = ref('')
const loading = ref(false)
const nodes = ref<FlowNode[]>([])
const edges = ref<FlowEdge[]>([])

const { fitView } = useVueFlow()

async function handleQuery() {
  if (!domain.value.trim()) return

  loading.value = true
  try {
    const { data } = await getForwardingPath(props.instanceId, domain.value.trim())
    // 使用 dagre 自动布局
    const layouted = applyDagreLayout(data.nodes, data.edges)
    nodes.value = layouted.nodes
    edges.value = layouted.edges
    setTimeout(() => fitView(), 100)
  } catch (error) {
    console.error('查询转发路径失败:', error)
  } finally {
    loading.value = false
  }
}

function applyDagreLayout(nodes: FlowNode[], edges: FlowEdge[]) {
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 100 })

  for (const node of nodes) {
    g.setNode(node.id, { width: 180, height: 40 })
  }
  for (const edge of edges) {
    g.setEdge(edge.source, edge.target)
  }

  dagre.layout(g)

  return {
    nodes: nodes.map(node => {
      const pos = g.node(node.id)
      return { ...node, position: { x: pos.x - 90, y: pos.y - 20 } }
    }),
    edges
  }
}
</script>

<template>
  <div class="forwarding-rule-tab">
    <div class="query-bar">
      <el-input
        v-model="domain"
        placeholder="请输入域名，如 google.com"
        clearable
        @keyup.enter="handleQuery"
        style="width: 400px"
      >
        <template #prepend>域名</template>
      </el-input>
      <el-button type="primary" @click="handleQuery" :loading="loading">
        查询转发路径
      </el-button>
    </div>

    <div class="flow-container" v-loading="loading">
      <VueFlow
        v-if="nodes.length > 0"
        :nodes="nodes"
        :edges="edges"
        :default-viewport="{ zoom: 0.8, x: 0, y: 0 }"
        fit-view-on-init
      >
        <Background />
        <Controls />
      </VueFlow>
      <el-empty v-else-if="!loading" description="请输入域名查询转发路径" />
    </div>
  </div>
</template>

<style scoped>
.forwarding-rule-tab {
  height: 600px;
  display: flex;
  flex-direction: column;
}
.query-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.flow-container {
  flex: 1;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
}
</style>
