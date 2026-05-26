import { apiDelete, apiGet, apiPost, apiPut } from '@/api/request'

export function fetchBrandOwnersPage({ page, size, brandCode, ownerName }) {
  return apiGet('/api/brand-owners', { page, size, brandCode, ownerName })
}

export async function fetchBrandOwnerByBrandCode(brandCode) {
  if (!brandCode) {
    return null
  }

  const result = await fetchBrandOwnersPage({
    page: 1,
    size: 1,
    brandCode,
  })

  const records = result?.records

  if (!Array.isArray(records) || records.length === 0) {
    return null
  }

  return records[0] || null
}

export function createBrandOwner(payload) {
  return apiPost('/api/brand-owners', payload)
}

export function updateBrandOwner(id, payload) {
  return apiPut(`/api/brand-owners/${id}`, payload)
}

/** 查某个负责人的所有品牌 */
export async function fetchBrandsByOwner(ownerName) {
  if (!ownerName) return []
  const result = await fetchBrandOwnersPage({ page: 1, size: 500, ownerName })
  return result?.records || []
}

/** 删除品牌归属 */
export function deleteBrandOwner(id) {
  return apiDelete(`/api/brand-owners/${id}`)
}
