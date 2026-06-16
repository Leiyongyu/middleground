import { apiGet, apiPost } from '@/api/request'

export function fetchAmzInventory(query) {
  return apiGet('/api/amz/inventory', query)
}

export function searchAmzInventory(body) {
  return apiPost('/api/amz/inventory/search', body)
}

export function fetchAmzDistinctValues(field, keyword) {
  return apiGet('/api/amz/inventory/distinct-values', { field, keyword })
}

export function saveAmzCategory(sid, sellerSku, category) {
  return apiPost('/api/amz/inventory/save-category', { sid, sellerSku, category })
}
