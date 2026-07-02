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

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
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
            placeholder="邮箱地址"
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
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.login-card {
  width: 100%;
  max-width: 400px;
  padding: 40px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.1);
}

.logo {
  text-align: center;
  margin-bottom: 30px;
}

.logo h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  color: #333;
}

.logo p {
  margin: 8px 0 0 0;
  font-size: 14px;
  color: #999;
}

:deep(.el-form) {
  margin-bottom: 20px;
}

:deep(.el-form-item) {
  margin-bottom: 16px;
}

:deep(.el-input__wrapper) {
  padding: 8px 12px;
}

.login-btn {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.register-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.register-link :deep(.el-button) {
  padding: 0;
  font-size: 14px;
}

@media (max-width: 480px) {
  .login-card {
    max-width: 100%;
    margin: 20px;
    padding: 30px 20px;
  }

  .logo h1 {
    font-size: 24px;
  }
}
</style>
