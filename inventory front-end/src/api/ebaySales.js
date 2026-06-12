import { apiPost } from '@/api/request'

export async function uploadEbaySales(file) {
  const form = new FormData()
  form.append('file', file)
  return apiPost('/api/ebay-sales/upload', form)
}
