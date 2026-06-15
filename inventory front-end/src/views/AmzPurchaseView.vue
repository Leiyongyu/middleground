<script setup>
import { computed, h, ref } from 'vue'
import {
  NButton, NCard, NCheckbox, NDataTable, NDrawer, NDrawerContent,
  NDropdown, NInput, NPopover, NSpace, NTag, useMessage,
} from 'naive-ui'

const message = useMessage()



const loading = ref(false)
const rows = ref(dummyData)
const searchKeyword = ref('')

// ===== 列排序 =====
const sortField = ref('')
const sortOrder = ref('')
const NUMERIC_FIELDS = new Set(['planQty','fbaStock','salesForecast','purchaseCycle'])

// ===== 列筛选 =====
const filterField = ref('')
const activeFilters = ref([])
const filterInputVal = ref('')
const showFilter = ref(false)
const filterX = ref(0)
const filterY = ref(0)

const distinctFilterValues = computed(() => {
  if (!filterField.value) return []
  const set = new Set()
  for (const row of rows.value) {
    const v = row[filterField.value]
    if (v != null && String(v).trim()) set.add(String(v).trim())
  }
  return [...set].sort().slice(0, 50)
})

function openFilter(key, e) {
  filterField.value = key; filterInputVal.value = ''
  const exist = activeFilters.value.find(f => f.field === key)
  if (exist) filterInputVal.value = exist.value
  const rect = e.currentTarget.getBoundingClientRect()
  filterX.value = rect.left; filterY.value = rect.bottom + 4; showFilter.value = true
}
function applyFilter() {
  const field = filterField.value; const val = filterInputVal.value.trim()
  activeFilters.value = activeFilters.value.filter(f => f.field !== field)
  if (val) activeFilters.value.push({ field, value: val, display: (columnMap[field] || field) + ':' + val })
  showFilter.value = false
}
function clearFilter() { activeFilters.value = activeFilters.value.filter(f => f.field !== filterField.value); filterInputVal.value = ''; showFilter.value = false }
function clearAllFilters() { activeFilters.value = []; filterField.value = '' }

function handleSortClick(key) {
  if (!NUMERIC_FIELDS.has(key)) return
  if (sortField.value !== key) { sortField.value = key; sortOrder.value = 'desc' }
  else if (sortOrder.value === 'desc') { sortOrder.value = 'asc' }
  else if (sortOrder.value === 'asc') { sortField.value = ''; sortOrder.value = '' }
  else { sortField.value = key; sortOrder.value = 'desc' }
}

const filteredRows = computed(() => {
  let result = rows.value
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim().toLowerCase()
    result = result.filter(r => r.sku.toLowerCase().includes(kw) || r.productName.toLowerCase().includes(kw))
  }
  if (sortField.value) {
    const f = sortField.value; const asc = sortOrder.value === 'asc'
    result = [...result].sort((a, b) => {
      const va = Number(a?.[f]) || 0; const vb = Number(b?.[f]) || 0
      return asc ? va - vb : vb - va
    })
  }
  return result
})

function renderFilterIcon(key) {
  const isActive = activeFilters.value.some(f => f.field === key)
  const color = isActive ? '#1677ff' : '#bfbfbf'
  return h('svg', { viewBox: '0 0 1024 1024', width: '12', height: '12', style: { color, marginLeft: '0', cursor: 'pointer', flexShrink: 0 }, onClick: (e) => { e.stopPropagation(); openFilter(key, e) } },
    [h('path', { fill: 'currentColor', d: 'M911.2 128H112.8c-52.8 0-80 58.4-41.6 98.4L384 576v288c0 17.6 14.4 32 32 32h192c17.6 0 32-14.4 32-32V576l312.8-349.6c38.4-40 11.2-98.4-41.6-98.4z' })])
}
function renderSortIcon(key) {
  if (!NUMERIC_FIELDS.has(key)) return ''
  const isActive = sortField.value === key
  const color = isActive && sortOrder.value ? '#1677ff' : '#bfbfbf'
  const triangle = isActive && sortOrder.value === 'asc'
    ? 'M863.764 270.458c32.518-35.975 85.239-35.975 117.757 0l401.018 443.652c32.519 35.977 19.57 65.14-28.913 65.14h-861.969c-48.485 0-61.43-29.164-28.913-65.14l401.018-443.652z'
    : 'M855.721 739.856c30.486 33.728 79.913 33.728 110.397 0l375.954-415.921c30.487-33.729 18.347-61.069-27.103-61.069h-808.099c-45.455 0-57.591 27.341-27.106 61.068l375.955 415.923z'
  return h('svg', { viewBox: '0 0 1824 1024', width: '12', height: '12', style: { color, marginLeft: '0', cursor: 'pointer', flexShrink: 0, verticalAlign: 'middle' }, onClick: (e) => { e.stopPropagation(); handleSortClick(key) } },
    [h('path', { fill: 'currentColor', d: triangle })])
}

