import api from './index'

export interface ScriptData {
  name: string
  content: string
}

export const scriptApi = {
  list() {
    return api.get<string[]>('/scripts')
  },

  get(name: string) {
    return api.get<string>(`/scripts/${name}`)
  },

  save(data: ScriptData) {
    return api.post('/scripts', data)
  },

  delete(name: string) {
    return api.delete(`/scripts/${name}`)
  },
}
