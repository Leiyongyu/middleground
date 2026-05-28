<script setup>
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NButton,
  NCard,
  NDataTable,
  NDatePicker,
  NInput,
  NInputNumber,
  NPopconfirm,
  NSelect,
  NSpace,
  dateZhCN,
  useMessage,
} from 'naive-ui'
import { createPurchasePlan, searchSkus, searchStores, searchWarehouses } from '@/api/purchasePlan'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

function isDateDisabled(ts) {
  return ts < Date.now() - 24 * 60 * 60 * 1000
}

function createEmptyRow() {
  return {
    sku: '',
    sid: null,
    supplier_id: null,
    fnsku: '',
    wid: null,
    purchaser_id: null,
    expect_arrive_time: null,
    quantity_plan: null,
    remark: '',
    is_auto_fill_fnsku: 0,
    is_auto_fill_store: 0,
  }
}

const items = ref([createEmptyRow()])
const submitting = ref(false)

// searchable options
const skuOptions = ref([])
const storeOptions = ref([])
const warehouseOptions = ref([])
const skuLoading = ref(false)
const storeLoading = ref(false)
const warehouseLoading = ref(false)

async function onSkuSearch(keyword) {
  skuLoading.value = true
  try {
    const list = await searchSkus(keyword)
    skuOptions.value = list.map((s) => ({ label: s.sku, value: s.sku }))
  } catch (error) {
    skuOptions.value = []
    message.error(error instanceof Error ? error.message : '加载 SKU 列表失败')
  } finally { skuLoading.value = false }
}

async function onStoreSearch(keyword) {
  storeLoading.value = true
  try {
    const list = await searchStores(keyword)
    storeOptions.value = list.map((s) => ({ label: s.seller_name, value: s.sid }))
  } catch (error) {
    storeOptions.value = []
    message.error(error instanceof Error ? error.message : '加载店铺列表失败')
  } finally { storeLoading.value = false }
}

async function onWarehouseSearch(keyword) {
  warehouseLoading.value = true
  try {
    const list = await searchWarehouses(keyword)
    warehouseOptions.value = list.map((w) => ({ label: w.name, value: w.wid }))
  } catch (error) {
    warehouseOptions.value = []
    message.error(error instanceof Error ? error.message : '加载仓库列表失败')
  } finally { warehouseLoading.value = false }
}

onMounted(() => {
  onSkuSearch('')
  onStoreSearch('')
  onWarehouseSearch('')
})

function addRow() {
  items.value.push(createEmptyRow())
}

function removeRow(index) {
  if (items.value.length <= 1) {
    items.value = [createEmptyRow()]
    return
  }
  items.value.splice(index, 1)
}

function isRowEmpty(row) {
  return (
    !String(row?.sku || '').trim() &&
    !String(row?.sid || '').trim() &&
    row?.supplier_id == null &&
    !String(row?.fnsku || '').trim() &&
    row?.wid == null &&
    row?.purchaser_id == null &&
    !String(row?.expect_arrive_time || '').trim() &&
    row?.quantity_plan == null &&
    !String(row?.remark || '').trim() &&
    (row?.is_auto_fill_fnsku ?? 0) === 0 &&
    (row?.is_auto_fill_store ?? 0) === 0
  )
}

