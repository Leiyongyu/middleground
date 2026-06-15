import { apiGet } from '@/api/request'

export function fetchAmzInventory(query) {
  return apiGet('/api/amz/inventory', query)
}
