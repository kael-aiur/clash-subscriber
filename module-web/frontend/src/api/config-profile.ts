import api from './index'

export interface ConfigProfile {
  id?: string
  name: string
  description?: string
  subscriptionIds: string[]
  proxyGroups: ProxyGroupConfig[]
  ruleGroups: RuleGroupRef[]
  basicConfig: ClashBasicConfig
  authUsername?: string
  authPassword?: string
  createdAt?: string
  updatedAt?: string
}

export interface ProxyGroupConfig {
  name: string
  type: 'select' | 'url-test' | 'fallback' | 'load-balance'
  nodeNames: string[]
  matchKeywords: string[]
  excludeKeywords: string[]
  includeAll: boolean
  url?: string
  interval?: number
}

export interface RuleGroupRef {
  ruleGroupId: string
  priority: number
  proxyObjectMappings?: Record<string, string>
}

export interface ClashBasicConfig {
  mixedPort: number
  port: number
  socksPort: number
  redirPort: number
  allowLan: boolean
  mode: 'rule' | 'global' | 'direct'
  logLevel: string
  externalController: string
  secret?: string
}

export const configProfileApi = {
  list() {
    return api.get<ConfigProfile[]>('/config/list')
  },

  get(id: string) {
    return api.get<ConfigProfile>(`/config/detail/${id}`)
  },

  create(data: Partial<ConfigProfile>) {
    return api.post<ConfigProfile>('/config', data)
  },

  update(id: string, data: Partial<ConfigProfile>) {
    return api.put<ConfigProfile>(`/config/update/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/config/${id}`)
  },
}
