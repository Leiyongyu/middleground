import { apiGet, apiPost } from '@/api/request'

export function syncSales(payload) {
  return apiPost('/api/sales/sync', payload)
}

export function getSkuSalesSummary() {
  return apiGet('/api/sales/sku-summary')
}
