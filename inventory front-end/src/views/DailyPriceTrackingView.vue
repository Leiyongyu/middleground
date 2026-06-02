<script setup>
import { h, onMounted, ref } from 'vue'
import {
  NButton, NCard, NCheckbox, NDataTable, NForm, NFormItem, NInput, NSelect, NSpace, NTag, useMessage,
} from 'naive-ui'
import { fetchDailyPriceTracking, exportDailyPriceTracking } from '@/api/dailyPriceTracking'
import { fetchInventoryOverviewWarehouses } from '@/api/inventoryOverview'
import { useDataTable } from '@/composables/useDataTable'

const message = useMessage()

const { loading, records, total, query, filters, loadData, handleSearch, handleReset } = useDataTable(
  fetchDailyPriceTracking,
  { sku: '', brand: '', operator: '' },
  { pageSize: 20, loadOnMount: false },
)

// ===== 仓库多选下拉（和补货页同一个接口） =====
const warehouseLoading = ref(false)
const warehouseOptions = ref([])
const siteFilter = ref([])

async function loadWarehouseOptions() {
  warehouseLoading.value = true
  try {
    const list = await fetchInventoryOverviewWarehouses()
    warehouseOptions.value = Array.isArray(list)
      ? list.map((item) => {
          const label = item?.label ? String(item.label) : ''
          if (!label) return null
          return { label, value: label }
        }).filter(Boolean)
      : []
    siteFilter.value = []
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载仓库下拉失败')
    warehouseOptions.value = []
    siteFilter.value = []
  } finally {
    warehouseLoading.value = false
  }
}

// 包装 loadData：将仓库多选转为逗号分隔字符串
async function doLoadData() {
  const selected = Array.isArray(siteFilter.value) ? siteFilter.value : []
  const optionCount = warehouseOptions.value.length
  // 全部选中或不选 → 不传 site 参数，查全部
  filters.site = selected.length > 0 && selected.length < optionCount ? selected.join(',') : ''
  loadData()
}

onMounted(() => {
  loadWarehouseOptions().then(() => doLoadData())
})

function onSearch() {
  query.page = 1
  doLoadData()
}

function onReset() {
  filters.sku = ''
  filters.operator = ''
  siteFilter.value = []
  query.page = 1
  filters.site = ''
  loadData()
}

async function handleExport() {
  try {
    await exportDailyPriceTracking({
      site: filters.site || undefined,
      sku: filters.sku.trim() || undefined,
      brand: filters.brand.trim() || undefined,
      operator: filters.operator.trim() || undefined,
    })
    message.success('导出成功')
  } catch (e) {
    message.error(e instanceof Error ? e.message : '导出失败')
  }
}

function renderWarehouseOption({ node, option, selected }) {
  return h('div', {
    style: { display: 'flex', alignItems: 'center', gap: '8px', width: '100%' },
    onClick: node?.props?.onClick,
    onMouseenter: node?.props?.onMouseenter,
    onMousemove: node?.props?.onMousemove,
  }, [
    h(NCheckbox, { checked: selected, style: { pointerEvents: 'none' } }),
    h('span', { style: { flex: 1, minWidth: 0 } }, option.label || option.value),
  ])
}

function renderLink(url) {
  if (!url) return ''
  return h('a', { href: url, target: '_blank', style: { color: '#1677ff' } }, url.length > 40 ? url.substring(0, 40) + '...' : url)
}

