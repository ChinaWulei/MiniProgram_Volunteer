const app = getApp()
const { request } = require('../../utils/request')

function splitTags(tags) {
  return (tags || '').split(',').map(item => item.trim()).filter(Boolean)
}

function normalizeHistory(item) {
  return {
    id: item.id,
    name: item.name,
    coverImageUrl: item.cover_image_url || item.coverImageUrl,
    category: item.category,
    location: item.location,
    startTime: String(item.start_time || item.startTime || '').replace('T', ' ').slice(0, 16),
    endTime: String(item.end_time || item.endTime || '').replace('T', ' ').slice(0, 16),
    serviceHours: item.service_hours || item.serviceHours,
    status: item.status
  }
}

Page({
  data: {
    id: null,
    profile: {},
    skills: [],
    badges: [],
    historyActivities: [],
    isAdmin: false,
    checkinStats: null,
    activities: [],
    activityNames: [],
    activityIndex: 0,
    inviteReason: '',
    showInvite: false
  },
  onLoad(options) {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ id: options.id, isAdmin: user && user.role === 'ADMIN' })
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: `/api/volunteers/${this.data.id}` })
      .then(profile => {
        this.setData({
          profile: Object.assign({}, profile, { displayName: profile.nickname || profile.name || '志愿者', avatarText: (profile.nickname || profile.name || '志').substring(0, 1) }),
          skills: splitTags(profile.skillTags),
          badges: splitTags(profile.badges),
          historyActivities: (profile.historyActivities || []).map(normalizeHistory)
        })
        if (this.data.isAdmin) {
          this.loadActivities()
          this.loadCheckinStats()
        }
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  loadActivities() {
    request({ url: '/api/activities', data: { status: '报名中' } })
      .then(list => this.setData({ activities: list || [], activityNames: (list || []).map(item => item.name) }))
      .catch(() => {})
  },
  loadCheckinStats() {
    request({ url: `/api/admin/volunteers/${this.data.id}/checkin/statistics`, silent: true })
      .then(checkinStats => this.setData({ checkinStats }))
      .catch(() => {})
  },
  startChat() {
    request({ url: '/api/chat/conversations', method: 'POST', data: { targetUserId: this.data.id } })
      .then(data => wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${data.conversationId}&peerId=${this.data.id}&peerName=${encodeURIComponent(this.data.profile.displayName)}` }))
      .catch(() => {})
  },
  openInvite() {
    if (!this.data.activities.length) {
      wx.showToast({ title: '暂无可邀请活动', icon: 'none' })
      return
    }
    this.setData({ showInvite: true })
  },
  closeInvite() { this.setData({ showInvite: false }) },
  pickActivity(e) { this.setData({ activityIndex: Number(e.detail.value) }) },
  inputReason(e) { this.setData({ inviteReason: e.detail.value }) },
  sendInvite() {
    const activity = this.data.activities[this.data.activityIndex]
    if (!activity) return
    wx.showLoading({ title: '发送中' })
    request({
      url: '/api/chat/activity-invite',
      method: 'POST',
      data: { receiverId: this.data.id, activityId: activity.id, reason: this.data.inviteReason }
    }).then(() => {
      wx.showToast({ title: '邀请已发送' })
      this.setData({ showInvite: false, inviteReason: '' })
    }).catch(() => {}).finally(() => wx.hideLoading())
  },
  goActivity(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${e.currentTarget.dataset.id}` })
  }
})