// ===== 列定义 =====
const statusMap = { '待审批': 'warning', '已审批': 'success', '已驳回': 'error' }
const columns = [
  { type: 'selection', multiple: true, width: 40, fixed: 'left' },
  { title: 'SKU', key: 'sku', width: 150, fixed: 'left', ellipsis: { tooltip: true } },
  { title: '产品名称', key: 'productName', width: 180, ellipsis: { tooltip: true } },
  { title: '站点', key: 'site', width: 80 },
  { title: '仓库', key: 'warehouse', width: 140 },
  { title: '计划采购量', key: 'planQty', width: 110 },
  { title: 'FBA在库', key: 'fbaStock', width: 100 },
  { title: '销量预测/月', key: 'salesForecast', width: 120 },
  { title: '采购周期(天)', key: 'purchaseCycle', width: 120 },
  { title: '供应商', key: 'supplier', width: 110 },
  { title: '状态', key: 'status', width: 90,
    render: (row) => h(NTag, { size: 'small', type: statusMap[row.status] || 'default', bordered: false }, { default: () => row.status }) },
  { title: '创建人', key: 'creator', width: 80 },
  { title: '提交时间', key: 'submitTime', width: 120 },
  { title: '备注', key: 'remark', width: 120, ellipsis: { tooltip: true } },
].map(c => ({ ...c, resizable: false, minWidth: 60 }))

const columnMap = columns.reduce((m, c) => { if (c.key) m[c.key] = c.title; return m }, {})

columns.forEach(c => {
  if (!c.key) return
  const needSort = NUMERIC_FIELDS.has(c.key)
  const origTitle = c.title
  c.title = () => h('span', { style: { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '10px' } },
    [renderFilterIcon(c.key), origTitle, needSort ? renderSortIcon(c.key) : null].filter(Boolean))
})

// ===== 列配置 =====
const showDrawer = ref(false)
const visibleKeys = ref(columns.filter(c => c.key).map(c => c.key))
const colByKey = columns.reduce((m, c) => { if (c.key) m[c.key] = c; return m }, {})
const selectionColumn = columns[0]
const visibleColumns = computed(() => {
  const ordered = visibleKeys.value.map(k => colByKey[k]).filter(Boolean)
  return [selectionColumn, ...ordered]
})
const visibleScrollX = computed(() => visibleColumns.value.reduce((s, c) => s + (Number(c?.width) || 100), 0))
const leftCols = computed(() =>
  columns.filter(c => c.key).map(c => ({ key: c.key, title: columnMap[c.key], checked: visibleKeys.value.includes(c.key) }))
)
const isAllChecked = computed(() => leftCols.value.every(c => c.checked))
function toggleAll() { if (isAllChecked.value) visibleKeys.value = ['sku', 'productName']; else visibleKeys.value = columns.filter(c => c.key).map(c => c.key) }
function toggleColumn(key) { const idx = visibleKeys.value.indexOf(key); if (idx >= 0) visibleKeys.value.splice(idx, 1); else visibleKeys.value.push(key) }

function handleRefresh() { loading.value = true; setTimeout(() => loading.value = false, 500) }
const checkedRowKeys = ref([])
</script>

