<script setup>
import { computed, h, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import {
  NButton, NCard, NCheckbox, NDataTable, NDrawer, NDrawerContent,
  NDropdown, NInput, NPopover, NSpace, NTag, useMessage,
} from 'naive-ui'
import { fetchAmzInventory, searchAmzInventory, fetchAmzDistinctValues, saveAmzCategory } from '@/api/amzInventory'
import { apiPost } from '@/api/request'
import { useColumnConfig } from '@/composables/useColumnConfig'

const message = useMessage()

const loading = ref(false)
const syncLoading = ref(false)
const rows = ref([])
const totalRecords = ref(0)
const query = reactive({ page: 1, size: 100 })

// ===== 服务端排序 =====
const sortField = ref('')
const sortOrder = ref('')
const NUMERIC_FIELDS = new Set([
  'rating','reviews','adRate','profitRate30','refundRate90','purchased',
  'domesticStock','lockNum','fbStock','fbaOnway','totalStock',
  'sales7','sales14','sales30','sales60','speed14','speed30','speed60',
  'safetyStock','avgMonthly','replenishQty','shipment',
])

async function loadData() {
  loading.value = true
  try {
    const body = { page: query.page, size: query.size }
    if (sortField.value) { body.sortField = sortField.value; body.sortOrder = sortOrder.value }
    if (activeFilters.value.length) {
      body.filters = activeFilters.value.map(f => ({ field: f.field, value: f.value }))
    }
    const res = await searchAmzInventory(body)
    rows.value = res?.records || []
    totalRecords.value = res?.total || 0
  } catch (e) {
    message.error('加载Amazon补货数据失败')
  } finally { loading.value = false }
}

onMounted(() => { loadData(); initColConfig('amz-replenishment') })

// ===== 列筛选 =====
const filterField = ref('')
const activeFilters = ref([])
const filterInputVal = ref('')
const filterNumOpVal = ref('>')
const filterNumInputVal = ref('')
const filterChecked = ref([])
const showFilter = ref(false)
const filterX = ref(0)
const filterY = ref(0)

const distinctFilterValues = computed(() => {
  return filterSearchResults.value.length ? filterSearchResults.value : localDistinctValues.value
})
const localDistinctValues = computed(() => {
  if (!filterField.value) return []
  const set = new Set()
  for (const row of rows.value) {
    const v = row[filterField.value]
    if (v != null && String(v).trim()) set.add(String(v).trim())
  }
  return [...set].sort().slice(0, 50)
})
// 服务端搜索回显
const filterSearchResults = ref([])
const filterRawRef = ref(null)
let filterTimer = null
watch(filterInputVal, (val) => {
  if (!showFilter.value || !filterField.value) return
  clearTimeout(filterTimer)
  filterTimer = setTimeout(async () => {
    if (!val || !val.trim()) { filterSearchResults.value = []; return }
    try { filterSearchResults.value = await fetchAmzDistinctValues(filterField.value, val.trim()) || [] }
    catch { filterSearchResults.value = [] }
  }, 200)
})

function openFilter(key, e) {
  filterField.value = key; filterInputVal.value = ''; filterNumOpVal.value = '>'; filterNumInputVal.value = ''; filterChecked.value = []
  const exist = activeFilters.value.find(f => f.field === key)
  if (exist) {
    if (NUMERIC_FIELDS.has(key)) {
      const m = exist.value.match(/^(>=|<=|>|<|=)(.+)$/)
      if (m) { filterNumOpVal.value = m[1]; filterNumInputVal.value = m[2] }
    } else { filterInputVal.value = exist.value }
  }
  const rect = e.currentTarget.getBoundingClientRect()
  filterX.value = rect.left; filterY.value = rect.bottom + 4; showFilter.value = true
}
function toggleFilterCheck(val) {
  const idx = filterChecked.value.indexOf(val)
  if (idx >= 0) filterChecked.value.splice(idx, 1); else filterChecked.value.push(val)
}
function applyFilter() {
  const field = filterField.value
  const rawVal = NUMERIC_FIELDS.has(field)
    ? (filterNumInputVal.value ? filterNumOpVal.value + filterNumInputVal.value.trim() : '')
    : filterInputVal.value
  const val = filterChecked.value.length ? filterChecked.value.join(',') : (rawVal && rawVal.trim() ? rawVal.trim() : '')
  activeFilters.value = activeFilters.value.filter(f => f.field !== field)
  if (val) activeFilters.value.push({ field, value: val, display: (columnMap[field] || field) + ':' + val })
  showFilter.value = false; query.page = 1; loadData()
}
function clearFilter() {
  activeFilters.value = activeFilters.value.filter(f => f.field !== filterField.value)
  filterInputVal.value = ''; filterNumOpVal.value = '>'; filterNumInputVal.value = ''; filterChecked.value = []; showFilter.value = false
  query.page = 1; loadData()
}
function clearAllFilters() { activeFilters.value = []; filterField.value = ''; query.page = 1; loadData() }

function handleSortClick(key) {
  if (!NUMERIC_FIELDS.has(key)) return
  if (sortField.value !== key) { sortField.value = key; sortOrder.value = 'desc' }
  else if (sortOrder.value === 'desc') { sortOrder.value = 'asc' }
  else if (sortOrder.value === 'asc') { sortField.value = ''; sortOrder.value = '' }
  else { sortField.value = key; sortOrder.value = 'desc' }
  query.page = 1; loadData()
}

const filteredRows = computed(() => rows.value)

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
const columns = [
  { type: 'selection', multiple: true, width: 40, fixed: 'left' },
  { title: 'MSKU', key: 'sellerSku', width: 150, fixed: 'left', ellipsis: { tooltip: true } },
  { title: '店铺', key: 'store', width: 160, fixed: 'left' },
  { title: '仓库SKU', key: 'warehouseSku', width: 130, ellipsis: { tooltip: true } },
  { title: '仓库', key: 'warehouseName', width: 130, ellipsis: { tooltip: true } },
  { title: 'ASIN', key: 'asin', width: 130, ellipsis: { tooltip: true } },
  { title: '评分', key: 'rating', width: 165, render: (row) => {
      const r = row.rating != null && !isNaN(Number(row.rating)) ? Number(row.rating) : 0
      if (r > 5) return ''
      const full = Math.floor(r)
      const starPath = 'M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z'
      const children = []
      for (let i = 0; i < 5; i++) {
        children.push(h('svg', { viewBox: '0 0 24 24', width: '13', height: '13',
          style: { color: i < full ? '#f5a623' : '#e0e0e0', marginRight: '1px', flexShrink: 0, verticalAlign: 'middle' }
        }, [h('path', { fill: 'currentColor', d: starPath })]))
      }
      children.push(h('span', { style: { fontSize: '12px', color: '#999', marginLeft: '4px' } }, String(r)))
      return h('span', { style: { display: 'inline-flex', alignItems: 'center' } }, children)
    } },
  { title: '评论数', key: 'reviews', width: 80 },
  { title: '广告费率', key: 'adRate', width: 100, render: (row) => row.adRate != null ? row.adRate + '%' : '' },
  { title: '近30天利润率', key: 'profitRate30', width: 130, render: (row) => row.profitRate30 != null ? row.profitRate30 + '%' : '' },
  { title: '近90天退款率', key: 'refundRate90', width: 130, render: (row) => row.refundRate90 != null ? row.refundRate90 + '%' : '' },
  { title: '产品分类', key: 'category', width: 130, render: (row) => {
      // 用闭包变量暂存输入值
      if (!row._catInput) row._catInput = row.category || ''
      return h(NInput, {
        size: 'tiny',
        value: row._catInput,
        placeholder: '输入分类',
        style: { width: '100%' },
        'onUpdate:value': (v) => { row._catInput = v },
        onBlur: () => {
          if ((row._catInput || '') !== (row.category || '')) {
            row.category = row._catInput
            saveAmzCategory(String(row.sid), row.sellerSku, row._catInput || '').catch(() => {})
          }
        },
        onKeyup: (e) => { if (e.key === 'Enter') e.target?.blur() },
      })
    } },
  { title: '已采购数量', key: 'purchased', width: 110 },
  { title: '国内仓数量', key: 'domesticStock', width: 110 },
  { title: '待出库', key: 'lockNum', width: 80 },
  { title: 'FBA在库', key: 'fbStock', width: 90 },
  { title: 'FBA在途', key: 'fbaOnway', width: 90 },
  { title: '总库存', key: 'totalStock', width: 90 },
  { title: '7天销量', key: 'sales7', width: 90 },
  { title: '14天销量', key: 'sales14', width: 90 },
  { title: '30天销量', key: 'sales30', width: 90 },
  { title: '60天销量', key: 'sales60', width: 90 },
  { title: '14日均销量', key: 'speed14', width: 120 },
  { title: '30日均销量', key: 'speed30', width: 120 },
  { title: '60日均销量', key: 'speed60', width: 120 },
  { title: '安全库存', key: 'safetyStock', width: 90 },
  { title: '平均月销量', key: 'avgMonthly', width: 110 },
  { title: '补货量', key: 'replenishQty', width: 90 },
  { title: '发货量', key: 'shipment', width: 90 },
  { title: '补货时间(天)', key: 'restockDays', width: 120 },
  { title: '负责人', key: 'principalName', width: 100, ellipsis: { tooltip: true } },
].map(c => ({ ...c, resizable: false, minWidth: 60 }))

const columnMap = columns.reduce((m, c) => { if (c.key) m[c.key] = c.title; return m }, {})

columns.forEach(c => {
  if (!c.key) return
  const needSort = NUMERIC_FIELDS.has(c.key)
  const origTitle = c.title
  c.title = () => h('span', { style: { display: 'inline-flex', alignItems: 'center', justifyContent: 'center', gap: '10px' } },
    [renderFilterIcon(c.key), origTitle, needSort ? renderSortIcon(c.key) : null].filter(Boolean))
})

const scrollX = columns.reduce((s, c) => s + (Number(c?.width) || 100), 0)

// ===== 列配置（对齐 eBay 补货页，使用 composable 持久化） =====
const {
  showDrawer, visibleKeys, editingKeys, leftCols, selCols, isAllChecked,
  init: initColConfig, openDrawer, apply: applyColConfig,
  toggleAll, toggleColumn, onDragStart, onDragOver, onDrop, onDragEnd,
} = useColumnConfig(['sellerSku', 'store'], columnMap)

const colByKey = columns.reduce((m, c) => { if (c.key) m[c.key] = c; return m }, {})
const selectionColumn = columns[0]
const visibleColumns = computed(() => {
  const ordered = visibleKeys.value.map(k => colByKey[k]).filter(Boolean)
  return [selectionColumn, ...ordered]
})
const visibleScrollX = computed(() => visibleColumns.value.reduce((s, c) => s + (Number(c?.width) || 100), 0))

onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))
function onDocClick() { if (showFilter.value) showFilter.value = false }

