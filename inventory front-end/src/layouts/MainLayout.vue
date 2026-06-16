<script setup>
import { computed, h, ref, watch } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter, useRoute } from 'vue-router'
import {
  NButton,
  NIcon,
  NLayout,
  NLayoutContent,
  NLayoutHeader,
  NLayoutSider,
  NMenu,
  NSelect,
  NSpace,
  NTag,
  NDropdown,
  NAvatar,
} from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const uiStore = useUiStore()
const { selectedPlatform } = storeToRefs(uiStore)

const collapsed = ref(true)
const userName = computed(() => authStore.user?.ownerName || authStore.user?.name || '未设置')
const userAccount = computed(() => authStore.user?.account || '')
const roleLabel = computed(() => (authStore.isAdmin ? '管理员' : '用户'))
const roleType = computed(() => (authStore.isAdmin ? 'error' : 'info'))
const canManageUsers = computed(() => authStore.isAdmin)
const platformOptions = computed(() =>
  uiStore.platformOptions.map((platform) => ({ label: platform, value: platform }))
)

// 根据当前路由自动同步平台选择器
watch(() => route.path, (path) => {
  if (path.startsWith('/amz')) uiStore.setPlatform('亚马逊')
  else if (path === '/dashboard' || path === '/daily-price-tracking' || path === '/purchases') uiStore.setPlatform('eBay')
}, { immediate: true })

const activeKey = computed(() => route.path)

// 当前路径对应的父菜单 key，用于切换路由时自动展开对应分组
const parentKeyForRoute = computed(() => {
  const map = {
    '/dashboard': 'operations',
    '/daily-price-tracking': 'operations',
    '/amz-replenishment': 'operations',
    '/purchases': 'purchase-group',
    '/purchase-plan/create': 'purchase-group',
    '/amz-purchases': 'purchase-group',
    '/users': 'system',
    '/brand-owners': 'system',
    '/link-templates': 'system',
  }
  return map[route.path] || 'operations'
})
const expandedKeys = ref([parentKeyForRoute.value])

// 路由切换时自动展开对应父菜单
watch(parentKeyForRoute, (key) => {
  if (!expandedKeys.value.includes(key)) {
    expandedKeys.value = [key]
  }
})

// 构建菜单项 —— 运营和采购按平台切换，系统管理始终可见
const menuOptions = computed(() => {
  const isAmz = selectedPlatform.value === '亚马逊'
  const opsChildren = isAmz
    ? [{ label: 'Amazon 补货', key: '/amz-replenishment' }]
    : [
        { label: '补货', key: '/dashboard' },
        { label: '每日跟价', key: '/daily-price-tracking' },
      ]
  const purchaseChildren = isAmz
    ? [{ label: 'Amazon 采购管理', key: '/amz-purchases' }]
    : [
        { label: '采购管理', key: '/purchases' },
      ]
  const base = [
    {
      label: '运营',
      key: 'operations',
      icon: renderMenuIcon('operations'),
      children: opsChildren,
    },
    {
      label: '采购',
      key: 'purchase-group',
      icon: renderMenuIcon('purchase'),
      children: purchaseChildren,
    },
  ]
  if (canManageUsers.value) {
    base.push({
      label: '系统管理',
      key: 'system',
      icon: renderMenuIcon('system'),
      children: [
        { label: '用户管理', key: '/users' },
        { label: '品牌管理', key: '/brand-owners' },
        { label: '链接管理', key: '/link-templates' },
        { label: '操作日志', key: '/operation-logs' },
      ],
    })
  }
  return base
})

// 路由映射表
const routeMap = {
  '/dashboard': 'dashboard',
  '/daily-price-tracking': 'dailyPriceTracking',
  '/users': 'users',
  '/brand-owners': 'brandOwners',
  '/purchases': 'purchases',
  '/link-templates': 'linkTemplates',
  '/operation-logs': 'operationLogs',
  '/amz-replenishment': 'amzReplenishment',
  '/amz-purchases': 'amzPurchases',
}

function handleMenuUpdate(key) {
  const name = routeMap[key]
  if (name) {
    router.push({ name })
  }
}

// 用户下拉菜单
const userDropdownOptions = [
  {
    label: '退出登录',
    key: 'logout',
    icon: () =>
      h(
        'svg',
        { viewBox: '0 0 24 24', width: '16', height: '16', fill: 'none', stroke: 'currentColor', 'stroke-width': '2' },
        [
          h('path', { d: 'M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4' }),
          h('polyline', { points: '16,17 21,12 16,7' }),
          h('line', { x1: '21', y1: '12', x2: '9', y2: '12' }),
        ],
      ),
  },
]

