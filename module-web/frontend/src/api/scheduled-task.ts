import api from './index'

export interface ScheduledTask {
  id: string
  name: string
  pipelineId: string
  targetInstances: string[]
  cronExpression: string
  enabled: boolean
  lastRunAt?: string
  lastRunStatus?: 'SUCCESS' | 'FAILED' | 'RUNNING'
}

export interface PipelineConfig {
  id: string
  name: string
  steps: Array<{
    processor: string
    config: Record<string, unknown>
  }>
}

export const scheduledTaskApi = {
  list() {
    return api.get<ScheduledTask[]>('/scheduled-tasks')
  },

  create(data: Partial<ScheduledTask>) {
    return api.post<ScheduledTask>('/scheduled-tasks', data)
  },

  get(id: string) {
    return api.get<ScheduledTask>(`/scheduled-tasks/${id}`)
  },

  update(id: string, data: Partial<ScheduledTask>) {
    return api.put<ScheduledTask>(`/scheduled-tasks/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/scheduled-tasks/${id}`)
  },

  enable(id: string) {
    return api.post(`/scheduled-tasks/${id}/enable`)
  },

  disable(id: string) {
    return api.post(`/scheduled-tasks/${id}/disable`)
  },

  trigger(id: string) {
    return api.post(`/scheduled-tasks/${id}/trigger`)
  },
}

export const pipelineApi = {
  list() {
    return api.get<PipelineConfig[]>('/pipelines')
  },

  get(id: string) {
    return api.get<PipelineConfig>(`/pipelines/${id}`)
  },
}
