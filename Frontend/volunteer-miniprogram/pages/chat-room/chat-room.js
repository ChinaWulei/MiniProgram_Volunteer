const app = getApp()
const { request, uploadFile } = require('../../utils/request')

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
    activityList: [],
    showActivityPicker: false,
    scrollIntoView: '',
    socketOpen: false
  },
  onLoad(options) {
    const user = app.globalData.user || wx.getStorageSync('user') || {}
    this.setData({ conversationId: Number(options.conversationId), peerId: Number(options.peerId), peerName: decodeURIComponent(options.peerName || '学院同学'), currentUserId: user.id })
    wx.setNavigationBarTitle({ title: this.data.peerName })
    this.loadMessages()
    this.loadActivities()
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
  chooseImages() {
    const sendFiles = files => {
      files.forEach(file => {
        const path = file.tempFilePath || file.path
        uploadFile({ url: '/api/chat/images', filePath: path, name: 'file' })
          .then(data => request({
            url: '/api/chat/messages',
            method: 'POST',
            data: { conversationId: this.data.conversationId, type: 'IMAGE', imageUrl: data.imageUrl || data.url }
          }))
          .then(() => this.loadMessages())
          .catch(() => {})
      })
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 9,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: res => sendFiles(res.tempFiles || [])
      })
    } else {
      wx.chooseImage({
        count: 9,
        sourceType: ['album', 'camera'],
        success: res => sendFiles((res.tempFilePaths || []).map(path => ({ tempFilePath: path })))
      })
    }
  },
  loadActivities() {
    request({ url: '/api/activities', data: { status: '报名中' }, silent: true })
      .then(list => this.setData({ activityList: (list || []).slice(0, 12) }))
      .catch(() => {})
  },
  toggleActivityPicker() {
    this.setData({ showActivityPicker: !this.data.showActivityPicker })
  },
  sendActivityCard(e) {
    const activityId = e.currentTarget.dataset.id
    request({
      url: '/api/chat/messages',
      method: 'POST',
      data: { conversationId: this.data.conversationId, activityId, content: '推荐你看看这个志愿活动' }
    }).then(() => {
      this.setData({ showActivityPicker: false })
      this.loadMessages()
    }).catch(() => {})
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
