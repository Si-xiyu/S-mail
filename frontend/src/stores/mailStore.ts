import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Mail, MailItem, User, Label } from '../types'
import type { MailboxItem, MailDetail, UserProfile } from '../types/mail'
import * as apiClient from '../api/client'

export const useMailStore = defineStore('mail', () => {
  // ============ 状态 ============

  // 当前用户
  const user = ref<User | null>(null)

  // 当前选择的标签/文件夹
  const currentLabel = ref<string>('INBOX')

  // 所有标签
  const labels = ref<Label[]>([
    { id: 'INBOX', name: '收件箱', count: 0, color: '#1f2937' },
    { id: 'STARRED', name: '标星', count: 0, color: '#f59e0b' },
    { id: 'SENT', name: '已发送', count: 0, color: '#10b981' },
    { id: 'DRAFTS', name: '草稿', count: 0, color: '#8b5cf6' },
    { id: 'TRASH', name: '垃圾箱', count: 0, color: '#ef4444' },
    { id: 'JUNK', name: '垃圾邮件', count: 0, color: '#6b7280' }
  ])

  // 真实邮件数据：从后端获取的 MailboxItem
  const mailboxItems = ref<MailboxItem[]>([])

  // 单个邮件详情缓存
  const mailDetailCache = ref<Map<number, MailDetail>>(new Map())

  // 加载状态
  const isLoading = ref(false)
  const error = ref<string | null>(null)

  // ============ 计算属性 ============

  // 获取当前标签的邮件列表
  const currentMailItems = computed((): MailboxItem[] => {
    if (currentLabel.value === 'STARRED') {
      return mailboxItems.value.filter(m => m.starred)
    }
    return mailboxItems.value.filter(m => m.folder === currentLabel.value)
  })

  // 转换为旧格式的 MailItem 用于向后兼容
  const mailItems = computed((): MailItem[] => {
    return currentMailItems.value.map(item => ({
      id: item.mailId.toString(),
      subject: item.subject,
      senderName: '', // 后端没有提供 senderName，使用 senderEmail 代替
      senderEmail: item.senderEmail,
      preview: item.preview,
      timestamp: new Date(item.receivedAt).getTime(),
      read: item.read,
      starred: item.starred,
      labels: [item.folder],
      hasAttachment: item.hasAttachment
    } as MailItem))
  })

  // ============ 方法 ============

  /**
   * 登录
   */
  const login = async (email: string, password: string): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      const userProfile = await apiClient.login(email, password)
      user.value = {
        id: userProfile.id.toString(),
        email: userProfile.email,
        name: userProfile.username
      }
      localStorage.setItem('smartmail_user', JSON.stringify(user.value))
      await loadMailbox('INBOX')
    } catch (err) {
      error.value = err instanceof Error ? err.message : '登录失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 注册
   */
  const register = async (email: string, username: string, password: string): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      const userProfile = await apiClient.register(email, username, password)
      user.value = {
        id: userProfile.id.toString(),
        email: userProfile.email,
        name: userProfile.username
      }
      localStorage.setItem('smartmail_user', JSON.stringify(user.value))
      await loadMailbox('INBOX')
    } catch (err) {
      error.value = err instanceof Error ? err.message : '注册失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 登出
   */
  const logout = (): void => {
    localStorage.removeItem('smartmail_token')
    localStorage.removeItem('smartmail_user')
    user.value = null
    mailboxItems.value = []
    mailDetailCache.value.clear()
    currentLabel.value = 'INBOX'
    error.value = null
  }

  /**
   * 初始化用户状态（从 localStorage 恢复）
   */
  const initializeUser = (): void => {
    const token = localStorage.getItem('smartmail_token')
    if (token) {
      const userStr = localStorage.getItem('smartmail_user')
      if (userStr) {
        try {
          user.value = JSON.parse(userStr)
        } catch (err) {
          console.error('Failed to restore user from localStorage:', err)
          user.value = null
        }
      }
    }
  }

  /**
   * 加载邮箱列表
   */
  const loadMailbox = async (folder: string = 'INBOX', page: number = 1, pageSize: number = 20): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      const items = await apiClient.listMailbox(folder, page, pageSize)
      mailboxItems.value = items
      currentLabel.value = folder

      // 更新标签计数
      const label = labels.value.find(l => l.id === folder)
      if (label) {
        label.count = items.length
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载邮箱失败'
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取邮件详情
   */
  const getMailDetail = async (mailId: number): Promise<MailDetail | null> => {
    // 先检查缓存
    const cached = mailDetailCache.value.get(mailId)
    if (cached) {
      return cached
    }

    isLoading.value = true
    error.value = null
    try {
      const detail = await apiClient.getMailDetail(mailId)
      mailDetailCache.value.set(mailId, detail)

      // 更新对应的 mailboxItem 的已读状态
      const item = mailboxItems.value.find(m => m.mailId === mailId)
      if (item) {
        item.read = true
      }

      return detail
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取邮件详情失败'
      return null
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 发送邮件
   */
  const sendMail = async (to: string[], subject: string, contentText: string, cc: string[] = [], contentHtml?: string): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      await apiClient.sendMail({
        to,
        cc,
        subject,
        contentText,
        contentHtml
      })
      // 发送后重新加载已发送列表
      await loadMailbox('SENT')
    } catch (err) {
      error.value = err instanceof Error ? err.message : '发送邮件失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 标记邮件为已读/未读
   */
  const markMailRead = async (itemId: number, read: boolean): Promise<void> => {
    error.value = null
    try {
      await apiClient.markMailRead(itemId, read)
      // 更新本地状态
      const item = mailboxItems.value.find(m => m.itemId === itemId)
      if (item) {
        item.read = read
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新邮件状态失败'
    }
  }

  /**
   * 标记邮件星标
   */
  const starMail = async (itemId: number, starred: boolean): Promise<void> => {
    error.value = null
    try {
      await apiClient.starMail(itemId, starred)
      // 更新本地状态
      const item = mailboxItems.value.find(m => m.itemId === itemId)
      if (item) {
        item.starred = starred
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '更新星标状态失败'
    }
  }

  /**
   * 删除邮件
   */
  const deleteMail = async (itemId: number): Promise<void> => {
    error.value = null
    try {
      await apiClient.deleteMail(itemId)
      // 删除邮件后重新加载当前文件夹的邮件列表
      // 这样可以正确显示邮件已移到回收站
      await loadMailbox(currentLabel.value)
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除邮件失败'
    }
  }

  /**
   * 移动邮件到指定文件夹
   */
  const moveMail = async (itemId: number, folder: string): Promise<void> => {
    error.value = null
    try {
      await apiClient.moveMail(itemId, folder)
      // 更新本地状态
      const item = mailboxItems.value.find(m => m.itemId === itemId)
      if (item) {
        item.folder = folder
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '移动邮件失败'
    }
  }

  /**
   * 添加新标签
   */
  const addLabel = async (labelName: string): Promise<void> => {
    error.value = null
    try {
      // 生成标签 ID（大写，空格转换为下划线）
      const labelId = labelName.toUpperCase().replace(/\s+/g, '_')

      // 检查是否已存在相同的标签
      if (labels.value.some(l => l.id === labelId)) {
        throw new Error('标签已存在')
      }

      // 添加新标签到本地状态
      labels.value.push({
        id: labelId,
        name: labelName,
        count: 0,
        color: '#667eea'
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : '添加标签失败'
      throw err
    }
  }

  /**
   * 切换标签/文件夹
   */
  const selectLabel = (label: string): void => {
    currentLabel.value = label
    // 只对真实文件夹加载邮件，STARRED 是虚拟视图，由前端计算属性过滤
    if (label !== 'STARRED') {
      loadMailbox(label)
    }
  }

  /**
   * 获取单个邮件（向后兼容）
   */
  const getMailById = (id: string): Mail | null => {
    const mailId = parseInt(id, 10)
    const item = mailboxItems.value.find(m => m.mailId === mailId)
    if (!item) return null

    return {
      id: item.mailId.toString(),
      subject: item.subject,
      senderName: '',
      senderEmail: item.senderEmail,
      to: [],
      content: item.preview,
      timestamp: new Date(item.receivedAt).getTime(),
      read: item.read,
      starred: item.starred,
      labels: [item.folder]
    }
  }

  /**
   * 标记为已读（向后兼容）
   */
  const markAsRead = async (id: string): Promise<void> => {
    const item = mailboxItems.value.find(m => m.mailId === parseInt(id, 10))
    if (item) {
      await markMailRead(item.itemId, true)
    }
  }

  /**
   * 切换星标（向后兼容）
   */
  const toggleStar = async (id: string): Promise<void> => {
    const item = mailboxItems.value.find(m => m.mailId === parseInt(id, 10))
    if (item) {
      await starMail(item.itemId, !item.starred)
    }
  }

  /**
   * 删除邮件（向后兼容）
   */
  const deleteMailCompat = async (id: string): Promise<void> => {
    const item = mailboxItems.value.find(m => m.mailId === parseInt(id, 10))
    if (item) {
      await deleteMail(item.itemId)
    }
  }

  /**
   * 移动到标签（向后兼容）
   */
  const moveToLabel = async (id: string, label: string): Promise<void> => {
    const item = mailboxItems.value.find(m => m.mailId === parseInt(id, 10))
    if (item) {
      await moveMail(item.itemId, label)
    }
  }

  return {
    // 状态
    user,
    currentLabel,
    labels,
    mailboxItems,
    mailDetailCache,
    isLoading,
    error,

    // 计算属性
    currentMailItems,
    mailItems,

    // 核心方法（新 API）
    login,
    register,
    logout,
    initializeUser,
    loadMailbox,
    getMailDetail,
    sendMail,
    markMailRead,
    starMail,
    deleteMail: deleteMailCompat,
    moveMail,
    selectLabel,
    addLabel,

    // 向后兼容的方法
    getMailById,
    markAsRead,
    toggleStar,
    moveToLabel
  }
})
