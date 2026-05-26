<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const form = reactive({
  username: '',
  password: '',
})

const errorMessage = ref('')
const isSubmitting = ref(false)

async function handleSubmit() {
  errorMessage.value = ''
  isSubmitting.value = true

  const result = await authStore.login(form.username, form.password)

  if (!result.success) {
    errorMessage.value = result.message
    isSubmitting.value = false
    return
  }

  const redirectPath =
    typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'

  await router.push(redirectPath)
  isSubmitting.value = false
}
</script>

<template>
  <div class="login-page">
    <section class="hero-panel">
      <div class="hero-badge">Global Commerce Console</div>
      <h1>跨境电商运营中台</h1>
      <p>
        聚合订单、库存、广告与物流数据，帮助团队统一查看亚马逊、Temu、TikTok Shop
        和独立站的运营表现。
      </p>

      <div class="hero-grid">
        <article>
          <span>14</span>
          <label>接入站点</label>
        </article>
        <article>
          <span>98.6%</span>
          <label>履约准时率</label>
        </article>
        <article>
          <span>23.4%</span>
          <label>广告转化提升</label>
        </article>
      </div>
    </section>

    <section class="login-card">
      <div class="card-header">
        <h2>欢迎登录</h2>
        <p>请输入系统账号和密码</p>
      </div>

      <form class="login-form" @submit.prevent="handleSubmit">
        <label>
          <span>账号</span>
          <input
            v-model.trim="form.username"
            type="text"
            placeholder="请输入账号"
            autocomplete="username"
          />
        </label>

        <label>
          <span>密码</span>
          <input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </label>

        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>

        <button type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? '登录中...' : '登录系统' }}
        </button>
      </form>
    </section>
  </div>
</template>

<style scoped src="../assets/styles/login-view.css"></style>