async function handleSyncAll() {
  syncLoading.value = true
  try {
    const res = await apiPost('/api/sync/amz-refresh-all')
    message.success('拉取完成：listing=' + (res?.listing||0) + ' profit=' + (res?.profit||0) + ' restock=' + (res?.restock||0) + ' inventory=' + (res?.inventory||0))
    await loadData()
  } catch (e) {
    message.error('拉取失败')
  } finally { syncLoading.value = false }
}

async function handleRefresh() {
  loading.value = true
  try {
    await apiPost('/api/amz/inventory/refresh')
    await loadData()
    message.success('刷新完成')
  } catch (e) {
    message.error('刷新失败')
  } finally { loading.value = false }
}
const checkedRowKeys = ref([])
const importExportLoading = ref(false)

function handleDropdownSelect(key) {
  if (key === 'exportExcel') handleExportExcel()
}

async function handleExportExcel() {
  if (importExportLoading.value) return
  importExportLoading.value = true
  try {
    const colKeys = visibleKeys.value.filter(k => k !== 'selection')
    const colTitles = colKeys.map(k => columnMap[k] || k)
    const body = { colKeys, colTitles }
    if (activeFilters.value.length) {
      body.filters = activeFilters.value.map(f => ({ field: f.field, value: f.value }))
    }
    if (checkedRowKeys.value.length) {
      body.rowKeys = checkedRowKeys.value
    }
    const token = JSON.parse(localStorage.getItem('inventory-auth-session') || '{}').token || ''
    const base = import.meta.env.VITE_API_BASE_URL || window.location.origin
    const resp = await fetch(base + '/api/amz/inventory/export', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` },
      body: JSON.stringify(body),
    })
    if (!resp.ok) throw new Error('导出失败')
    const blob = await resp.blob()
    const a = document.createElement('a')
    a.href = URL.createObjectURL(blob)
    a.download = 'Amazon补货_' + new Date().toISOString().slice(0,10) + '.xlsx'
    a.click()
    URL.revokeObjectURL(a.href)
    message.success('导出成功')
  } catch (e) {
    message.error('导出失败')
  } finally { importExportLoading.value = false }
}
</script>

<template>
  <div class="dashboard-page">
    <div class="table-card-wrap">
      <NCard title="Amazon 补货" size="large">
        <template #header-extra>
          <NSpace align="center" size="small">
            <NTag size="small" :bordered="false" type="default">共 {{ totalRecords }} 条</NTag>
            <NButton size="small" secondary :loading="syncLoading" @click="handleSyncAll">
              <template #icon>
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              </template>
              拉取最新数据
            </NButton>
            <NButton size="small" secondary :loading="loading" @click="handleRefresh">
              <template #icon>
                <svg viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><path d="M20.49 15a9 9 0 1 1-2.12-9.36L23 10"/></svg>
              </template>
              刷新快照
            </NButton>
            <NDropdown trigger="click" :options="[{ label: '导出 Excel', key: 'exportExcel' }]" @select="handleDropdownSelect">
              <NButton size="small" type="info" :loading="importExportLoading">导入导出</NButton>
            </NDropdown>
            <NButton size="small" round @click="openDrawer" title="列设置">
              <template #icon>
                <svg viewBox="0 0 1024 1024" width="14" height="14"><path fill="currentColor" d="M512 938.666667a32.032 32.032 0 0 1-15.648-4.085334l-352-197.333333A32 32 0 0 1 128 709.333333v-394.666666a32 32 0 0 1 16.352-27.914667l352-197.333333a32 32 0 0 1 31.296 0l352 197.333333A32 32 0 0 1 896 314.666667v394.666666a32 32 0 0 1-16.352 27.914667l-352 197.333333A32 32 0 0 1 512 938.666667zM192 690.581333L512 869.973333l320-179.392V333.408L512 154.016 192 333.408v357.173333zM512 682.666667c-94.101333 0-170.666667-76.565333-170.666667-170.666667S417.898667 341.333333 512 341.333333s170.666667 76.565333 170.666667 170.666667-76.565333 170.666667-170.666667 170.666667z m0-277.333334c-58.816 0-106.666667 47.850667-106.666667 106.666667s47.850667 106.666667 106.666667 106.666667 106.666667-47.850667 106.666667-106.666667S570.816 405.333333 512 405.333333z"/></svg>
              </template>
            </NButton>
          </NSpace>
        </template>

        <div v-if="activeFilters.length" class="filter-tags" style="padding: 0 16px 8px;">
          <NTag v-for="f in activeFilters" :key="f.field" closable size="small" type="info" @close="activeFilters = activeFilters.filter(af => af.field !== f.field); query.page = 1; loadData()">{{ f.display }}</NTag>
          <NButton size="tiny" text type="primary" @click="clearAllFilters()">清除全部</NButton>
        </div>

        <NDataTable
          remote
          :loading="loading"
          :columns="visibleColumns"
          :data="filteredRows"
          :bordered="false"
          :scroll-x="visibleScrollX"
          :max-height="680"
          :row-key="(row) => `${row.sid || ''}|${row.sellerSku || ''}|${row.warehouseSku || ''}`"
          v-model:checked-row-keys="checkedRowKeys"
          :pagination="{
            page: query.page,
            pageSize: query.size,
            itemCount: totalRecords,
            showSizePicker: true,
            pageSizes: [20, 50, 100, 200],
            onUpdatePage: (p) => { query.page = p; loadData() },
            onUpdatePageSize: (s) => { query.size = s; query.page = 1; loadData() },
          }"
          striped
        />
      </NCard>
    </div>

    <!-- ===== 列配置抽屉 ===== -->
    <NDrawer v-model:show="showDrawer" :width="480" placement="right">
      <NDrawerContent title="列设置" closable>
        <div class="col-config-body">
          <div class="col-config-left">
            <NSpace vertical size="small">
              <NCheckbox :checked="isAllChecked" @update:checked="toggleAll">全选</NCheckbox>
              <NCheckbox v-for="c in leftCols" :key="c.key" :checked="c.checked" :disabled="c.disabled" @update:checked="toggleColumn(c.key)">{{ c.title }}</NCheckbox>
            </NSpace>
          </div>
          <div class="col-config-right">
            <div class="col-config-right-title">已选字段</div>
            <div v-for="(c, idx) in selCols" :key="c.key"
              class="col-config-item"
              :class="{ 'col-config-item-fixed': c.fixed }"
              :draggable="!c.fixed"
              @dragstart="onDragStart(idx)"
              @dragover="onDragOver"
              @drop="onDrop(idx)"
              @dragend="onDragEnd">
              <span class="col-config-drag" :style="{ visibility: c.fixed ? 'hidden' : 'visible' }">⠿</span>
              <span>{{ c.title }}{{ c.fixed ? ' (固定)' : '' }}</span>
            </div>
          </div>
        </div>
        <template #footer>
          <NSpace justify="end">
            <NButton @click="showDrawer = false">取消</NButton>
            <NButton type="primary" @click="applyColConfig('amz-replenishment', message)">确定</NButton>
          </NSpace>
        </template>
      </NDrawerContent>
    </NDrawer>

    <!-- ===== 筛选弹窗 ===== -->
    <Teleport to="body">
      <div v-if="showFilter" class="filter-popover" :style="{ left: filterX + 'px', top: filterY + 'px' }" @click.stop>
        <div class="filter-popover-inner">
          <div style="font-size:12px;color:#999;margin-bottom:6px">筛选字段：{{ columnMap[filterField] || filterField }}</div>
          <div v-if="!NUMERIC_FIELDS.has(filterField)">
            <input :value="filterInputVal" class="filter-raw-input" placeholder="搜索关键词..." @input="filterInputVal = $event.target.value" @keyup.enter="applyFilter()" />
          </div>
          <div v-else class="filter-num-row">
            <select v-model="filterNumOpVal" class="filter-op-select">
              <option v-for="op in ['>','>=','=','<=','<']" :key="op" :value="op">{{ op }}</option>
            </select>
            <input :value="filterNumInputVal" class="filter-raw-input" style="flex:1" type="number" placeholder="数值" @input="filterNumInputVal = $event.target.value" @keyup.enter="applyFilter()" />
          </div>
          <div v-if="distinctFilterValues.length" class="filter-distinct-list">
            <div class="filter-distinct-title">{{ filterSearchResults.length ? '搜索结果' : '当前页可选值' }}
              <NButton size="tiny" text type="primary" style="float:right" @click="filterChecked = (filterChecked.length === distinctFilterValues.length ? [] : [...distinctFilterValues])">
                {{ filterChecked.length === distinctFilterValues.length ? '取消全选' : '全选' }}
              </NButton>
            </div>
            <div v-for="v in distinctFilterValues" :key="v" class="filter-distinct-item">
              <NCheckbox size="small" :checked="filterChecked.includes(v)" @update:checked="toggleFilterCheck(v)" />
              <span @click="toggleFilterCheck(v)" class="filter-item-label">{{ v }}</span>
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
.table-card-wrap :deep(.n-data-table .n-data-table-base-table-body) { overflow-y: auto !important; }
:deep(.n-data-table-th) { background: #fafafa; font-weight: 600; font-size: 12px; color: rgba(0,0,0,0.55); text-align: center !important; }
:deep(.n-data-table-td) { font-size: 13px; border-bottom: 1px solid #f5f5f5; text-align: center !important; }

.col-config-body { display: flex; gap: 16px; min-height: 300px; }
.col-config-left { width: 160px; flex-shrink: 0; padding: 8px; border-right: 1px solid #eee; max-height: 400px; overflow-y: auto; }
.col-config-right { flex: 1; padding: 8px; max-height: 400px; overflow-y: auto; }
.col-config-right-title { font-size: 12px; color: #999; margin-bottom: 8px; }
.col-config-item { display: flex; align-items: center; gap: 6px; padding: 6px 10px; margin-bottom: 4px; background: #fafafa; border-radius: 4px; font-size: 13px; border: 1px solid #eee; cursor: grab; }
.col-config-item:hover { border-color: #1677ff; }
.col-config-item-fixed { background: #f5f5f5; color: #999; cursor: default; }
.col-config-drag { color: #bbb; font-size: 14px; user-select: none; }

.filter-popover { position: fixed; z-index: 2000; }
.filter-popover-inner { background: #fff; border-radius: 8px; box-shadow: 0 6px 16px rgba(0,0,0,0.12); padding: 12px; min-width: 220px; max-width: 300px; }
.filter-raw-input { width: 100%; box-sizing: border-box; height: 30px; padding: 0 8px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 13px; outline: none; }
.filter-raw-input:focus { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,0.1); }
.filter-num-row { display: flex; align-items: center; gap: 8px; }
.filter-op-select { height: 30px; width: 60px; border: 1px solid #d9d9d9; border-radius: 4px; font-size: 14px; text-align: center; cursor: pointer; outline: none; }
.filter-op-select:focus { border-color: #1677ff; }
.filter-distinct-list { max-height: 180px; overflow-y: auto; margin-top: 8px; border-top: 1px solid #eee; padding-top: 4px; }
.filter-distinct-title { font-size: 11px; color: #999; padding: 4px 0; }
.filter-distinct-item { display: flex; align-items: center; gap: 6px; font-size: 13px; padding: 3px 8px; cursor: pointer; border-radius: 3px; }
.filter-distinct-item:hover { background: #e6f4ff; color: #1677ff; }
.filter-item-label { flex: 1; cursor: pointer; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