function handleUserSelect(key) {
  if (key === 'logout') {
    handleLogout()
  }
}

// SVG 图标渲染
function renderMenuIcon(name) {
  const icons = {
    // 运营 - 趋势图图标
    operations: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('polyline', { points: '22 12 18 12 15 21 9 3 6 12 2 12' }),
      ],
    ),
    // 补货 - 网格/仪表盘
    dashboard: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('rect', { x: '3', y: '3', width: '7', height: '7', rx: '1' }),
        h('rect', { x: '14', y: '3', width: '7', height: '7', rx: '1' }),
        h('rect', { x: '3', y: '14', width: '7', height: '7', rx: '1' }),
        h('rect', { x: '14', y: '14', width: '7', height: '7', rx: '1' }),
      ],
    ),
    // 用户管理
    users: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('path', { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' }),
        h('circle', { cx: '9', cy: '7', r: '4' }),
        h('path', { d: 'M23 21v-2a4 4 0 0 0-3-3.87' }),
        h('path', { d: 'M16 3.13a4 4 0 0 1 0 7.75' }),
      ],
    ),
    // 采购 - 文档/订单
    purchase: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('path', { d: 'M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z' }),
        h('polyline', { points: '14,2 14,8 20,8' }),
        h('line', { x1: '16', y1: '13', x2: '8', y2: '13' }),
        h('line', { x1: '16', y1: '17', x2: '8', y2: '17' }),
      ],
    ),
    // 品牌负责人
    brand: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('path', { d: 'M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z' }),
        h('line', { x1: '7', y1: '7', x2: '7.01', y2: '7' }),
      ],
    ),
    // 系统管理 - 齿轮/设置
    system: h(
      'svg',
      {
        viewBox: '0 0 24 24', width: '20', height: '20', fill: 'none',
        stroke: 'currentColor', 'stroke-width': '2', 'stroke-linecap': 'round', 'stroke-linejoin': 'round',
      },
      [
        h('circle', { cx: '12', cy: '12', r: '3' }),
        h('path', { d: 'M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06A1.65 1.65 0 0 0 4.68 15a1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06A1.65 1.65 0 0 0 9 4.68a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06A1.65 1.65 0 0 0 19.4 9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z' }),
      ],
    ),
  }
  return () => h(NIcon, null, { default: () => icons[name] })
}

function handlePlatformSwitch(platform) {
  uiStore.setPlatform(platform)
  if (platform === '亚马逊') {
    router.push({ name: 'amzReplenishment' })
  } else {
    router.push({ name: 'dashboard' })
  }
}

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <NLayout has-sider position="absolute" class="main-layout">
    <!-- ===== 侧边栏 ===== -->
    <NLayoutSider
      bordered
      collapse-mode="width"
      :collapsed-width="64"
      :width="220"
      :collapsed="collapsed"
      show-trigger="arrow"
      class="app-sider"
      @collapse="collapsed = true"
      @expand="collapsed = false"
    >
      <!-- Logo -->
      <div class="sider-logo">
        <div class="logo-icon">
          <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2" />
          </svg>
        </div>
        <Transition name="logo-text-fade">
          <span v-show="!collapsed" class="logo-text">电商运营中台</span>
        </Transition>
      </div>

      <!-- 导航菜单 -->
      <div class="sider-menu-wrap">
        <NMenu
          :value="activeKey"
          v-model:expanded-keys="expandedKeys"
          :collapsed="collapsed"
          :collapsed-width="64"
          :collapsed-icon-size="22"
          :options="menuOptions"
          :indent="24"
          inverted
          @update:value="handleMenuUpdate"
        />
      </div>
    </NLayoutSider>

    <!-- ===== 主内容区 ===== -->
    <NLayout>
      <!-- 顶部栏 -->
      <NLayoutHeader bordered class="layout-header">
        <div class="header-left">
          <div class="breadcrumb-current">{{ route.meta?.title || '' }}</div>
        </div>

        <div class="header-right">
          <!-- 平台切换 -->
          <div class="platform-switch">
            <span class="platform-label">平台</span>
            <NSelect
              :value="selectedPlatform"
              :options="platformOptions"
              size="small"
              style="width: 110px"
              @update:value="handlePlatformSwitch"
            />
          </div>

          <!-- 用户区域 -->
          <NDropdown
            trigger="click"
            :options="userDropdownOptions"
            @select="handleUserSelect"
          >
            <div class="user-trigger">
              <NAvatar
                size="small"
                round
                :style="{ background: 'linear-gradient(135deg, #1677ff, #722ed1)' }"
              >
                {{ userName.charAt(0) }}
              </NAvatar>
              <span class="user-name">{{ userName }}</span>
              <NTag size="tiny" :bordered="false" :type="roleType" class="role-tag">
                {{ roleLabel }}
              </NTag>
            </div>
          </NDropdown>
        </div>
      </NLayoutHeader>

      <!-- 内容区域 -->
      <NLayoutContent
        content-style="padding: 12px 16px; background: #F6F7F9; min-height: calc(100vh - 48px);"
        class="layout-content"
      >
        <RouterView />
      </NLayoutContent>
    </NLayout>
  </NLayout>
