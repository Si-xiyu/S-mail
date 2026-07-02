import { createRouter, createWebHistory, RouteRecordRaw, NavigationGuardNext, RouteLocationNormalized } from 'vue-router'
import { useMailStore } from './stores/mailStore'
import AuthLayout from './pages/AuthLayout.vue'
import LoginPage from './pages/LoginPage.vue'
import RegisterPage from './pages/RegisterPage.vue'
import MailApp from './pages/MailApp.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/auth',
    component: AuthLayout,
    children: [
      {
        path: 'login',
        component: LoginPage
      },
      {
        path: 'register',
        component: RegisterPage
      }
    ]
  },
  {
    path: '/mail',
    component: MailApp
  },
  {
    path: '/',
    redirect: () => {
      const token = localStorage.getItem('smartmail_token')
      return token ? '/mail' : '/auth/login'
    }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：认证检查
router.beforeEach(
  (to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    const mailStore = useMailStore()
    const token = localStorage.getItem('smartmail_token')

    // 初始化用户信息
    if (token && !mailStore.user) {
      mailStore.initializeUser()
    }

    // 访问认证页面时，如果已登录则重定向到邮件应用
    if (to.path.startsWith('/auth') && token) {
      next('/mail')
      return
    }

    // 访问邮件应用时，如果未登录则重定向到登录页面
    if (to.path.startsWith('/mail') && !token) {
      next('/auth/login')
      return
    }

    next()
  }
)

export default router
