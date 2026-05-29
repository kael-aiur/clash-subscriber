import api from './index'

export interface Subscription {
  id: string
  name: string
  url: string
  userAgent?: string
  headers?: Record<string, string>
  createdAt?: string
  updatedAt?: string
  lastFetchedAt?: string
}

export interface ClashConfig {
  name?: string
  raw?: Record<string, unknown>
  proxies?: unknown[]
  proxyGroups?: Record<string, unknown>
  rules?: string[]
}

export const subscriptionApi = {
  list() {
    return api.get<Subscription[]>('/subscriptions')
  },

  create(data: Partial<Subscription>) {
    return api.post<Subscription>('/subscriptions', data)
  },

  get(id: string) {
    return api.get<Subscription>(`/subscriptions/${id}`)
  },

  update(id: string, data: Partial<Subscription>) {
    return api.put<Subscription>(`/subscriptions/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/subscriptions/${id}`)
  },

  fetch(id: string) {
    return api.post<ClashConfig>(`/subscriptions/${id}/fetch`)
  },
}
