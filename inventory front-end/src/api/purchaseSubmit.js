import { apiDelete, apiGet, apiPost, apiPut } from '@/api/request'
import { httpRequest } from '@/api/httpClient'

/**
 * @typedef {import('@/api/types').PageQuery & { creator?: string, status?: string }} PurchaseSubmitQuery
 * @typedef {import('@/api/types').PageResult & { records: import('@/api/types').PurchaseSubmitRecord[] }} PurchaseSubmitPageResult
 */

/**
 * 分页查询采购计划提交记录
 * @param {PurchaseSubmitQuery} params
 * @returns {Promise<PurchaseSubmitPageResult>}
 */
export function fetchSubmitPage({ page, size, sku, creator, status }) {
  return apiGet('/api/purchase-plan-submit', { page, size, sku, creator, status })
}

/** @param {Array} items */
export function submitPlans(items) {
  return apiPost('/api/purchase-plan-submit', items)
}

/** @param {string} id */
export function deleteSubmit(id) {
  return apiDelete(`/api/purchase-plan-submit/${id}`)
}

/**
 * @param {string} id
 * @param {{ quantityPlan?: number, remark?: string }} data
 */
export function updateSubmit(id, data) {
  return apiPut(`/api/purchase-plan-submit/${id}`, data)
}

/**
 * @param {string[]} ids
 * @param {string} status
 */
export function batchUpdateStatus(ids, status) {
  return apiPut('/api/purchase-plan-submit/batch-status', { ids, status })
}

/**
 * 导出 Excel，传 ids 数组则只导出指定记录
 * @param {string[]|null} ids
 */
export async function exportExcel(ids) {
  let token = ''
  try {
    const session = JSON.parse(localStorage.getItem('inventory-auth-session'))
    token = session?.token || ''
  } catch { /* ignore */ }

  // 使用 httpRequest 发送 GET 请求获取 Blob
  const base = import.meta.env.VITE_API_BASE_URL || window.location.origin
  let url = `${base}/api/purchase-plan-submit/export`
  if (ids && ids.length > 0) url += '?ids=' + ids.join(',')
  const resp = await fetch(url, {
    headers: { Authorization: `Bearer ${token}` },
  })
  if (!resp.ok) throw new Error('导出失败')
  const blob = await resp.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = '采购计划导出.xlsx'
  a.click()
}
