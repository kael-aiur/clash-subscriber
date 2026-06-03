import api from './index'

export interface NodeTag {
  id: string
  name: string
  priority: number
  patterns: string[]
  createdAt?: string
  updatedAt?: string
}

export const nodeTagApi = {
  list() {
    return api.get<NodeTag[]>('/node-tags')
  },

  create(data: Partial<NodeTag>) {
    return api.post<NodeTag>('/node-tags', data)
  },

  get(id: string) {
    return api.get<NodeTag>(`/node-tags/${id}`)
  },

  update(id: string, data: Partial<NodeTag>) {
    return api.put<NodeTag>(`/node-tags/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/node-tags/${id}`)
  },
}
