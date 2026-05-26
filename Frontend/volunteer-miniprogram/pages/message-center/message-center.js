const { request } = require('../../utils/request')

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

Page({
  data: {
    conversations: [],
    notices: [
      { id: 'system', type: '系统通知', title: '学院志愿服务社区已开启', summary: '可在志愿者库中查看公开档案并发起内部私聊。', time: '' }
    ]
  },
  onShow() {
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/chat/conversations' })
      .then(list => {
        this.setData({ conversations: (list || []).map(item => Object.assign({}, item, { timeText: formatTime(item.lastMessageAt), avatarText: (item.peerName || '志').substring(0, 1) })) })
      })
    request({ url: '/api/notifications', silent: true })
      .then(list => {
        const notices = (list || []).map(item => ({
          id: item.id,
          type: item.type === 'ACTIVITY_NEWS' ? '新闻发布通知' : '系统通知',
          title: item.title,
          summary: item.content,
          targetType: item.targetType,
          targetId: item.targetId,
          time: formatTime(item.createdAt)
        }))
        this.setData({ notices: notices.concat(this.data.notices.filter(item => item.id === 'system')) })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  goChat(e) {
    const item = this.data.conversations.find(conv => conv.id === e.currentTarget.dataset.id)
    if (!item) return
    wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${item.id}&peerId=${item.peerUserId}&peerName=${encodeURIComponent(item.peerName)}` })
  },
  goNotice(e) {
    const item = this.data.notices.find(notice => notice.id === e.currentTarget.dataset.id)
    if (item && item.targetType === 'ACTIVITY_NEWS') {
      wx.navigateTo({ url: `/pages/activity-news-detail/activity-news-detail?id=${item.targetId}` })
    }
  }
})
