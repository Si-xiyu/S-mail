import axios, { AxiosError } from 'axios'
import type { AgentTaskResponse, MailDetail, MailboxItem, SendMailPayload, UserProfile, MailSendResponse } from '../types/mail'

// ============ HTTP 客户端配置 ============
const http = axios.create({
  baseURL: '/api/v1',
  timeout: 5000
})

// 请求拦截器：自动添加 Authorization header
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('smartmail_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器：统一处理错误
http.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('smartmail_token')
    }

    // 尝试从响应数据中获取错误信息
    const errorData = error.response?.data as any
    if (errorData?.message) {
      // 如果后端返回了 message，使用它
      const customError = new Error(errorData.message)
      throw customError
    }

    throw error
  }
)

// ============ API 响应类型 ============
interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface PageResponse<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
}

// ============ 认证 API ============
/**
 * 用户登录
 * @param email 邮箱
 * @param password 密码
 * @returns 用户信息和 token
 */
export async function login(email: string, password: string): Promise<UserProfile> {
  const { data } = await http.post<ApiResponse<{ token: string; userId: number; email: string; username: string }>>('/auth/login', {
    email,
    password
  })

  if (data.code !== 0) {
    throw new Error(data.message || '登录失败')
  }

  localStorage.setItem('smartmail_token', data.data.token)
  return {
    id: data.data.userId,
    email: data.data.email,
    username: data.data.username
  }
}

/**
 * 用户注册
 * @param email 邮箱
 * @param username 用户名
 * @param password 密码
 * @returns 用户信息和 token
 */
export async function register(email: string, username: string, password: string): Promise<UserProfile> {
  const { data } = await http.post<ApiResponse<{ token: string; userId: number; email: string; username: string }>>('/auth/register', {
    email,
    username,
    password
  })

  if (data.code !== 0) {
    throw new Error(data.message || '注册失败')
  }

  localStorage.setItem('smartmail_token', data.data.token)
  return {
    id: data.data.userId,
    email: data.data.email,
    username: data.data.username
  }
}

// ============ 邮箱 API ============
/**
 * 获取邮箱列表
 * @param folder 文件夹类型，如 INBOX、SENT、DRAFTS、TRASH、JUNK、STARRED
 * @param page 页码，默认 1
 * @param pageSize 每页数量，默认 20
 * @returns 邮箱项列表
 */
export async function listMailbox(
  folder: string = 'INBOX',
  page: number = 1,
  pageSize: number = 20
): Promise<MailboxItem[]> {
  const { data } = await http.get<ApiResponse<PageResponse<MailboxItem>>>('/mailbox', {
    params: { folder, page, pageSize }
  })

  if (data.code !== 0) {
    throw new Error(data.message)
  }

  return data.data.records || []
}

/**
 * 获取邮件详情
 * @param mailId 邮件 ID
 * @returns 邮件详情
 */
export async function getMailDetail(mailId: number): Promise<MailDetail> {
  const { data } = await http.get<ApiResponse<MailDetail>>(`/mails/${mailId}`)

  if (data.code !== 0) {
    throw new Error(data.message)
  }

  return data.data
}

/**
 * 发送邮件
 * @param payload 邮件内容
 * @returns 邮件发送响应（包含 mailId 和 messageNo）
 */
export async function sendMail(payload: SendMailPayload): Promise<MailSendResponse> {
  const { data } = await http.post<ApiResponse<MailSendResponse>>('/mails', payload)

  if (data.code !== 0) {
    throw new Error(data.message)
  }

  return data.data
}

/**
 * 标记邮件为已读/未读
 * @param itemId 邮箱项 ID
 * @param read 是否已读
 */
export async function markMailRead(itemId: number, read: boolean): Promise<void> {
  const { data } = await http.patch<ApiResponse<void>>(`/mailbox/items/${itemId}/read`, { read })

  if (data.code !== 0) {
    throw new Error(data.message)
  }
}

/**
 * 标记邮件星标
 * @param itemId 邮箱项 ID
 * @param starred 是否星标
 */
export async function starMail(itemId: number, starred: boolean): Promise<void> {
  const { data } = await http.patch<ApiResponse<void>>(`/mailbox/items/${itemId}/star`, { starred })

  if (data.code !== 0) {
    throw new Error(data.message)
  }
}

/**
 * 删除邮件
 * @param itemId 邮箱项 ID
 */
export async function deleteMail(itemId: number): Promise<void> {
  const { data } = await http.delete<ApiResponse<void>>(`/mailbox/items/${itemId}`)

  if (data.code !== 0) {
    throw new Error(data.message)
  }
}

/**
 * 移动邮件到指定文件夹
 * @param itemId 邮箱项 ID
 * @param folder 目标文件夹
 */
export async function moveMail(itemId: number, folder: string): Promise<void> {
  const { data } = await http.post<ApiResponse<void>>(`/mailbox/items/${itemId}/move`, { folder })

  if (data.code !== 0) {
    throw new Error(data.message)
  }
}

// ============ AI 任务 API ============
/**
 * 运行 AI 任务
 * @param mailId 邮件 ID
 * @param task 任务类型：summary（摘要）、reply-draft（回复草稿）、analyze（分析）
 * @returns AI 任务响应
 */
export async function runAiTask(
  mailId: number,
  task: 'summary' | 'reply-draft' | 'analyze'
): Promise<AgentTaskResponse> {
  const { data } = await http.post<ApiResponse<AgentTaskResponse>>(`/ai/mails/${mailId}/${task}`)

  if (data.code !== 0) {
    throw new Error(data.message)
  }

  return data.data
}
