import api from './index'

export interface AuthStatus {
  initialized: boolean
  authenticated: boolean
  username?: string
}

export interface SetupPayload {
  username: string
  password: string
  confirmPassword: string
}

export interface LoginPayload {
  username: string
  password: string
}

export const authApi = {
  async status(): Promise<AuthStatus> {
    const response = await api.get<AuthStatus>('/auth/status')
    return response.data
  },
  async setup(payload: SetupPayload): Promise<void> {
    await api.post('/auth/setup', payload)
  },
  async login(payload: LoginPayload): Promise<AuthStatus> {
    const response = await api.post<AuthStatus>('/auth/login', payload)
    return response.data
  },
  async logout(): Promise<void> {
    await api.post('/auth/logout')
  },
}
