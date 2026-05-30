import api from './index'

export interface BuildPipeline {
  id: string
  name: string
  primarySubscriptionId: string
  additionalSubscriptionIds: string[]
  scriptName?: string
  targetInstanceId: string
  cronExpression?: string
  enabled: boolean
  createdAt?: string
  updatedAt?: string
  lastRunAt?: string
  lastRunStatus?: 'SUCCESS' | 'FAILED' | 'RUNNING'
}

export interface BuildRecord {
  id: string
  buildPipelineId: string
  startedAt: string
  finishedAt?: string
  status: 'SUCCESS' | 'FAILED' | 'RUNNING'
  errorMessage?: string
  logs: string[]
}

export const buildPipelineApi = {
  list() {
    return api.get<BuildPipeline[]>('/build-pipelines')
  },

  create(data: Partial<BuildPipeline>) {
    return api.post<BuildPipeline>('/build-pipelines', data)
  },

  get(id: string) {
    return api.get<BuildPipeline>(`/build-pipelines/${id}`)
  },

  update(id: string, data: Partial<BuildPipeline>) {
    return api.put<BuildPipeline>(`/build-pipelines/${id}`, data)
  },

  delete(id: string) {
    return api.delete(`/build-pipelines/${id}`)
  },

  execute(id: string) {
    return api.post<BuildRecord>(`/build-pipelines/${id}/execute`)
  },

  getRecords(id: string) {
    return api.get<BuildRecord[]>(`/build-pipelines/${id}/records`)
  },
}

export const buildRecordApi = {
  get(id: string) {
    return api.get<BuildRecord>(`/build-records/${id}`)
  },
}
