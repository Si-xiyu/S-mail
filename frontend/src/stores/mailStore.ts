import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Mail, MailItem, User, Label } from '../types'
import type {
  MailboxItem,
  MailDetail,
  MailSendResponse,
  NotificationPollResponse,
  SendMailPayload,
  ThreadMessage,
  UserProfile
} from '../types/mail'
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

  // 邮件线程缓存
  const mailThreadCache = ref<Map<number, ThreadMessage[]>>(new Map())

  // 加载状态
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const total = ref(0)
  const page = ref(1)
  const pageSize = ref(20)
  const searchQuery = ref('')
  const unreadCount = ref(0)

  // ============ 计算属性 ============

  // 获取当前标签的邮件列表
  const currentMailItems = computed((): MailboxItem[] => {
    // STARRED 是虚拟视图，由前端过滤
    if (currentLabel.value === 'STARRED') {
      return mailboxItems.value.filter(m => m.starred)
    }
    // 其他情况（标准文件夹或自定义分类），从后端已经过滤好的 mailboxItems 中直接使用
    // 因为 loadMailbox() 会根据 folder 或 categoryId 从后端加载正确的邮件
    return mailboxItems.value
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
      // 加载分类
      await initializeCategories()
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
      // 加载分类
      await initializeCategories()
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
  const initializeUser = async (): Promise<boolean> => {
    const token = localStorage.getItem('smartmail_token')
    if (!token) {
      user.value = null
      return false
    }

    try {
      const profile = await apiClient.getCurrentUser()
      user.value = {
        id: profile.id.toString(),
        email: profile.email,
        name: profile.username
      }
      localStorage.setItem('smartmail_user', JSON.stringify(user.value))
      await Promise.all([loadMailbox('INBOX', 1, 20, true), initializeCategories()])
      return true
    } catch (err) {
      console.error('Failed to restore authenticated session:', err)
      if (err instanceof apiClient.ApiError && err.status === 401) {
        logout()
        return false
      }

      const cachedUser = localStorage.getItem('smartmail_user')
      if (cachedUser) {
        try {
          user.value = JSON.parse(cachedUser)
          error.value = '暂时无法连接服务器，请稍后刷新重试'
          return true
        } catch {
          user.value = null
        }
      }
      return false
    }
  }

  /**
   * 加载邮箱列表
   */
  const loadMailbox = async (
    folder: string = 'INBOX',
    requestedPage: number = 1,
    requestedPageSize: number = 20,
    propagateError: boolean = false
  ): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      const result = await apiClient.listMailbox(folder, requestedPage, requestedPageSize)
      mailboxItems.value = result.records
      total.value = result.total
      pageSize.value = result.pageSize
      page.value = result.page
      currentLabel.value = folder

      // 更新标签计数
      const label = labels.value.find(l => l.id === folder)
      if (label) {
        label.count = result.total
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载邮箱失败'
      if (propagateError) {
        throw err
      }
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 加载所有标星邮件
   */
  const loadStarredMails = async (requestedPage: number = 1): Promise<void> => {
    isLoading.value = true
    error.value = null
    try {
      const result = await apiClient.searchMailbox(
        '', undefined, undefined, true, requestedPage, pageSize.value
      )
      mailboxItems.value = result.records
      total.value = result.total
      page.value = result.page
      pageSize.value = result.pageSize
      currentLabel.value = 'STARRED'
    } catch (err) {
      error.value = err instanceof Error ? err.message : '加载星标邮件失败'
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
  const sendMessage = async (payload: SendMailPayload): Promise<MailSendResponse> => {
    isLoading.value = true
    error.value = null
    try {
      const result = await apiClient.sendMail(payload)
      mailThreadCache.value.clear()
      mailDetailCache.value.clear()
      await loadMailbox(currentLabel.value, page.value, pageSize.value)
      return result
    } catch (err) {
      error.value = err instanceof Error ? err.message : '发送邮件失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const sendMail = async (
    to: string[],
    subject: string,
    contentText: string,
    cc: string[] = [],
    contentHtml?: string,
    parentMailId?: number
  ): Promise<void> => {
    await sendMessage({ to, cc, subject, contentText, contentHtml, parentMailId })
  }

  const searchMailbox = async (keyword: string, requestedPage: number = 1): Promise<void> => {
    const trimmed = keyword.trim()
    searchQuery.value = trimmed
    if (!trimmed) {
      if (currentLabel.value === 'STARRED') {
        await loadStarredMails(requestedPage)
      } else {
        await loadMailbox(currentLabel.value, requestedPage, pageSize.value)
      }
      return
    }
    isLoading.value = true
    error.value = null
    try {
      const folder = /^[A-Z]+$/.test(currentLabel.value) && currentLabel.value !== 'STARRED'
        ? currentLabel.value
        : undefined
      const categoryId = /^\d+$/.test(currentLabel.value) ? Number(currentLabel.value) : undefined
      const starred = currentLabel.value === 'STARRED' ? true : undefined
      const result = await apiClient.searchMailbox(
        trimmed, folder, categoryId, starred, requestedPage, pageSize.value
      )
      mailboxItems.value = result.records
      total.value = result.total
      page.value = result.page
    } catch (err) {
      error.value = err instanceof Error ? err.message : '搜索邮件失败'
      throw err
    } finally {
      isLoading.value = false
    }
  }

  const refreshCurrent = async (): Promise<void> => {
    if (searchQuery.value) {
      await searchMailbox(searchQuery.value, page.value)
    } else if (currentLabel.value === 'STARRED') {
      await loadStarredMails(page.value)
    } else {
      await loadMailbox(currentLabel.value, page.value, pageSize.value)
    }
  }

  const applyNotificationCounts = (notification: NotificationPollResponse): void => {
    unreadCount.value = notification.unreadCount
    const inbox = labels.value.find(label => label.id === 'INBOX')
    const junk = labels.value.find(label => label.id === 'JUNK')
    if (inbox) inbox.count = notification.inboxCount
    if (junk) junk.count = notification.junkCount
  }

  const refreshDraftCount = (): void => {
    const draft = labels.value.find(label => label.id === 'DRAFTS')
    if (!draft) return
    const key = `smartmail_draft_${user.value?.id || 'anonymous'}`
    const raw = localStorage.getItem(key)
    if (!raw) {
      draft.count = 0
      return
    }
    try {
      const value = JSON.parse(raw) as Record<string, unknown>
      const hasText = ['to', 'cc', 'bcc', 'subject', 'content']
        .some(field => typeof value[field] === 'string' && value[field].trim().length > 0)
      const hasAttachments = Array.isArray(value.attachments) && value.attachments.length > 0
      draft.count = hasText || hasAttachments ? 1 : 0
    } catch {
      localStorage.removeItem(key)
      draft.count = 0
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
      throw err
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
      throw err
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
      throw err
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
      throw err
    }
  }

  /**
   * 添加新标签
   */
  const addLabel = async (labelName: string): Promise<void> => {
    error.value = null
    try {
      // 调用后端 API 创建分类
      const category = await apiClient.createCategory(labelName, '#667eea')

      // 添加到本地状态
      labels.value.push({
        id: category.id.toString(),
        name: category.name,
        count: 0,
        color: category.color
      })
    } catch (err) {
      error.value = err instanceof Error ? err.message : '添加标签失败'
      throw err
    }
  }

  /**
   * 删除标签
   */
  const deleteLabel = async (labelId: string): Promise<void> => {
    error.value = null
    try {
      const categoryId = parseInt(labelId, 10)
      if (isNaN(categoryId)) {
        // 如果不是数字 ID，直接删除本地状态
        const index = labels.value.findIndex(l => l.id === labelId)
        if (index > -1) {
          labels.value.splice(index, 1)
        }
      } else {
        // 调用后端 API 删除分类
        await apiClient.deleteCategory(categoryId)
        // 删除本地状态
        const index = labels.value.findIndex(l => l.id === labelId)
        if (index > -1) {
          labels.value.splice(index, 1)
        }
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '删除标签失败'
      throw err
    }
  }

  /**
   * 获取邮件线程/会话（兼容方法）
   * 返回整个线程中的所有邮件
   */
  const getMailThread = async (mailId: number, force: boolean = false): Promise<ThreadMessage[]> => {
    // 先检查缓存
    const cached = mailThreadCache.value.get(mailId)
    if (cached && !force) {
      return cached
    }

    isLoading.value = true
    error.value = null
    try {
      let thread: ThreadMessage[] = []

      // 调用线程 API
      try {
        thread = await apiClient.getMailThread(mailId)
      } catch (err) {
        console.error('Failed to load mail thread:', err)
        // 至少返回当前邮件
        const current = await getMailDetail(mailId)
        if (current) {
          thread = [{
            mailId: current.mailId,
            senderEmail: current.senderEmail,
            subject: current.subject,
            contentText: current.contentText,
            sentAt: current.sentAt,
            parentMailId: null,
            threadId: null
          }]
        }
      }

      // 按发送时间排序（升序）
      thread.sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime())

      mailThreadCache.value.set(mailId, thread)
      return thread
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取邮件线程失败'
      return []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 获取邮件的对话路径（从起点到当前邮件）
   * 相比 getMailThread，只返回需要显示的对话链
   */
  const getMailPath = async (mailId: number, force: boolean = false): Promise<ThreadMessage[]> => {
    // 先检查缓存
    const cached = mailThreadCache.value.get(mailId)
    if (cached && !force) {
      return cached
    }

    isLoading.value = true
    error.value = null
    try {
      let path: ThreadMessage[] = []

      // 调用对话路径 API
      try {
        path = await apiClient.getMailPath(mailId)
      } catch (err) {
        console.error('Failed to load mail path:', err)
        // 至少返回当前邮件
        const current = await getMailDetail(mailId)
        if (current) {
          path = [{
            mailId: current.mailId,
            senderEmail: current.senderEmail,
            subject: current.subject,
            contentText: current.contentText,
            sentAt: current.sentAt,
            parentMailId: null,
            threadId: null
          }]
        }
      }

      // 按发送时间排序（升序）
      path.sort((a, b) => new Date(a.sentAt).getTime() - new Date(b.sentAt).getTime())

      mailThreadCache.value.set(mailId, path)
      return path
    } catch (err) {
      error.value = err instanceof Error ? err.message : '获取邮件对话路径失败'
      return []
    } finally {
      isLoading.value = false
    }
  }

  /**
   * 切换标签/文件夹
   */
  const selectLabel = (label: string): void => {
    currentLabel.value = label
    // STARRED 需要从后端加载所有标星邮件（跨 folder）
    if (label === 'STARRED') {
      loadStarredMails()
    } else {
      // 其他标签（folder 或 category ID）从后端加载
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
      itemId: item.itemId,
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

  /**
   * 将邮件添加到分类
   */
  const changeCategory = async (id: string, categoryId: string): Promise<void> => {
    error.value = null
    try {
      const mailId = parseInt(id, 10)
      const catId = parseInt(categoryId, 10)
      const item = mailboxItems.value.find(m => m.mailId === mailId)
      if (item) {
        await apiClient.changeCategory(item.itemId, catId)
      }
    } catch (err) {
      error.value = err instanceof Error ? err.message : '添加到分类失败'
      throw err
    }
  }

  /**
   * 初始化：加载分类列表
   */
  const initializeCategories = async (): Promise<void> => {
    error.value = null
    try {
      const categories = await apiClient.listCategories()
      // 将后端的分类转换为前端的标签格式
      const customLabels = categories.map(cat => ({
        id: cat.id.toString(),
        name: cat.name,
        count: 0,
        color: cat.color
      }))
      // 替换现有的自定义标签（保留标准的文件夹标签）
      const standardLabelIds = ['INBOX', 'STARRED', 'SENT', 'DRAFTS', 'TRASH', 'SPAM', 'JUNK']
      labels.value = labels.value.filter(l => standardLabelIds.includes(l.id)).concat(customLabels)
    } catch (err) {
      console.error('Failed to load categories:', err)
    }
  }

  return {
    // 状态
    user,
    currentLabel,
    labels,
    mailboxItems,
    mailDetailCache,
    mailThreadCache,
    isLoading,
    error,
    total,
    page,
    pageSize,
    searchQuery,
    unreadCount,

    // 计算属性
    currentMailItems,
    mailItems,

    // 核心方法（新 API）
    login,
    register,
    logout,
    initializeUser,
    loadMailbox,
    loadStarredMails,
    getMailDetail,
    getMailThread,
    getMailPath,
    sendMail,
    sendMessage,
    searchMailbox,
    refreshCurrent,
    applyNotificationCounts,
    refreshDraftCount,
    markMailRead,
    starMail,
    deleteMail: deleteMailCompat,
    moveMail,
    changeCategory,
    selectLabel,
    addLabel,
    deleteLabel,
    initializeCategories,

    // 向后兼容的方法
    getMailById,
    markAsRead,
    toggleStar,
    moveToLabel
  }
})
