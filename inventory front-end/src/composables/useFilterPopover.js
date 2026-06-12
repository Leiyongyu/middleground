import { computed, ref, watch } from 'vue'

/**
 * 可复用的筛选弹窗逻辑 — 消除 DashboardView 和 DailyPriceTrackingView 的重复代码。
 *
 * @param {{ fetchDistinctValues: (field: string, keyword: string) => Promise<string[]>,
 *            numericFields: Set<string>,
 *            columnNameMap: Record<string, string>,
 *            rowsRef: import('vue').Ref<Array<Record<string, any>>>,
 *            onFilterChange: () => void }} opts
 */
export function useFilterPopover({ fetchDistinctValues, numericFields, columnNameMap, rowsRef, onFilterChange }) {
  // ===== 状态 =====
  const filterField = ref('')
  const activeFilters = ref([])          // [{ field, value, display }]
  const filterInputVal = ref('')
  const filterNumOpVal = ref('>')
  const filterNumInputVal = ref('')
  const filterChecked = ref([])          // 多选复选框值
  const filterSearchResults = ref([])    // 远程搜索结果
  const filterRawRef = ref(null)
  let filterTimer = null
  const showFilter = ref(false)
  const filterX = ref(0)
  const filterY = ref(0)

  // ===== 远程搜索防抖 =====
  watch(filterInputVal, (val) => {
    if (!showFilter.value || !filterField.value) return
    clearTimeout(filterTimer)
    filterTimer = setTimeout(async () => {
      if (!val || !val.trim()) { filterSearchResults.value = []; return }
      try {
        filterSearchResults.value = await fetchDistinctValues(filterField.value, val.trim()) || []
      } catch { filterSearchResults.value = [] }
    }, 200)
  })

  // ===== 本地去重值 =====
  const distinctFilterValues = computed(() => {
    if (!filterField.value) return []
    const set = new Set()
    for (const row of (rowsRef.value || [])) {
      const v = row[filterField.value]
      if (v != null && String(v).trim()) set.add(String(v).trim())
    }
    return [...set].sort().slice(0, 50)
  })

  // ===== 操作 =====
  function openFilter(key, e) {
    filterField.value = key
    filterInputVal.value = ''
    filterNumOpVal.value = '>'
    filterNumInputVal.value = ''
    filterSearchResults.value = []
    const exist = activeFilters.value.find(f => f.field === key)
    filterChecked.value = exist && !numericFields.has(key)
      ? exist.value.split(',').filter(Boolean) : []
    if (exist) {
      if (numericFields.has(key)) {
        const m = exist.value.match(/^(>=|<=|>|<|=)(.+)$/)
        if (m) { filterNumOpVal.value = m[1]; filterNumInputVal.value = m[2] }
      } else {
        filterInputVal.value = exist.value
      }
    }
    const rect = e.currentTarget.getBoundingClientRect()
    filterX.value = rect.left
    filterY.value = rect.bottom + 4
    showFilter.value = true
  }

  function toggleFilterCheck(val) {
    const idx = filterChecked.value.indexOf(val)
    if (idx >= 0) filterChecked.value.splice(idx, 1)
    else filterChecked.value.push(val)
  }

  function applyFilter() {
    const field = filterField.value
    const rawVal = numericFields.has(field)
      ? (filterNumInputVal.value ? filterNumOpVal.value + filterNumInputVal.value.trim() : '')
      : filterInputVal.value
    const val = filterChecked.value.length ? filterChecked.value.join(',') : (rawVal && rawVal.trim() ? rawVal.trim() : '')
    activeFilters.value = activeFilters.value.filter(f => f.field !== field)
    if (val) {
      const title = columnNameMap[field] || field
      activeFilters.value.push({ field, value: val, display: title + ':' + val })
    }
    showFilter.value = false
    onFilterChange()
  }

  function clearFilter() {
    activeFilters.value = activeFilters.value.filter(f => f.field !== filterField.value)
    filterInputVal.value = ''
    filterNumOpVal.value = '>'
    filterNumInputVal.value = ''
    filterChecked.value = []
    showFilter.value = false
    onFilterChange()
  }

  function clearAllFilters() {
    activeFilters.value = []
    filterField.value = ''
    onFilterChange()
  }

  return {
    // state
    filterField, activeFilters, filterInputVal, filterNumOpVal, filterNumInputVal,
    filterChecked, filterSearchResults, filterRawRef, showFilter, filterX, filterY,
    // computed
    distinctFilterValues,
    // actions
    openFilter, toggleFilterCheck, applyFilter, clearFilter, clearAllFilters,
  }
}
