// ============ Legacy Types (for Pinia Store compatibility) ============
// 这些类型用于维持现有 mailStore 的兼容性
// 新代码应该优先使用 mail.ts 中的类型

export interface Mail {
  id: string
  itemId?: number
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

export interface Attachment {
  id: string
  filename: string
  size: number
  mimeType: string
}

export interface User {
  id: string
  email: string
  name: string
  avatar?: string
}

export interface Label {
  id: string
  name: string
  color?: string
  count: number
}
