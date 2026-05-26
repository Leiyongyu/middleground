<script setup>
import { h, onMounted, reactive, ref } from 'vue'
import {
  NButton,
  NCard,
  NDataTable,
  NDynamicTags,
  NForm,
  NFormItem,
  NInput,
  NModal,
  NSelect,
  NSpace,
  NTag,
  useDialog,
  useMessage,
} from 'naive-ui'
import { createUser, deleteUser, fetchUsersPage, updateUser } from '@/api/users'
import { createBrandOwner, deleteBrandOwner, fetchBrandOwnerByBrandCode, fetchBrandsByOwner, updateBrandOwner } from '@/api/brandOwners'

const message = useMessage()
const dialog = useDialog()

const loading = ref(false)
const users = ref([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
  account: '',
})

const roleOptions = [
  { label: '管理员', value: 'admin' },
  { label: '用户', value: 'user' },
]

const showCreateModal = ref(false)
const showEditModal = ref(false)
const editOwners = ref([])

const createForm = reactive({
  account: '',
  password: '',
  role: 'user',
  brandCode: '',
  ownerName: '',
})

const editForm = reactive({
  id: '',
  account: '',
  password: '',
  role: 'user',
  ownerName: '',
  brands: [],
  _originalBrands: [],
  originalOwnerName: '',
})

function normalizeRole(role) {
  if (!role) {
    return 'user'
  }

  const raw = String(role).trim()

  if (raw === '管理员') {
    return 'admin'
  }

  if (raw === '用户') {
    return 'user'
  }

  const lowered = raw.toLowerCase()

  if (lowered === 'admin') {
    return 'admin'
  }

  if (lowered === 'user' || lowered === 'viewer') {
    return 'user'
  }

  return 'user'
}

function getRoleLabel(role) {
  return normalizeRole(role) === 'admin' ? '管理员' : '用户'
}

function formatTime(value) {
  if (!value) {
    return ''
  }

  return String(value).replace('T', ' ')
}

function renderOwners(owners) {
  if (!Array.isArray(owners) || owners.length === 0) {
    return '-'
  }

  return h(
    NSpace,
    { size: 6, wrap: true },
    {
      default: () =>
        owners.map((ownerName) => {
          return h(
            NTag,
            {
              size: 'small',
              type: 'success',
              bordered: false,
            },
            {
              default: () => ownerName,
            },
          )
        }),
    },
  )
}

function renderManagedBrands(value) {
  if (!value) {
    return '-'
  }

  const raw = String(value)
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)

  if (raw.length === 0) {
    return '-'
  }

  return h(
    NSpace,
    { size: 6, wrap: true },
    {
      default: () =>
        raw.map((item) =>
          h(
            NTag,
            {
              size: 'small',
              bordered: false,
            },
            { default: () => item },
          ),
        ),
    },
  )
}

