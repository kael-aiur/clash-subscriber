import api from './index'

export interface ScriptData {
  name: string
  content: string
}

export interface ConfigSummary {
  nodeCount: number
  proxyGroupCount: number
  ruleCount: number
  nodeNames?: string[]
  proxyGroupNames?: string[]
}

export interface PreviewSubscriptionResult {
  summary: ConfigSummary
  yaml: string
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
  inputSummary?: ConfigSummary
  inputYaml?: string
  outputSummary?: ConfigSummary
  outputYaml?: string
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

  previewSubscription(subscriptionId: string) {
    return api.post<PreviewSubscriptionResult>('/scripts/preview-subscription', { subscriptionId })
  },
}
