import { apiPost } from '@/api/request'

export function syncAll() {
  return apiPost('/api/sync/all')
}
