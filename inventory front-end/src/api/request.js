import router from '@/router'
import { useAuthStore } from '@/stores/auth'
import { httpRequest } from '@/api/httpClient'

/**
 * 带认证的 API 请求封装：自动附加 Bearer token，处理 401 跳转和统一错误。
 */
export async function apiRequest({ path, method = 'GET', query, body, auth = true }) {
  const headers = {}

  if (auth) {
    const authStore = useAuthStore()
    const token = authStore.token
    const tokenType = authStore.tokenType || 'Bearer'

    if (!token) {
      await router.push('/login')
      throw new Error('请先登录')
    }
    headers.Authorization = `${tokenType} ${token}`
  }

  const { response, data } = await httpRequest({ path, method, query, body, headers })

  if (response.status === 401) {
    const authStore = useAuthStore()
    await authStore.logout()
    await router.push('/login')
    throw new Error(data?.message || '登录已失效，请重新登录')
  }

  if (!response.ok) {
    throw new Error(data?.message || `请求失败，状态码：${response.status}`)
  }

  if (data && typeof data === 'object' && 'code' in data) {
    const codeValue = typeof data.code === 'number' ? data.code : Number(data.code)
    if (codeValue !== 0 && codeValue !== 200) {
      throw new Error(data.message || '请求失败')
    }
    return data.data
  }

  return data
}

export function apiGet(path, query, options) {
  return apiRequest({ path, method: 'GET', query, ...options })
}

export function apiPost(path, body, options) {
  return apiRequest({ path, method: 'POST', body, ...options })
}

export function apiPut(path, body, options) {
  return apiRequest({ path, method: 'PUT', body, ...options })
}

export function apiDelete(path, options) {
  return apiRequest({ path, method: 'DELETE', ...options })
}
