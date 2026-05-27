import { apiDelete, apiGet, apiPost, apiPut } from '@/api/request'

export function fetchUsersPage({ page, size, account, role }) {
  return apiGet('/api/users', { page, size, account, role })
}

export function createUser(payload) {
  return apiPost('/api/users', payload)
}

export function updateUser(id, payload) {
  return apiPut(`/api/users/${id}`, payload)
}

export function deleteUser(id) {
  return apiDelete(`/api/users/${id}`)
}