async function loadUsers() {
  loading.value = true

  try {
    const pageResult = await fetchUsersPage({
      page: query.page,
      size: query.size,
      account: query.account.trim() || undefined,
    })

    users.value = pageResult?.records || []
    total.value = Number(pageResult?.total || 0)
  } catch (error) {
    message.error(error instanceof Error ? error.message : '加载用户失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.page = 1
  loadUsers()
}

function openCreateModal() {
  createForm.account = ''
  createForm.password = ''
  createForm.role = 'user'
  createForm.brandCode = ''
  createForm.ownerName = ''
  showCreateModal.value = true
}

async function submitCreate() {
  const account = createForm.account.trim()
  const brandCode = createForm.brandCode.trim()
  const ownerName = createForm.ownerName.trim()

  if (!account) {
    message.warning('请输入账号')
    return
  }

  if (!createForm.password) {
    message.warning('请输入密码')
    return
  }

  if ((brandCode && !ownerName) || (!brandCode && ownerName)) {
    message.warning('品牌编码与负责人需要同时填写')
    return
  }

  try {
    await createUser({
      account,
      password: createForm.password,
      role: createForm.role,
      brandCode: brandCode || null,
      ownerName: ownerName || null,
    })

    message.success('新增成功')
    showCreateModal.value = false
    query.page = 1
    loadUsers()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '新增用户失败')
  }
}

async function openEditModal(row) {
  editForm.id = row.id
  editForm.account = row.account
  editForm.password = ''
  editForm.role = normalizeRole(row.role)
  editForm.ownerName = row.ownerName || ''
  editForm.originalOwnerName = row.ownerName || ''

  // 加载该负责人当前管理的品牌
  const brands = await fetchBrandsByOwner(row.ownerName)
  editForm.brands = brands.map((b) => b.brandCode)
  editForm._originalBrands = [...editForm.brands]

  editOwners.value = Array.isArray(row.owners) ? row.owners : []
  showEditModal.value = true
}

async function submitEdit() {
  if (!editForm.id) {
    return
  }

  const newOwnerName = (editForm.ownerName || '').trim()
  const oldOwnerName = editForm.originalOwnerName || ''
  const newBrands = (editForm.brands || []).map((s) => (s || '').trim()).filter(Boolean)

  try {
    // 1. 更新用户 ownerName
    await updateUser(editForm.id, {
      role: editForm.role,
      ownerName: newOwnerName || null,
      password: editForm.password || undefined,
    })

    // 2. 同步 brand_owner：如果负责人改名，先把旧名下所有品牌的 owner 更新
    if (newOwnerName && oldOwnerName && newOwnerName !== oldOwnerName) {
      const oldBrands = await fetchBrandsByOwner(oldOwnerName)
      await Promise.all(
        oldBrands.map((b) => updateBrandOwner(b.id, { ownerName: newOwnerName })),
      )
    }

    // 3. 删除被移除的品牌：品牌不在新列表中 → 从 brand_owner 中删除
    const originalBrands = editForm._originalBrands || []
    const removedBrands = originalBrands.filter((b) => !newBrands.includes(b))
    if (removedBrands.length > 0) {
      await Promise.all(
        removedBrands.map(async (brandCode) => {
          const existing = await fetchBrandOwnerByBrandCode(brandCode)
          if (existing?.id) {
            return deleteBrandOwner(existing.id)
          }
          return null
        }),
      )
    }

    // 4. 确保新品牌列表中的每个 brand 都属于该负责人
    if (newOwnerName && newBrands.length > 0) {
      await Promise.all(
        newBrands.map(async (brandCode) => {
          const existing = await fetchBrandOwnerByBrandCode(brandCode)
          if (existing?.id) {
            if (existing.ownerName !== newOwnerName) {
              return updateBrandOwner(existing.id, { ownerName: newOwnerName })
            }
            return null
          }
          return createBrandOwner({ brandCode, ownerName: newOwnerName })
        }),
      )
    }

    message.success('更新成功')
    showEditModal.value = false
    loadUsers()
  } catch (error) {
    message.error(error instanceof Error ? error.message : '更新用户失败')
  }
}

function confirmDelete(row) {
  dialog.warning({
    title: '确认删除',
    content: `确认删除用户 ${row.account} 吗？`,
    positiveText: '删除',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await deleteUser(row.id)
        message.success('删除成功')

        if (users.value.length === 1 && query.page > 1) {
          query.page -= 1
        }

        loadUsers()
      } catch (error) {
        message.error(error instanceof Error ? error.message : '删除用户失败')
      }
    },
  })
}

function confirmResetPassword(row) {
  dialog.warning({
    title: '重置密码',
    content: `确认将用户 ${row.account} 的密码重置为 123456 吗？`,
    positiveText: '重置',
    negativeText: '取消',
    onPositiveClick: async () => {
      try {
        await updateUser(row.id, { password: '123456' })
        message.success('密码已重置为 123456')
      } catch (error) {
        message.error(error instanceof Error ? error.message : '重置密码失败')
      }
    },
  })
}

const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  pageSizes: [10, 20, 50],
  showSizePicker: true,
  'onUpdate:page': (page) => {
    query.page = page
    loadUsers()
  },
  'onUpdate:pageSize': (pageSize) => {
    query.size = pageSize
    query.page = 1
    loadUsers()
  },
})

