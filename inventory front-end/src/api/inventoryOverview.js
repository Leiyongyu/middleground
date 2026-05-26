import { apiGet } from '@/api/request'

export function fetchInventoryOverview(query) {
  return apiGet('/api/inventory-overview', query)
}

export function fetchInventoryOverviewWarehouses() {
  return apiGet('/api/inventory-overview/warehouses')
}
