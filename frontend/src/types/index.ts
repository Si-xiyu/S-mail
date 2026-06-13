// 邮件主数据
export interface Mail {
  id: string
  subject: string
  senderName: string
  senderEmail: string
  to: string[]
  cc?: string[]
  bcc?: string[]
  content: string
  timestamp: number
  read: boolean
  starred: boolean
  labels: string[]
  attachments?: Attachment[]
}

// 用户视图中的邮件项
export interface MailItem {
  id: string
  subject: string
  senderName: string
  senderEmail: string
  preview: string
  timestamp: number
  read: boolean
  starred: boolean
  labels: string[]
  hasAttachment: boolean
}

// 附件
export interface Attachment {
  id: string
  filename: string
  size: number
  mimeType: string
}

// 用户
export interface User {
  id: string
  email: string
  name: string
  avatar?: string
}

// 标签
export interface Label {
  id: string
  name: string
  color?: string
  count: number
}
