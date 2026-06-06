import api from './index'

export interface RuleProxyObject {
  id: string
  sourceName: string
  description?: string
}

export interface RuleGroup {
  id: string
  name: string
  description?: string
  sourceSubscriptionId?: string
  rules: string[]
  proxyObjects: RuleProxyObject[]
  createdAt?: string
  updatedAt?: string
}

export interface ParsedRule {
  index: number
  type: string
  match: string
  proxyDisplay: string
  proxyObjectId: string | null
}

export const ruleGroupApi = {
  list() {
    return api.get<RuleGroup[]>('/rule-groups')
  },

  get(id: string) {
    return api.get<RuleGroup>(`/rule-groups/${id}`)
  },

  create(data: Partial<RuleGroup>) {
    return api.post<RuleGroup>('/rule-groups', data)
  },

  update(id: string, data: Partial<RuleGroup>) {
    return api.put<RuleGroup>(`/rule-groups/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/rule-groups/${id}`)
  },

  extract(subscriptionId: string) {
    return api.post<RuleGroup>('/rule-groups/extract', { subscriptionId })
  },
}

/**
 * 解析规则字符串为结构化对象
 * 格式：类型,参数,代理名 或 类型,代理名（MATCH 等）
 */
export function parseRule(rule: string, proxyObjects: RuleProxyObject[]): ParsedRule {
  const parts = rule.split(',')
  const type = parts[0]?.trim() || ''

  if (parts.length >= 3) {
    const match = parts[1]?.trim() || ''
    const proxyPart = parts[2]?.trim() || ''
    const proxyObjId = extractProxyObjectId(proxyPart)
    let proxyDisplay = proxyPart
    if (proxyObjId) {
      const obj = proxyObjects.find(p => p.id === proxyObjId)
      proxyDisplay = obj ? `${obj.sourceName} (${proxyObjId})` : proxyPart
    }
    return { index: 0, type, match, proxyDisplay, proxyObjectId: proxyObjId }
  } else if (parts.length === 2) {
    const proxyPart = parts[1]?.trim() || ''
    const proxyObjId = extractProxyObjectId(proxyPart)
    let proxyDisplay = proxyPart
    if (proxyObjId) {
      const obj = proxyObjects.find(p => p.id === proxyObjId)
      proxyDisplay = obj ? `${obj.sourceName} (${proxyObjId})` : proxyPart
    }
    return { index: 0, type, match: '', proxyDisplay, proxyObjectId: proxyObjId }
  }
  return { index: 0, type, match: '', proxyDisplay: '', proxyObjectId: null }
}

/**
 * 从占位符 {{id}} 中提取代理对象 ID
 */
function extractProxyObjectId(value: string): string | null {
  const match = value.match(/^\{\{(.+?)\}\}$/)
  return match ? match[1] : null
}

/**
 * 将代理对象 ID 包装为占位符
 */
export function wrapProxyObjectId(id: string): string {
  return `{{${id}}}`
}
