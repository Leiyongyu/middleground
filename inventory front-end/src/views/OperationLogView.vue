<script setup>
import { h, onMounted, ref } from 'vue'
import { NButton, NCard, NDataTable, NModal, NTag, NSpace, useMessage } from 'naive-ui'
import { fetchOperationLogs } from '@/api/operationLogs'
import { useDataTable } from '@/composables/useDataTable'

const message = useMessage()

const { loading, records, total, query, loadData } = useDataTable(
  fetchOperationLogs,
  {},
  { pageSize: 20 },
)

const showDetail = ref(false)
const detailContent = ref('')

function openDetail(row) {
  try {
    const obj = typeof row.details === 'string' ? JSON.parse(row.details) : row.details
    detailContent.value = JSON.stringify(obj, null, 2)
  } catch {
    detailContent.value = row.details || '(无详情)'
  }
  showDetail.value = true
}

function getStatusType(s) {
  if (s === '成功') return 'success'
  if (s && s.startsWith('成功')) return 'warning'
  if (s === '失败') return 'error'
  return 'info'
}

function formatTime(v) {
  if (!v) return ''
  return String(v).replace('T', ' ').substring(0, 19)
}

const columns = [
  { title: '时间', key: 'createTime', width: 170,
    render: (row) => formatTime(row.createTime) },
  { title: '操作人', key: 'operator', width: 100 },
  { title: '方法', key: 'httpMethod', width: 70, align: 'center' },
  { title: '接口路径', key: 'apiPath', width: 200, ellipsis: { tooltip: true } },
  { title: '操作类型', key: 'operationType', width: 80, align: 'center' },
  { title: '目标', key: 'target', width: 220, ellipsis: { tooltip: true } },
  { title: '状态', key: 'status', width: 80, align: 'center',
    render(row) {
      return h(NTag, { size: 'small', type: getStatusType(row.status), bordered: false },
        { default: () => row.status })
    },
  },
  { title: '总数', key: 'totalCount', width: 70, align: 'center' },
  { title: '成功数', key: 'successCount', width: 80, align: 'center' },
  { title: '失败数', key: 'failCount', width: 80, align: 'center' },
  { title: 'IP', key: 'ipAddress', width: 120 },
  { title: '错误信息', key: 'errorMessage', width: 200, ellipsis: { tooltip: true },
    render: (row) => row.errorMessage || '—' },
  { title: '详情', key: 'details', width: 80, align: 'center',
    render(row) {
      if (!row.details) return '—'
      return h(NButton, { size: 'tiny', text: true, type: 'info', onClick: () => openDetail(row) }, { default: () => '查看' })
    },
  },
]
</script>

<template>
  <div class="dashboard-page">
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:4px">
      <h2 class="users-title" style="margin:0">操作日志</h2>
      <NButton @click="loadData" :loading="loading">刷新</NButton>
    </div>

    <NCard size="small">
      <template #header-extra>
        <NTag size="small" :bordered="false">共 {{ total }} 条</NTag>
      </template>
      <NDataTable
        remote
        :loading="loading"
        :columns="columns"
        :data="records"
        :row-key="(row) => row.id"
        :scroll-x="1800"
        :max-height="600"
        :pagination="{
          page: query.page,
          pageSize: query.size,
          itemCount: total,
          showSizePicker: true,
          pageSizes: [10, 20, 50],
          onUpdatePage: (p) => { query.page = p; loadData() },
          onUpdatePageSize: (s) => { query.size = s; query.page = 1; loadData() },
        }"
      />
    </NCard>

    <NModal v-model:show="showDetail" title="操作详情" preset="card" style="width:680px;max-height:80vh">
      <pre style="max-height:55vh;overflow:auto;background:#f5f5f5;padding:12px;border-radius:4px;font-size:12px;white-space:pre-wrap;word-break:break-all">{{ detailContent }}</pre>
    </NModal>
  </div>
</template>
