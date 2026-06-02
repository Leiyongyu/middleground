import { onMounted, reactive, ref } from 'vue'

/**
 * 通用数据表格 composable：封装分页、搜索、加载状态。
 *
 * @param {Function} fetchFn  - 分页查询函数，签名为 ({ page, size, ...filters }) => Promise<{ records, total }>
 * @param {object}   [initialFilters] - 初始筛选条件
 * @param {object}   [opts]
 * @param {number}   [opts.pageSize]    - 默认每页条数，默认 20
 * @param {boolean}  [opts.loadOnMount] - 是否 onMounted 时自动加载，默认 true
 * @returns {{ loading, records, total, query, filters, loadData, handleSearch, handleReset }}
 */
export function useDataTable(fetchFn, initialFilters = {}, opts = {}) {
  const { pageSize = 20, loadOnMount = true } = opts

  const loading = ref(false)
  const records = ref([])
  const total = ref(0)

  const query = reactive({ page: 1, size: pageSize })
  const filters = reactive({ ...initialFilters })

  async function loadData() {
    loading.value = true
    try {
      const params = { page: query.page, size: query.size }
      // 将所有非空 filter 值放入 params
      for (const [key, value] of Object.entries(filters)) {
        const trimmed = typeof value === 'string' ? value.trim() : value
        if (trimmed !== '' && trimmed != null && trimmed !== undefined) {
          params[key] = typeof trimmed === 'string' ? trimmed : value
        }
      }

      const result = await fetchFn(params)
      records.value = result?.records || []
      total.value = Number(result?.total || 0)
    } finally {
      loading.value = false
    }
  }

  function handleSearch() {
    query.page = 1
    loadData()
  }

  function handleReset() {
    for (const key of Object.keys(initialFilters)) {
      filters[key] = initialFilters[key]
    }
    query.page = 1
    loadData()
  }

  if (loadOnMount) {
    onMounted(() => loadData())
  }

  return { loading, records, total, query, filters, loadData, handleSearch, handleReset }
}
