// ============ Auth ============
export interface UserProfile {
  id: number
  email: string
  username: string
  status?: string
  settings?: UserSettings
}

export interface UserSettings {
  aiEnabled: boolean
  agentAutoWriteEnabled: boolean
}

// ============ Mailbox Items ============
/**
 * 邮箱项 - 对应后端 MailboxItemResponse
 * 代表用户对一封邮件的个人视图（已读、标星、优先级等）
 */
export interface MailboxItem {
  itemId: number
  mailId: number
  senderEmail: string
  subject: string
  preview: string
  folder: string
  read: boolean
  starred: boolean
  priority: string
  hasAttachment: boolean
  receivedAt: string // ISO 8601 format: "2026-06-10T10:30:00"
}

/**
 * 邮件详情 - 对应后端 MailDetailResponse
 * 包含完整的邮件内容和附件信息
 */
export interface MailDetail {
  mailId: number
  itemId: number
  messageNo: string
  senderEmail: string
  subject: string
  contentText: string
  contentHtml: string | null
  folder: string
  read: boolean
  starred: boolean
  priority: string
  sentAt: string
  recipients: string[]
  attachments: Attachment[]
  aiResults: Array<Record<string, unknown>>
}

/**
 * 附件 - 对应后端 AttachmentResponse
 */
export interface Attachment {
  id: number
  fileName: string
  mimeType: string
  fileSize: number
  downloadUrl: string
}

export interface PendingAttachment {
  pendingAttachmentId: number
  fileName: string
  mimeType: string
  fileSize: number
  status: string
}

export interface ThreadMessage {
  mailId: number
  senderEmail: string
  subject: string
  contentText: string
  sentAt: string
  parentMailId: number | null
  threadId: number | null
}

// ============ Send Mail ============
/**
 * 发送邮件的请求体 - 对应后端 SendMailRequest
 */
export interface SendMailPayload {
  to: string[]
  cc?: string[]
  bcc?: string[]
  subject: string
  contentText: string
  contentHtml?: string
  pendingAttachmentIds?: number[]
  parentMailId?: number
}

/**
 * 邮件发送响应 - 对应后端 MailSendResponse
 */
export interface MailSendResponse {
  mailId: number
  messageNo: string
  delivery: {
    delivered: string[]
    failed: string[]
  }
}

// ============ Mail synchronization ============
/** 对应后端 NotificationPollResponse。 */
export interface NotificationPollResponse {
  unreadCount: number
  newMailCount: number
  inboxCount: number
  junkCount: number
  unreadByLabel: Record<string, number>
  cursor: string
}

// ============ AI Agent ============
export interface AgentTaskResponse {
  task: string
  status: string
  result: Record<string, unknown>
  steps?: Array<{ name: string; detail: string }>
}

// ============ Workspace API ============
/**
 * 工作区视图项 - 对应后端 WorkspaceView
 */
export interface WorkspaceView {
  key: string
  label: string
  count: number
}

/**
 * 邮件分类 - 对应后端 CategoryResponse
 */
export interface Category {
  id: number
  name: string
  color?: string
  count?: number
}

/**
 * 工作区导航数据 - 对应后端 WorkspaceViewResponse
 */
export interface WorkspaceViewResponse {
  views: WorkspaceView[]
  folders: WorkspaceView[]
  categories: Category[]
}