const columns = [
  { title: '站点', key: 'site', width: 100, fixed: 'left' },
  { title: 'SKU等级', key: 'skuLevel', width: 90, fixed: 'left' },
  { title: 'SKU', key: 'sku', width: 140, fixed: 'left', ellipsis: { tooltip: true } },
  { title: '最低价', key: 'ourLowestPrice', width: 90, align: 'right',
    render: (row) => row.ourLowestPrice ?? '' },
  { title: '跟卖价格', key: 'trackingPrice', width: 100, align: 'right',
    render: (row) => row.trackingPrice ?? '' },
  { title: '跟卖利润率', key: 'trackingProfitMargin', width: 100, align: 'right',
    render: (row) => row.trackingProfitMargin != null ? row.trackingProfitMargin + '%' : '' },
  { title: '底线价', key: 'floorPrice', width: 90, align: 'right',
    render: (row) => row.floorPrice ?? '' },
  { title: '退货率', key: 'returnRate', width: 80, align: 'right',
    render: (row) => row.returnRate != null ? row.returnRate + '%' : '' },
  { title: '近3天销量', key: 'last3DaysSales', width: 100, align: 'right',
    render: (row) => row.last3DaysSales ?? '' },
  { title: '近7天销量', key: 'last7DaysSales', width: 100, align: 'right',
    render: (row) => row.last7DaysSales ?? '' },
  { title: '近30天销量', key: 'last30DaysSales', width: 110, align: 'right',
    render: (row) => row.last30DaysSales ?? '' },
  { title: '近90天销量', key: 'last90DaysSales', width: 110, align: 'right',
    render: (row) => row.last90DaysSales ?? '' },
  { title: '历史最大月销', key: 'maxMonthlySales', width: 120, align: 'right',
    render: (row) => row.maxMonthlySales ?? '' },
  { title: 'eBay前台首页', key: 'ebayFrontpageUrl', width: 150, ellipsis: { tooltip: true },
    render: (row) => renderLink(row.ebayFrontpageUrl) },
  { title: '前台已售页面', key: 'frontpageSoldUrl', width: 150, ellipsis: { tooltip: true },
    render: (row) => renderLink(row.frontpageSoldUrl) },
  { title: '海外仓库存', key: 'overseasWarehouseStock', width: 110, align: 'right',
    render: (row) => row.overseasWarehouseStock ?? '' },
  { title: '海外仓库龄', key: 'overseasWarehouseAge', width: 110, align: 'right',
    render: (row) => row.overseasWarehouseAge != null ? row.overseasWarehouseAge + '天' : '' },
  { title: '库销比', key: 'stockSalesRatio', width: 90, align: 'right',
    render: (row) => row.stockSalesRatio ?? '' },
  { title: '预估补货量', key: 'estimatedReplenish', width: 110, align: 'right',
    render: (row) => row.estimatedReplenish ?? '' },
  { title: '品牌', key: 'brand', width: 100 },
  { title: '操作员', key: 'operator', width: 100 },
  { title: '备注', key: 'remark', width: 180, ellipsis: { tooltip: true },
    render: (row) => row.remark || '' },
].map((c) => ({ ...c, resizable: true, minWidth: 70 }))

const scrollX = columns.reduce((s, c) => s + (Number(c?.width) || 100), 0)
</script>

<template>
  <section class="dashboard-page">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
      <h2 class="users-title" style="margin:0">每日跟价</h2>
      <NSpace>
        <NButton @click="doLoadData" :loading="loading">刷新</NButton>
        <NButton type="primary" @click="handleExport">导出 Excel</NButton>
      </NSpace>
    </div>

    <NCard size="small" class="dashboard-card" style="margin-bottom:16px">
      <NForm inline :model="filters" @keyup.enter="onSearch">
        <NFormItem label="站点">
          <NSelect
            v-model:value="siteFilter"
            multiple
            filterable
            clearable
            placeholder="选择站点"
            :options="warehouseOptions"
            :loading="warehouseLoading"
            :render-option="renderWarehouseOption"
            :max-tag-count="1"
            style="width: 180px"
            @update:value="onSearch"
          />
        </NFormItem>
        <NFormItem label="SKU">
          <NInput v-model:value="filters.sku" clearable placeholder="SKU模糊搜索" style="width:180px" />
        </NFormItem>
        <NFormItem label="操作员">
          <NInput v-model:value="filters.operator" clearable placeholder="操作员" style="width:120px" />
        </NFormItem>
        <NFormItem>
          <NSpace size="small">
            <NButton type="primary" secondary @click="onSearch" :loading="loading">查询</NButton>
            <NButton @click="onReset">重置</NButton>
          </NSpace>
        </NFormItem>
      </NForm>
    </NCard>

    <NCard size="small" class="dashboard-card">
      <template #header-extra>
        <NTag size="small" :bordered="false">共 {{ total }} 条</NTag>
      </template>

      <NDataTable
        remote
        :loading="loading"
        :columns="columns"
        :data="records"
        :row-key="(row) => `${row.site || ''}|${row.sku || ''}`"
        :scroll-x="scrollX"
        :max-height="510"
        :pagination="{
          page: query.page,
          pageSize: query.size,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50, 100],
          onUpdatePage: (p) => { query.page = p; doLoadData() },
          onUpdatePageSize: (s) => { query.size = s; query.page = 1; doLoadData() },
        }"
        striped
      />
    </NCard>
  </section>
</template>

<style scoped src="../assets/styles/user-management.css"></style>
