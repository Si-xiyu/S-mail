import axios from 'axios'
import type { AgentTaskResponse, MailDetail, MailboxItem, SendMailPayload, UserProfile } from '../types/mail'

const http = axios.create({
  baseURL: '',
  timeout: 5000
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem('smartmail_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const mockItems: MailboxItem[] = [
  {
    itemId: 1,
    mailId: 1,
    senderEmail: 'teacher@example.com',
    subject: '项目阶段汇报提醒',
    preview: '请各组在明天下午前提交项目进度，并准备 5 分钟阶段性演示。',
    folder: 'INBOX',
    read: false,
    starred: true,
    priority: 'HIGH',
    hasAttachment: false,
    receivedAt: new Date().toISOString()
  },
  {
    itemId: 2,
    mailId: 2,
    senderEmail: 'alice@smartmail.local',
    subject: 'Notion Mail 风格收件箱讨论',
    preview: '建议列表保持高信息密度，AI 摘要放在阅读区右侧。',
    folder: 'INBOX',
    read: true,
    starred: false,
    priority: 'NORMAL',
    hasAttachment: false,
    receivedAt: new Date(Date.now() - 3600_000).toISOString()
  }
]

export async function login(email: string, password: string): Promise<UserProfile> {
  try {
    const { data } = await http.post<ApiResponse<{ token: string; userId: number; email: string; username: string }>>('/api/v1/auth/login', { email, password })
    localStorage.setItem('smartmail_token', data.data.token)
    return { id: data.data.userId, email: data.data.email, username: data.data.username }
  } catch {
    localStorage.setItem('smartmail_token', 'mock-token')
    return { id: 1, email, username: 'Mock User' }
  }
}

export async function register(email: string, username: string, password: string): Promise<UserProfile> {
  try {
    const { data } = await http.post<ApiResponse<{ token: string; userId: number; email: string; username: string }>>('/api/v1/auth/register', { email, username, password })
    localStorage.setItem('smartmail_token', data.data.token)
    return { id: data.data.userId, email: data.data.email, username: data.data.username }
  } catch {
    localStorage.setItem('smartmail_token', 'mock-token')
    return { id: 1, email, username }
  }
}

export async function listMailbox(folder: string): Promise<MailboxItem[]> {
  try {
    const { data } = await http.get<ApiResponse<{ records: MailboxItem[] }>>('/api/v1/mailbox', { params: { folder } })
    return data.data.records
  } catch {
    return mockItems.filter((item) => item.folder === folder)
  }
}

export async function getMailDetail(mailId: number): Promise<MailDetail> {
  try {
    const { data } = await http.get<ApiResponse<MailDetail>>(`/api/v1/mails/${mailId}`)
    return data.data
  } catch {
    const item = mockItems.find((mail) => mail.mailId === mailId) ?? mockItems[0]
    return {
      ...item,
      messageNo: `MOCK-${mailId}`,
      contentText: `${item.preview}\n\n这是前端 mock 内容。后端启动后会自动切换到真实邮件详情接口。`,
      recipients: ['student@example.com'],
      aiResults: []
    }
  }
}

export async function sendMail(payload: SendMailPayload): Promise<void> {
  try {
    await http.post('/api/v1/mails', payload)
  } catch {
    await new Promise((resolve) => window.setTimeout(resolve, 300))
  }
}

export async function runAiTask(mailId: number, task: 'summary' | 'reply-draft' | 'analyze'): Promise<AgentTaskResponse> {
  try {
    const { data } = await http.post<ApiResponse<AgentTaskResponse>>(`/api/v1/ai/mails/${mailId}/${task}`)
    return data.data
  } catch {
    return {
      task,
      status: 'MOCK',
      result: task === 'reply-draft'
        ? { draft: '邮件已收到，我们会按要求推进并及时反馈。' }
        : { summary: ['需要提交项目进度', '准备阶段性演示'], priority: 'HIGH', labels: ['课程', '待处理'] }
    }
  }
}
