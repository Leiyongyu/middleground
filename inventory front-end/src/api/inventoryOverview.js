import { apiGet, apiPost } from '@/api/request'

export function fetchInventoryOverview(query) {
  return apiGet('/api/inventory-overview', query)
}

export function searchInventoryOverview(body) {
  return apiPost('/api/inventory-overview/search', body)
}

export function fetchDistinctValues(field, keyword) {
  return apiGet('/api/inventory-overview/distinct-values', { field, keyword })
}

/** 仅从DB重算快照，不拉外部接口 */
export function refreshSnapshot() {
  return apiPost('/api/inventory-overview/refresh-snapshot')
}
