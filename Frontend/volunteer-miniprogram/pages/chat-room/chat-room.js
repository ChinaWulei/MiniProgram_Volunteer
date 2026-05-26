const app = getApp()
const { request } = require('../../utils/request')

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

Page({
  data: {
    conversationId: null,
    peerId: null,
    peerName: '',
    currentUserId: null,
    messages: [],
    inputValue: '',
    scrollIntoView: '',
    socketOpen: false
  },
  onLoad(options) {
    const user = app.globalData.user || wx.getStorageSync('user') || {}
    this.setData({ conversationId: Number(options.conversationId), peerId: Number(options.peerId), peerName: decodeURIComponent(options.peerName || '学院同学'), currentUserId: user.id })
    wx.setNavigationBarTitle({ title: this.data.peerName })
    this.loadMessages()
    this.connectSocket()
  },
  onUnload() {
    if (this.socket) this.socket.close()
  },
  connectSocket() {
    const token = app.globalData.token || wx.getStorageSync('token')
    const base = app.globalData.baseUrl.replace(/^http/, 'ws')
    this.socket = wx.connectSocket({ url: `${base}/ws/chat?token=${encodeURIComponent(token)}&userId=${this.data.currentUserId}` })
    this.socket.onOpen(() => this.setData({ socketOpen: true }))
    this.socket.onMessage(() => this.loadMessages())
  },
  loadMessages() {
    request({ url: `/api/chat/conversations/${this.data.conversationId}/messages` })
      .then(list => {
        const messages = (list || []).map(item => {
          const isMine = item.senderId === this.data.currentUserId
          return Object.assign({}, item, {
            isMine,
            avatarText: isMine ? '我' : (this.data.peerName || '志').substring(0, 1),
            timeText: formatTime(item.createdAt),
            statusText: item.inviteStatus === 'ACCEPTED' ? '已接受' : item.inviteStatus === 'DECLINED' ? '已拒绝' : '待回复'
          })
        })
        const last = messages[messages.length - 1]
        this.setData({ messages, scrollIntoView: last ? `m-${last.id}` : '' })
      })
      .catch(() => {})
  },
  input(e) {
    this.setData({ inputValue: e.detail.value })
  },
  send() {
    const content = (this.data.inputValue || '').trim()
    if (!content) return
    request({ url: '/api/chat/messages', method: 'POST', data: { conversationId: this.data.conversationId, content } })
      .then(() => {
        this.setData({ inputValue: '' })
        this.loadMessages()
      })
      .catch(() => {})
  },
  replyInvite(e) {
    const id = e.currentTarget.dataset.id
    const status = e.currentTarget.dataset.status
    request({ url: `/api/chat/activity-invite/${id}/reply`, method: 'POST', data: { status } })
      .then(() => {
        wx.showToast({ title: status === 'ACCEPTED' ? '已接受邀请' : '已暂时拒绝' })
        this.loadMessages()
      })
      .catch(() => {})
  },
  goActivity(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${e.currentTarget.dataset.id}` })
  }
})