const columns = [
  {
    title: '账号',
    key: 'account',
  },
  {
    title: '角色',
    key: 'role',
    render(row) {
      return getRoleLabel(row.role)
    },
  },
  {
    title: '负责人',
    key: 'ownerName',
  },
  {
    title: '管理品牌',
    key: 'owners',
    render(row) {
      return renderOwners(row.owners)
    },
  },
  {
    title: '创建时间',
    key: 'createTime',
    render(row) {
      return formatTime(row.createTime)
    },
  },
  {
    title: '更新时间',
    key: 'updateTime',
    render(row) {
      return formatTime(row.updateTime)
    },
  },
  {
    title: '操作',
    key: 'actions',
    align: 'right',
    render(row) {
      return h(
        NSpace,
        { justify: 'end', size: 8 },
        {
          default: () => [
            h(
              NButton,
              {
                size: 'small',
                secondary: true,
                onClick: () => openEditModal(row),
              },
              { default: () => '编辑' },
            ),
            h(
              NButton,
              {
                size: 'small',
                type: 'warning',
                secondary: true,
                onClick: () => confirmResetPassword(row),
              },
              { default: () => '重置密码' },
            ),
            h(
              NButton,
              {
                size: 'small',
                type: 'error',
                secondary: true,
                onClick: () => confirmDelete(row),
              },
              { default: () => '删除' },
            ),
          ],
        },
      )
    },
  },
]

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <section class="users-page">
    <header class="users-header">
      <div>
        <h2 class="users-title">用户管理</h2>
      </div>
    </header>

    <NCard class="users-card" size="small">
      <div class="users-filters">
        <NForm inline :model="query">
          <NFormItem label="账号">
            <NInput
              v-model:value="query.account"
              clearable
              placeholder="按账号模糊查询"
              @keyup.enter="handleSearch"
            />
          </NFormItem>
          <NButton type="primary" secondary @click="handleSearch">查询</NButton>
        </NForm>

        <NButton type="primary" @click="openCreateModal">新增用户</NButton>
      </div>
    </NCard>

    <NCard class="users-card" size="small">
      <template #header>用户列表</template>
      <template #header-extra>
        <NTag size="small" :bordered="false">共 {{ total }} 条</NTag>
      </template>

      <NDataTable
        remote
        :loading="loading"
        :columns="columns"
        :data="users"
        :row-key="(row) => row.id"
        :pagination="{
          ...pagination,
          page: query.page,
          pageSize: query.size,
          itemCount: total,
        }"
      />
    </NCard>

    <NModal v-model:show="showCreateModal" preset="card" title="新增用户" style="width: 560px">
      <NForm :model="createForm" label-placement="left" label-width="88">
        <NFormItem label="账号">
          <NInput v-model:value="createForm.account" />
        </NFormItem>
        <NFormItem label="密码">
          <NInput v-model:value="createForm.password" type="password" show-password-on="click" />
        </NFormItem>
        <NFormItem label="角色">
          <NSelect v-model:value="createForm.role" :options="roleOptions" />
        </NFormItem>
        <NFormItem label="品牌编码">
          <NInput v-model:value="createForm.brandCode" placeholder="例如：BR001A" />
        </NFormItem>
        <NFormItem label="负责人">
          <NInput v-model:value="createForm.ownerName" placeholder="例如：张三" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showCreateModal = false">取消</NButton>
          <NButton type="primary" :loading="loading" @click="submitCreate">保存</NButton>
        </NSpace>
      </template>
    </NModal>

    <NModal v-model:show="showEditModal" preset="card" title="编辑用户" style="width: 560px">
      <NForm :model="editForm" label-placement="left" label-width="88">
        <NFormItem label="账号">
          <NInput v-model:value="editForm.account" disabled />
        </NFormItem>
        <NFormItem label="密码">
          <NInput
            v-model:value="editForm.password"
            type="password"
            show-password-on="click"
            placeholder="留空则不修改"
          />
        </NFormItem>
        <NFormItem label="角色">
          <NSelect v-model:value="editForm.role" :options="roleOptions" />
        </NFormItem>
        <NFormItem label="负责人">
          <NInput
            v-model:value="editForm.ownerName"
            placeholder="负责人姓名"
          />
        </NFormItem>
        <NFormItem label="管理品牌">
          <NDynamicTags v-model:value="editForm.brands" />
        </NFormItem>
      </NForm>

      <template #footer>
        <NSpace justify="end">
          <NButton @click="showEditModal = false">取消</NButton>
          <NButton type="primary" :loading="loading" @click="submitEdit">保存</NButton>
        </NSpace>
      </template>
    </NModal>
  </section>
</template>

<style scoped src="../assets/styles/user-management.css"></style>