<template>
  <div class="dashboard-page">
    <div class="table-card-wrap">
      <NCard title="Amazon 采购管理" size="large">
        <template #header-extra>
          <NSpace align="center" size="small">
            <NTag size="small" :bordered="false" type="default">共 {{ rows.length }} 条</NTag>
            <NInput v-model:value="searchKeyword" placeholder="搜索 SKU" clearable size="small" style="width: 180px" />
            <NButton size="small" type="primary">创建采购计划</NButton>
            <NButton size="small" secondary :loading="loading" @click="handleRefresh">刷新</NButton>
            <NDropdown trigger="click" :options="[{ label: '导出 Excel', key: 'export' }]">
              <NButton size="small" type="info">导入导出</NButton>
            </NDropdown>
            <NButton size="small" round @click="showDrawer = true" title="列设置">
              <template #icon>
                <svg viewBox="0 0 1024 1024" width="14" height="14"><path fill="currentColor" d="M512 938.666667a32.032 32.032 0 0 1-15.648-4.085334l-352-197.333333A32 32 0 0 1 128 709.333333v-394.666666a32 32 0 0 1 16.352-27.914667l352-197.333333a32 32 0 0 1 31.296 0l352 197.333333A32 32 0 0 1 896 314.666667v394.666666a32 32 0 0 1-16.352 27.914667l-352 197.333333A32 32 0 0 1 512 938.666667zM192 690.581333L512 869.973333l320-179.392V333.408L512 154.016 192 333.408v357.173333zM512 682.666667c-94.101333 0-170.666667-76.565333-170.666667-170.666667S417.898667 341.333333 512 341.333333s170.666667 76.565333 170.666667 170.666667-76.565333 170.666667-170.666667 170.666667z m0-277.333334c-58.816 0-106.666667 47.850667-106.666667 106.666667s47.850667 106.666667 106.666667 106.666667 106.666667-47.850667 106.666667-106.666667S570.816 405.333333 512 405.333333z"/></svg>
              </template>
            </NButton>
          </NSpace>
        </template>

        <NDataTable
          :loading="loading"
          :columns="visibleColumns"
          :data="filteredRows"
          :bordered="false"
          :scroll-x="visibleScrollX"
          :max-height="680"
          :row-key="(row) => row.sku + '|' + row.site"
          v-model:checked-row-keys="checkedRowKeys"
          :pagination="{ pageSize: 100, pageSizes: [20, 50, 100, 200] }"
          striped
        />
      </NCard>
    </div>

    <NDrawer v-model:show="showDrawer" :width="480" placement="right">
      <NDrawerContent title="列设置" closable>
        <div class="col-config-body">
          <div class="col-config-left">
            <NSpace vertical size="small">
              <NCheckbox :checked="isAllChecked" @update:checked="toggleAll">全选</NCheckbox>
              <NCheckbox v-for="c in leftCols" :key="c.key" :checked="c.checked" @update:checked="toggleColumn(c.key)">{{ c.title }}</NCheckbox>
            </NSpace>
          </div>
          <div class="col-config-right">
            <div class="col-config-right-title">已选字段</div>
            <div v-for="c in visibleColumns.filter(x => x.key)" :key="c.key" class="col-config-item">
              <span class="col-config-drag">⠿</span><span>{{ columnMap[c.key] }}</span>
            </div>
          </div>
        </div>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">取消</NButton>
            <NButton type="primary" @click="showDrawer = false">确定</NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <Teleport to="body">
      <div v-if="showFilter" class="filter-popover" :style="{ left: filterX + 'px', top: filterY + 'px' }" @click.stop>
        <div class="filter-popover-inner">
          <input :value="filterInputVal" class="filter-raw-input" placeholder="搜索关键词..." @input="filterInputVal = $event.target.value" @keyup.enter="applyFilter()" />
          <div v-if="!filterInputVal && distinctFilterValues.length" class="filter-distinct-list">
            <div class="filter-distinct-title">当前页可选值</div>
            <div v-for="v in distinctFilterValues" :key="v" class="filter-distinct-item">
              <NCheckbox size="small" :checked="false" /><span class="filter-item-label">{{ v }}</span>
            </div>
          </div>
          <NSpace size="small" style="margin-top: 8px" justify="end">
            <NButton size="tiny" @click="clearFilter">清除</NButton>
            <NButton size="tiny" type="primary" @click="applyFilter()">确定</NButton>
          </NSpace>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.dashboard-page { display: flex; flex-direction: column; gap: 8px; }
.table-card-wrap { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.table-card-wrap :deep(.n-card) { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.table-card-wrap :deep(.n-card__content) { flex: 1; min-height: 0; overflow: hidden; display: flex; flex-direction: column; padding: 8px 16px 12px; }
.table-card-wrap :deep(.n-card-header) { padding: 10px 16px 0; font-weight: 600; font-size: 15px; flex-shrink: 0; }
.table-card-wrap :deep(.n-data-table) { flex: 1; min-height: 0; }
:deep(.n-data-table-th) { background: #fafafa; font-weight: 600; font-size: 12px; color: rgba(0,0,0,0.55); text-align: center !important; }
:deep(.n-data-table-td) { font-size: 13px; border-bottom: 1px solid #f5f5f5; text-align: center !important; }
.col-config-body { display: flex; gap: 16px; min-height: 300px; }
.col-config-left { width: 160px; flex-shrink: 0; padding: 8px; border-right: 1px solid #eee; max-height: 400px; overflow-y: auto; }
.col-config-right { flex: 1; padding: 8px; max-height: 400px; overflow-y: auto; }
.col-config-right-title { font-size: 12px; color: #999; margin-bottom: 8px; }
.col-config-item { display: flex; align-items: center; gap: 6px; padding: 6px 10px; margin-bottom: 4px; background: #fafafa; border-radius: 4px; font-size: 13px; border: 1px solid #eee; }
.col-config-drag { color: #bbb; font-size: 14px; user-select: none; }
.filter-popover { position: fixed; z-index: 2000; }
.filter-popover-inner { background: #fff; border-radius: 8px; box-shadow: 0 6px 16px rgba(0,0,0,0.12); padding: 12px; min-width: 220px; max-width: 300px; }
.filter-raw-input { width: 100%; box-sizing: border-box; height: 30px; padding: 0 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; outline: none; }
.filter-raw-input:focus { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,0.1); }
.filter-distinct-list { max-height: 180px; overflow-y: auto; margin-top: 8px; border-top: 1px solid #eee; padding-top: 4px; }
.filter-distinct-title { font-size: 11px; color: #999; padding: 4px 0; }
.filter-distinct-item { display: flex; align-items: center; gap: 6px; font-size: 13px; padding: 3px 8px; cursor: pointer; border-radius: 3px; }
.filter-distinct-item:hover { background: #e6f4ff; color: #1677ff; }
.filter-item-label { flex: 1; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
