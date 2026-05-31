import { reactive } from 'vue'

export const authSession = reactive({
  initialized: false,
  authenticated: false,
  username: '',
})

export function updateAuthSession(status: { initialized: boolean; authenticated: boolean; username?: string }) {
  authSession.initialized = status.initialized
  authSession.authenticated = status.authenticated
  authSession.username = status.username || ''
}

export function clearAuthSession() {
  authSession.authenticated = false
  authSession.username = ''
}