</template>

<style scoped>
/* ===== 布局 ===== */
.main-layout {
  min-height: 100vh;
}

/* ===== 侧边栏 ===== */
.app-sider {
  transition: width 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.app-sider :deep(.n-layout-sider-scroll-container) {
  display: flex;
  flex-direction: column;
}

.sider-logo {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
  white-space: nowrap;
  flex-shrink: 0;
}

.logo-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  min-width: 30px;
  border-radius: 6px;
  background: #E8B830;
  color: #0D1B2A;
  font-weight: 800;
  font-size: 16px;
  transition: background 0.25s;
}

.logo-icon:hover { background: #F0C840; }

.logo-text {
  margin-left: 10px;
  font-size: 14px;
  font-weight: 600;
  color: #C8D6E5;
  letter-spacing: 0.04em;
}

.logo-text-fade-enter-active,
.logo-text-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.logo-text-fade-enter-from,
.logo-text-fade-leave-to {
  opacity: 0;
  transform: translateX(-4px);
}

.sider-menu-wrap {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

/* ===== 侧边栏主题 ===== */
:deep(.n-layout-sider) {
  background: #0D1B2A !important;
}

:deep(.n-menu.n-menu--inverted .n-menu-item-content) {
  transition: background 0.15s;
  color: #8899AA;
  font-size: 13px;
  margin: 0 8px;
  border-radius: 6px;
  padding-left: 8px !important;
}

:deep(.n-menu.n-menu--inverted .n-menu-item-content:hover) {
  background: rgba(255, 255, 255, 0.05) !important;
  color: #C8D6E5;
}

:deep(.n-menu.n-menu--inverted .n-menu-item-content--selected) {
  background: #1B3A5C !important;
  color: #E8B830 !important;
  font-weight: 600;
}

:deep(.n-menu.n-menu--inverted .n-menu-item-content--selected::before) {
  background: #E8B830 !important;
  border-radius: 0 3px 3px 0;
  width: 3px;
}

/* 子菜单分组标题 */
:deep(.n-menu.n-menu--inverted .n-menu-item-content--child-selected) {
  color: #C8D6E5 !important;
}

:deep(.n-menu .n-submenu .n-menu-item-content) {
  font-size: 13px;
  padding-left: 8px !important;
}

/* ===== 顶部栏 ===== */
.layout-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 48px;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #E8ECF0;
  z-index: 20;
}

.header-left { display: flex; align-items: center; }

.breadcrumb-current {
  font-size: 14px;
  font-weight: 600;
  color: #1A2332;
}

.header-right { display: flex; align-items: center; gap: 16px; }

.platform-switch { display: flex; align-items: center; gap: 6px; }

.platform-label {
  color: #8899AA;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

/* ===== 用户触发器 ===== */
.user-trigger {
  display: flex; align-items: center; gap: 6px;
  padding: 3px 8px 3px 3px;
  border-radius: 16px;
  cursor: pointer;
  transition: background 0.15s;
}
.user-trigger:hover { background: #F5F6F8; }

.user-name {
  font-size: 13px; font-weight: 500;
  color: #1A2332;
  max-width: 80px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.role-tag { flex-shrink: 0; }

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .layout-header {
    padding: 0 16px;
  }

  .header-right {
    gap: 12px;
  }

  .platform-switch {
    display: none;
  }
}
</style>
