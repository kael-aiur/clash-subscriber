import api from './index'

export interface ScriptData {
  name: string
  content: string
}

export interface TryRunResult {
  success: boolean
  summary?: {
    proxiesBefore: number
    proxiesAfter: number
    groupsBefore: number
    groupsAfter: number
    rulesBefore: number
    rulesAfter: number
  }
  config?: Record<string, unknown>
  error?: string
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

  tryRun(scriptContent: string, subscriptionId: string) {
    return api.post<TryRunResult>('/scripts/try-run', { scriptContent, subscriptionId })
  },
}
