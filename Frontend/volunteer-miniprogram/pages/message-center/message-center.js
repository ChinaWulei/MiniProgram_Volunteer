const { request } = require('../../utils/request')

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(5, 16)
}

Page({
  data: {
    conversations: [],
    activityInvites: [],
    notices: [
      { id: 'system', type: '系统通知', title: '学院志愿服务社区已开启', summary: '可在志愿者库中查看公开档案并发起内部私聊。', time: '' }
    ],
    noticeUnread: 0,
    inviteUnread: 0
  },
  onShow() {
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/chat/conversations' })
      .then(list => {
        this.setData({
          conversations: (list || []).map(item => Object.assign({}, item, {
            timeText: formatTime(item.lastMessageAt),
            avatarText: (item.peerName || '志').substring(0, 1)
          }))
        })
      })
      .catch(() => {})

    request({ url: '/api/chat/activity-invites', silent: true })
      .then(list => {
        this.setData({
          activityInvites: (list || []).map(item => Object.assign({}, item, {
            time: formatTime(item.createdAt),
            unread: !item.readAt,
            statusText: item.inviteStatus === 'ACCEPTED' ? '已接受' : item.inviteStatus === 'DECLINED' ? '已拒绝' : '待回复'
          }))
        })
      })
      .catch(() => {})

    request({ url: '/api/chat/activity-invites/unread-count', silent: true })
      .then(data => this.setData({ inviteUnread: data.unreadCount || 0 }))
      .catch(() => {})

    request({ url: '/api/notifications', silent: true })
      .then(list => {
        const notices = (list || []).map(item => ({
          id: item.id,
          type: item.type === 'REGISTRATION_REVIEW' ? '报名审核通知' : item.type === 'ACTIVITY_NEWS' ? '新闻发布通知' : '系统通知',
          title: item.title,
          summary: item.content,
          targetType: item.targetType,
          targetId: item.targetId,
          unread: !item.readAt,
          time: formatTime(item.createdAt)
        }))
        this.setData({
          notices: notices.concat(this.data.notices.filter(item => item.id === 'system')),
          noticeUnread: notices.filter(item => item.unread).length
        })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  goChat(e) {
    const item = this.data.conversations.find(conv => conv.id === e.currentTarget.dataset.id)
    if (!item) return
    wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${item.id}&peerId=${item.peerUserId}&peerName=${encodeURIComponent(item.peerName)}` })
  },
  goInvite(e) {
    const item = this.data.activityInvites.find(invite => invite.id === e.currentTarget.dataset.id)
    if (!item) return
    request({ url: `/api/chat/messages/${item.id}/read`, method: 'POST', silent: true })
      .then(() => this.load())
      .catch(() => {})
    if (item.activityId) wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${item.activityId}` })
  },
  goNotice(e) {
    const item = this.data.notices.find(notice => notice.id === e.currentTarget.dataset.id)
    if (!item) return
    if (item.id !== 'system') {
      request({ url: `/api/notifications/${item.id}/read`, method: 'POST', silent: true })
        .then(() => this.load())
        .catch(() => {})
    }
    if (item.targetType === 'ACTIVITY_NEWS') {
      wx.navigateTo({ url: `/pages/activity-news-detail/activity-news-detail?id=${item.targetId}` })
    } else if (item.targetType === 'ACTIVITY') {
      wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${item.targetId}` })
    }
  }
})
