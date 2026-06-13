import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Mail, MailItem, User, Label } from '../types'

export const useMailStore = defineStore('mail', () => {
  // 当前用户
  const user = ref<User>({
    id: '1',
    email: 'john.doe@smartmail.local',
    name: 'John Doe',
    avatar: 'https://i.pravatar.cc/150?img=1'
  })

  // 当前选择的标签/文件夹
  const currentLabel = ref<string>('INBOX')

  // 所有标签
  const labels = ref<Label[]>([
    { id: 'INBOX', name: '收件箱', count: 5, color: '#1f2937' },
    { id: 'STARRED', name: '标星', count: 2, color: '#f59e0b' },
    { id: 'SENT', name: '已发送', count: 8, color: '#10b981' },
    { id: 'DRAFTS', name: '草稿', count: 1, color: '#8b5cf6' },
    { id: 'TRASH', name: '垃圾箱', count: 3, color: '#ef4444' },
    { id: 'SPAM', name: '垃圾邮件', count: 5, color: '#6b7280' }
  ])

  // 模拟邮件数据
  const mockMails = ref<Mail[]>([
    {
      id: '1',
      subject: 'Welcome to SmartMail',
      senderName: 'Alice Johnson',
      senderEmail: 'alice@company.com',
      to: ['john.doe@smartmail.local'],
      content: 'Welcome to SmartMail! We are excited to have you on board. This is a modern email system with AI capabilities. You can send, receive, and manage emails efficiently.',
      timestamp: Date.now() - 3600000,
      read: false,
      starred: true,
      labels: ['INBOX'],
      attachments: [{ id: 'a1', filename: 'onboarding.pdf', size: 245000, mimeType: 'application/pdf' }]
    },
    {
      id: '2',
      subject: 'Project Update - Q2 Results',
      senderName: 'Bob Smith',
      senderEmail: 'bob@company.com',
      to: ['john.doe@smartmail.local'],
      content: 'Hi John,\n\nPlease find attached the Q2 results summary. Our team has achieved 120% of the quarterly targets. The project is on track and we expect to deliver ahead of schedule.',
      timestamp: Date.now() - 7200000,
      read: false,
      starred: false,
      labels: ['INBOX'],
      attachments: [{ id: 'a2', filename: 'Q2_Results.xlsx', size: 512000, mimeType: 'application/vnd.ms-excel' }]
    },
    {
      id: '3',
      subject: 'Meeting Tomorrow at 2 PM',
      senderName: 'Carol White',
      senderEmail: 'carol@company.com',
      to: ['john.doe@smartmail.local'],
      cc: ['bob@company.com'],
      content: 'Hi Team,\n\nDon\'t forget about our meeting tomorrow at 2 PM in the conference room. We\'ll be discussing the upcoming product launch and timeline.',
      timestamp: Date.now() - 86400000,
      read: true,
      starred: false,
      labels: ['INBOX']
    },
    {
      id: '4',
      subject: 'Your order has been shipped',
      senderName: 'Amazon',
      senderEmail: 'orders@amazon.com',
      to: ['john.doe@smartmail.local'],
      content: 'Your order #12345 has been shipped. Tracking number: 1Z999AA10123456784. Estimated delivery: Tomorrow.',
      timestamp: Date.now() - 172800000,
      read: true,
      starred: false,
      labels: ['INBOX']
    },
    {
      id: '5',
      subject: 'Verify your email address',
      senderName: 'SmartMail Team',
      senderEmail: 'support@smartmail.local',
      to: ['john.doe@smartmail.local'],
      content: 'Please click the link below to verify your email address and complete your registration.',
      timestamp: Date.now() - 259200000,
      read: true,
      starred: false,
      labels: ['INBOX']
    },
    {
      id: '6',
      subject: 'Re: Project Update - Q2 Results',
      senderName: 'John Doe',
      senderEmail: 'john.doe@smartmail.local',
      to: ['bob@company.com'],
      content: 'Hi Bob,\n\nThank you for the Q2 results. They look great! Let\'s schedule a follow-up meeting to discuss the implementation plan for the next quarter.',
      timestamp: Date.now() - 3600000,
      read: true,
      starred: false,
      labels: ['SENT']
    },
    {
      id: '7',
      subject: 'Draft: Feature Request',
      senderName: '',
      senderEmail: '',
      to: ['team@company.com'],
      content: 'Hi Team,\n\nI have a feature request for the next sprint. Can we add support for...',
      timestamp: Date.now() - 7200000,
      read: true,
      starred: false,
      labels: ['DRAFTS']
    }
  ])

  // 所有邮件
  const mails = ref<Mail[]>(mockMails.value)

  // 获取当前标签的邮件列表
  const currentMails = computed(() => {
    if (currentLabel.value === 'STARRED') {
      return mails.value.filter(m => m.starred)
    }
    return mails.value.filter(m => m.labels.includes(currentLabel.value))
  })

  // 转换为邮件项（列表显示）
  const mailItems = computed(() => {
    return currentMails.value.map(mail => ({
      id: mail.id,
      subject: mail.subject,
      senderName: mail.senderName,
      senderEmail: mail.senderEmail,
      preview: mail.content.substring(0, 100).replace(/\n/g, ' '),
      timestamp: mail.timestamp,
      read: mail.read,
      starred: mail.starred,
      labels: mail.labels,
      hasAttachment: !!mail.attachments?.length
    } as MailItem))
  })

  // 获取单个邮件详情
  const getMailById = (id: string) => {
    return mails.value.find(m => m.id === id)
  }

  // 标记邮件为已读
  const markAsRead = (id: string) => {
    const mail = mails.value.find(m => m.id === id)
    if (mail) {
      mail.read = true
    }
  }

  // 切换星标
  const toggleStar = (id: string) => {
    const mail = mails.value.find(m => m.id === id)
    if (mail) {
      mail.starred = !mail.starred
    }
  }

  // 删除邮件
  const deleteMail = (id: string) => {
    const mail = mails.value.find(m => m.id === id)
    if (mail) {
      mail.labels = ['TRASH']
    }
  }

  // 移动邮件到标签
  const moveToLabel = (id: string, label: string) => {
    const mail = mails.value.find(m => m.id === id)
    if (mail) {
      mail.labels = [label]
    }
  }

  // 发送邮件
  const sendMail = (to: string[], subject: string, content: string, cc?: string[]) => {
    const newMail: Mail = {
      id: Date.now().toString(),
      subject,
      senderName: user.value.name,
      senderEmail: user.value.email,
      to,
      cc,
      content,
      timestamp: Date.now(),
      read: true,
      starred: false,
      labels: ['SENT']
    }
    mails.value.push(newMail)
  }

  // 切换标签
  const selectLabel = (label: string) => {
    currentLabel.value = label
  }

  return {
    user,
    currentLabel,
    labels,
    mails,
    mailItems,
    getMailById,
    markAsRead,
    toggleStar,
    deleteMail,
    moveToLabel,
    sendMail,
    selectLabel
  }
})
