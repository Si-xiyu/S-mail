export interface UserProfile {
  id: number
  email: string
  username: string
}

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
  receivedAt: string
}

export interface MailDetail extends MailboxItem {
  messageNo: string
  contentText: string
  contentHtml?: string
  recipients: string[]
  aiResults: Array<Record<string, unknown>>
}

export interface SendMailPayload {
  to: string[]
  cc: string[]
  subject: string
  contentText: string
  contentHtml?: string
}

export interface AgentTaskResponse {
  task: string
  status: string
  result: Record<string, unknown>
  steps?: Array<{ name: string; detail: string }>
}