async function submitAll() {
  const meaningfulRows = items.value.filter((row) => !isRowEmpty(row))
  if (meaningfulRows.length === 0) {
    message.warning('请至少填写一条产品')
    return
  }

  const invalidIndex = meaningfulRows.findIndex(
    (row) => !String(row?.sku || '').trim() || row?.quantity_plan === null || row?.quantity_plan === undefined,
  )
  if (invalidIndex >= 0) {
    message.warning(`第 ${invalidIndex + 1} 行 SKU/计划采购量不能为空`)
    return
  }

  submitting.value = true
  try {
    const payload = meaningfulRows.map((row) => {
      const item = {
        sku: String(row.sku || '').trim(),
        quantity_plan: row.quantity_plan,
        cg_uid: null,
      }

      if (row.sid) item.sid = row.sid
      if (row.supplier_id != null) item.supplier_id = row.supplier_id
      if (row.fnsku) item.fnsku = row.fnsku
      if (row.wid != null) item.wid = row.wid
      if (row.purchaser_id != null) item.purchaser_id = row.purchaser_id
      if (row.expect_arrive_time) item.expect_arrive_time = row.expect_arrive_time
      if (row.remark) item.remark = row.remark

      const isAutoFnsku = Number(row.is_auto_fill_fnsku) === 1
      const isAutoStore = Number(row.is_auto_fill_store) === 1
      if (isAutoFnsku || isAutoStore) {
        item.options = {}
        if (isAutoFnsku) item.options.is_auto_fill_fnsku = 1
        if (isAutoStore) item.options.is_auto_fill_store = 1
      }

      return item
    })

    const res = await createPurchasePlan(payload)
    message.success(`创建成功${res?.ppgSn ? '，批次号: ' + res.ppgSn : ''}`)
    items.value = [createEmptyRow()]
  } catch (err) {
    message.error(err instanceof Error ? err.message : '创建失败')
  } finally { submitting.value = false }
}

const columns = [
  {
    title: 'SKU',
    key: 'sku',
    render: (row) =>
      h(NSelect, {
        value: row.sku,
        size: 'small',
        clearable: true,
        filterable: true,
        options: skuOptions.value,
        loading: skuLoading.value,
        placeholder: '搜索SKU',
        style: { width: '100%', minWidth: 0 },
        onSearch: onSkuSearch,
        'onUpdate:value': (val) => {
          row.sku = val || ''
        },
      }),
  },
  {
    title: '店铺',
    key: 'sid',
    render: (row) =>
      h(NSelect, {
        value: row.sid,
        size: 'small',
        clearable: true,
        filterable: true,
        options: storeOptions.value,
        loading: storeLoading.value,
        placeholder: '搜索店铺',
        style: { width: '100%', minWidth: 0 },
        onSearch: onStoreSearch,
        'onUpdate:value': (val) => {
          row.sid = val || ''
        },
      }),
  },
  {
    title: '供应商ID',
    key: 'supplier_id',
    render: (row) =>
      h(NInputNumber, {
        value: row.supplier_id,
        size: 'small',
        min: 0,
        showButton: false,
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.supplier_id = val
        },
      }),
  },
  {
    title: 'FNSKU',
    key: 'fnsku',
    render: (row) =>
      h(NInput, {
        value: row.fnsku,
        size: 'small',
        placeholder: 'FNSKU001',
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.fnsku = val
        },
      }),
  },
  {
    title: '仓库',
    key: 'wid',
    render: (row) =>
      h(NSelect, {
        value: row.wid,
        size: 'small',
        clearable: true,
        filterable: true,
        options: warehouseOptions.value,
        loading: warehouseLoading.value,
        placeholder: '搜索仓库',
        style: { width: '100%', minWidth: 0 },
        onSearch: onWarehouseSearch,
        'onUpdate:value': (val) => {
          row.wid = val ?? null
        },
      }),
  },
  {
    title: '采购方ID',
    key: 'purchaser_id',
    render: (row) =>
      h(NInputNumber, {
        value: row.purchaser_id,
        size: 'small',
        min: 0,
        showButton: false,
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.purchaser_id = val
        },
      }),
  },
  {
    title: '采购员',
    key: 'purchaser_name',
    render: () =>
      h(NInput, { value: auth.ownerName, size: 'small', disabled: true, style: { width: '100%', minWidth: 0 } }),
  },
  {
    title: '计划采购量',
    key: 'quantity_plan',
    render: (row) =>
      h(NInputNumber, {
        value: row.quantity_plan,
        size: 'small',
        min: 1,
        placeholder: '100',
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.quantity_plan = val
        },
      }),
  },
  {
    title: '期望到货时间',
    key: 'expect_arrive_time',
    render: (row) =>
      h(NDatePicker, {
        type: 'date',
        size: 'small',
        locale: dateZhCN,
        valueFormat: 'yyyy-MM-dd',
        formattedValue: row.expect_arrive_time,
        style: { width: '100%', minWidth: 0 },
        isDateDisabled,
        'onUpdate:formattedValue': (val) => {
          row.expect_arrive_time = val ?? null
        },
      }),
  },
  {
    title: '产品备注',
    key: 'remark',
    render: (row) =>
      h(NInput, {
        value: row.remark,
        size: 'small',
        placeholder: '备注',
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.remark = val
        },
      }),
  },
  {
    title: '自动FNSKU',
    key: 'is_auto_fill_fnsku',
    render: (row) =>
      h(NSelect, {
        value: row.is_auto_fill_fnsku,
        size: 'small',
        options: [
          { label: '否', value: 0 },
          { label: '是', value: 1 },
        ],
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.is_auto_fill_fnsku = val ?? 0
        },
      }),
  },
  {
    title: '自动填充店铺',
    key: 'is_auto_fill_store',
    render: (row) =>
      h(NSelect, {
        value: row.is_auto_fill_store,
        size: 'small',
        options: [
          { label: '否', value: 0 },
          { label: '是', value: 1 },
        ],
        style: { width: '100%', minWidth: 0 },
        'onUpdate:value': (val) => {
          row.is_auto_fill_store = val ?? 0
        },
      }),
  },
  {
    title: '操作',
    key: 'actions',
    width: 120,
    render(row, index) {
      return h(NSpace, { size: 12, wrap: false }, {
        default: () => [
          h(
            NButton,
            { size: 'tiny', type: 'info', secondary: true, onClick: addRow, style: { minWidth: '30px' } },
            { default: () => '+' },
          ),
          h(
            NPopconfirm,
            {
              positiveText: '移除',
              negativeText: '取消',
              onPositiveClick: () => removeRow(index),
            },
            {
              default: () => '确认移除该行？',
              trigger: () =>
                h(
                  NButton,
                  { size: 'tiny', type: 'error', secondary: true, style: { minWidth: '30px' } },
                  { default: () => '-' },
                ),
            },
          ),
        ],
      })
    },
  },
]
</script>

