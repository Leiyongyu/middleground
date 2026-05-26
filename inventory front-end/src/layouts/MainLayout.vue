<script setup>
import { computed } from 'vue'
import { storeToRefs } from 'pinia'
import { useRouter } from 'vue-router'
import { NButton, NSelect } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { useUiStore } from '@/stores/ui'

const router = useRouter()
const authStore = useAuthStore()
const uiStore = useUiStore()
const { selectedPlatform } = storeToRefs(uiStore)

const userName = computed(() => authStore.user?.name ?? '跨境运营管理员')
const canManageUsers = computed(() => authStore.isAdmin)
const platformOptions = computed(() =>
  uiStore.platformOptions.map((platform) => ({ label: platform, value: platform }))
)

async function handleLogout() {
  await authStore.logout()
  await router.push('/login')
}
</script>

<template>
  <div class="dashboard-layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-icon">EMP</div>
        <div>
          <strong>电商中台</strong>
          <span>E-commerce Middle Platform</span>
        </div>
      </div>

      <nav class="nav-list">
        <RouterLink to="/dashboard" active-class="active" exact-active-class="active">
          运营组
        </RouterLink>
        <RouterLink
          v-if="canManageUsers"
          to="/users"
          active-class="active"
          exact-active-class="active"
        >
          用户管理
        </RouterLink>
      </nav>
    </aside>

    <main class="main-content">
      <header class="topbar">
        <div>
          <h1>电商中台</h1>
          <p>ERP 管理台</p>
        </div>

        <div class="topbar-actions">
          <div class="platform-switch">
            <label for="platform-select">当前平台</label>
            <NSelect
              id="platform-select"
              v-model:value="selectedPlatform"
              :options="platformOptions"
              size="small"
            />
          </div>
          <div class="welcome-card">
            <span>欢迎回来</span>
            <strong>{{ userName }}</strong>
          </div>
          <NButton type="primary" @click="handleLogout">退出登录</NButton>
        </div>
      </header>

      <RouterView />
    </main>
  </div>
</template>

<style scoped src="../assets/styles/dashboard-view.css"></style>
