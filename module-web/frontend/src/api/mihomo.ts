import api from './index'
import type { ClashConfig } from './subscription'

export interface FlowNode {
  id: string
  type: 'domain' | 'rule' | 'proxyGroup' | 'proxy' | 'target'
  data: Record<string, any>
  position: { x: number; y: number }
}

export interface FlowEdge {
  id: string
  source: string
  target: string
}

export interface ForwardingPathResult {
  nodes: FlowNode[]
  edges: FlowEdge[]
}

export interface MihomoInstance {
  id: string
  name: string
  apiUrl: string
  apiSecret?: string
  enabled: boolean
  status: 'HEALTHY' | 'UNHEALTHY' | 'UNKNOWN'
  lastHealthCheck?: string
}

export const mihomoApi = {
  list() {
    return api.get<MihomoInstance[]>('/mihomo-instances')
  },

  create(data: Partial<MihomoInstance>) {
    return api.post<MihomoInstance>('/mihomo-instances', data)
  },

  get(id: string) {
    return api.get<MihomoInstance>(`/mihomo-instances/${id}`)
  },

  update(id: string, data: Partial<MihomoInstance>) {
    return api.put<MihomoInstance>(`/mihomo-instances/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/mihomo-instances/${id}`)
  },

  healthCheck(id: string) {
    return api.get<MihomoInstance>(`/mihomo-instances/${id}/health`)
  },

  healthCheckAll() {
    return api.get<MihomoInstance[]>('/mihomo-instances/health')
  },

  pushConfig(id: string, config: ClashConfig) {
    return api.post(`/mihomo-instances/${id}/push`, config)
  },

  pushConfigAll(config: ClashConfig) {
    return api.post('/mihomo-instances/push', config)
  },
}

/**
 * 查询域名的转发路径
 */
export function getForwardingPath(id: string, domain: string) {
  return api.get<ForwardingPathResult>(`/mihomo-instances/${id}/forwarding-path`, {
    params: { domain }
  })
}

/**
 * 获取实例当前配置
 */
export function getMihomoConfig(id: string) {
  return api.get<string>(`/mihomo-instances/${id}/config`)
}