<template>
  <section class="json-workspace purchase-plan-workspace">
    <NCard title="创建采购计划" size="large">
      <template #header-extra>
        <NSpace>
          <NButton size="small" @click="router.push({ name: 'dashboard' })">返回看板</NButton>
          <NButton type="success" size="small" :loading="submitting" @click="submitAll">
            提交 ({{ items.filter((row) => !isRowEmpty(row)).length }}条)
          </NButton>
        </NSpace>
      </template>

      <NDataTable
        class="purchase-plan-table"
        :columns="columns"
        :data="items"
        :row-key="(_, i) => i"
        :pagination="{ pageSize: 20 }"
        :max-height="500"
      />
    </NCard>
  </section>
</template>

<style scoped src="../assets/styles/dashboard-view.css"></style>

<style scoped>
.purchase-plan-workspace {
  padding: 16px;
  width: 100%;
  min-width: 0;
}

.purchase-plan-table :deep(.n-data-table-td),
.purchase-plan-table :deep(.n-data-table-th) {
  padding: 8px 10px;
}

.purchase-plan-table :deep(.n-data-table-table) {
  table-layout: fixed;
}

.purchase-plan-table :deep(.n-data-table-th__title) {
  white-space: nowrap;
}

.purchase-plan-table :deep(.n-data-table-td) {
  font-size: 12px;
}

.purchase-plan-table :deep(.n-data-table-td .n-input),
.purchase-plan-table :deep(.n-data-table-td .n-input-number),
.purchase-plan-table :deep(.n-data-table-td .n-base-selection) {
  width: 100%;
  min-width: 0;
  max-width: 100%;
}

.purchase-plan-table :deep(.n-data-table-td .n-input__input-el),
.purchase-plan-table :deep(.n-data-table-td .n-base-selection-label) {
  min-width: 0;
}
</style>
