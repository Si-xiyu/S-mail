<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useMailStore } from '../stores/mailStore'
import type { FormInstance } from 'element-plus'

const router = useRouter()
const mailStore = useMailStore()
const formRef = ref<FormInstance>()

const form = ref({
  email: '',
  password: ''
})

const loading = ref(false)
const smartMailPattern = /^[A-Za-z0-9._%+-]+@smail\.com$/i

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' },
    { pattern: smartMailPattern, message: '邮箱必须使用 @smail.com 后缀', trigger: 'blur' }
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await mailStore.login(form.value.email, form.value.password)
      ElMessage.success('登录成功')
      router.push('/mail')
    } catch (error) {
      const errorMsg = error instanceof Error ? error.message : '登录失败'
      ElMessage.error(errorMsg)
    } finally {
      loading.value = false
    }
  })
}

const goToRegister = () => {
  router.push('/auth/register')
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="logo">
        <h1>SmartMail</h1>
        <p>智能邮件管理平台</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="name@smail.com"
            clearable
            type="email"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="密码"
            type="password"
            show-password
            clearable
            size="large"
          />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" @click="handleLogin" class="login-btn">
          登 录
        </el-button>
      </el-form>

      <div class="register-link">
        <span>没有账户？</span>
        <el-button link @click="goToRegister" type="primary">去注册</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #f7f6f3;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 360px;
  padding: 40px 36px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e0e0e0;
}

.logo {
  text-align: left;
  margin-bottom: 36px;
}

.logo h1 {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #37352f;
}

.logo p {
  margin: 6px 0 0 0;
  font-size: 14px;
  color: rgba(55, 53, 47, 0.55);
}

:deep(.el-form) {
  margin-bottom: 0;
}

:deep(.el-form-item) {
  margin-bottom: 14px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
  border-radius: 4px;
  box-shadow: 0 0 0 1px #e0e0e0;
}
:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #b0b0b0;
}
:deep(.el-input.is-focus .el-input__wrapper) {
  box-shadow: 0 0 0 2px rgba(55, 53, 47, 0.2);
}

.login-btn {
  width: 100%;
  font-size: 14px;
  font-weight: 500;
  letter-spacing: 0;
  border-radius: 4px;
  margin-top: 4px;
  background: #37352f;
  border-color: #37352f;
}
.login-btn:hover {
  background: #2b2925;
  border-color: #2b2925;
}

.register-link {
  text-align: center;
  font-size: 13px;
  color: rgba(55, 53, 47, 0.5);
  margin-top: 20px;
}

.register-link :deep(.el-button) {
  padding: 0;
  font-size: 13px;
  color: #37352f;
}

@media (max-width: 480px) {
  .login-card {
    max-width: 100%;
    padding: 28px 20px;
  }
  .logo h1 {
    font-size: 20px;
  }
}
</style>
