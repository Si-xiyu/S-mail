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
  username: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)

const validatePassword = (rule: any, value: any, callback: any) => {
  if (value === '') {
    callback(new Error('请输入确认密码'))
  } else if (value !== form.value.password) {
    callback(new Error('两次输入密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 2, max: 20, message: '用户名长度在 2 到 20 个字符之间', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少为 6 个字符', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validatePassword, trigger: 'blur' }]
}

const handleRegister = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      await mailStore.register(form.value.email, form.value.username, form.value.password)
      ElMessage.success('注册成功，正在跳转...')
      router.push('/mail')
    } catch (error) {
      ElMessage.error(error instanceof Error ? error.message : '注册失败，请稍后重试')
    } finally {
      loading.value = false
    }
  })
}

const goToLogin = () => {
  router.push('/auth/login')
}
</script>

<template>
  <div class="register-container">
    <div class="register-card">
      <div class="logo">
        <h1>SmartMail</h1>
        <p>创建您的账户</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleRegister">
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            placeholder="邮箱地址"
            clearable
            type="email"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            clearable
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            placeholder="密码（至少 6 个字符）"
            type="password"
            show-password
            clearable
            size="large"
          />
        </el-form-item>

        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            placeholder="确认密码"
            type="password"
            show-password
            clearable
            size="large"
          />
        </el-form-item>

        <el-button type="primary" size="large" :loading="loading" @click="handleRegister" class="register-btn">
          注 册
        </el-button>
      </el-form>

      <div class="login-link">
        <span>已有账户？</span>
        <el-button link @click="goToLogin" type="primary">去登录</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.register-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.register-card {
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

.register-btn {
  width: 100%;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.login-link {
  text-align: center;
  font-size: 14px;
  color: #666;
}

.login-link :deep(.el-button) {
  padding: 0;
  font-size: 14px;
}

@media (max-width: 480px) {
  .register-card {
    max-width: 100%;
    margin: 20px;
    padding: 30px 20px;
  }

  .logo h1 {
    font-size: 24px;
  }
}
</style>
