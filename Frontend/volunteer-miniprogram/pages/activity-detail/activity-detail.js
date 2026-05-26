const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    id: null,
    activity: null,
    skills: [],
    checkin: {},
    registrations: [],
    showCancel: false,
    cancelItem: null,
    cancelReason: '',
    isAdmin: false,
    buttonText: '立即报名',
    buttonDisabled: false
  },
  onLoad(options) {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ id: options.activityId || options.id, isAdmin: user && user.role === 'ADMIN' })
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: `/api/activity/${this.data.id}` })
      .then(activity => {
        activity.startTimeText = this.formatTime(activity.startTime)
        activity.endTimeText = this.formatTime(activity.endTime)
        activity.signupDeadlineText = this.formatTime(activity.signupDeadline)
        this.setData({ activity, skills: this.splitTags(activity.skillRequirements) })
        this.refreshButton(activity)
        if (!this.data.isAdmin) this.loadCheckinStatus()
        if (this.data.isAdmin) this.loadRegistrations()
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  loadRegistrations() {
    request({ url: '/api/registrations/admin', data: { activityId: this.data.id }, silent: true })
      .then(list => {
        const registrations = (list || []).map(item => Object.assign({}, item, {
          userText: item.nickname || item.userName || '志愿者',
          avatarText: (item.nickname || item.userName || '志').substring(0, 1),
          createdText: this.formatTime(item.created_at || item.createdAt)
        }))
        this.setData({ registrations })
      })
      .catch(() => {})
  },
  formatTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : ''
  },
  loadCheckinStatus() {
    request({ url: `/api/activity/${this.data.id}/checkin/status`, silent: true })
      .then(checkin => this.setData({ checkin }))
      .catch(() => {})
  },
  splitTags(tags) {
    return (tags || '').split(',').map(item => item.trim()).filter(Boolean)
  },
  refreshButton(activity) {
    let buttonText = '立即报名'
    let buttonDisabled = false
    if (activity.status === '已结束') {
      buttonText = '活动已结束'
      buttonDisabled = true
    } else if (activity.status === '已满员' || activity.remainingNumber <= 0) {
      buttonText = '名额已满'
      buttonDisabled = true
    } else if (activity.signupStatus === '待审核') {
      buttonText = '待审核'
      buttonDisabled = true
    } else if (activity.signupStatus === '已通过') {
      buttonText = '已通过'
      buttonDisabled = true
    }
    this.setData({ buttonText, buttonDisabled })
  },
  join() {
    if (this.data.buttonDisabled) return
    wx.showLoading({ title: '报名中' })
    request({ url: `/api/activity/${this.data.id}/signup`, method: 'POST' })
      .then(() => {
        wx.showToast({ title: '报名成功' })
        this.load()
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  contactAdmin() {
    const adminId = this.data.activity && this.data.activity.createdBy
    if (!adminId) {
      wx.showToast({ title: '暂无管理员联系人', icon: 'none' })
      return
    }
    request({ url: '/api/chat/conversations', method: 'POST', data: { targetUserId: adminId } })
      .then(data => wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${data.conversationId}&peerId=${adminId}&peerName=${encodeURIComponent('活动管理员')}` }))
      .catch(() => {})
  },
  goMatch() {
    wx.navigateTo({ url: `/pages/match/match?activityId=${this.data.id}` })
  },
  edit() {
    wx.navigateTo({ url: `/pages/activity-form/activity-form?id=${this.data.id}` })
  },
  checkin() {
    wx.getLocation({
      type: 'gcj02',
      success: res => {
        wx.showLoading({ title: '签到中' })
        request({
          url: '/api/activity/checkin',
          method: 'POST',
          data: { activityId: this.data.id, latitude: res.latitude, longitude: res.longitude }
        }).then(checkin => {
          this.setData({ checkin })
          wx.showToast({ title: '签到成功' })
        }).catch(() => {}).finally(() => wx.hideLoading())
      },
      fail: () => wx.showToast({ title: '定位失败，请检查授权', icon: 'none' })
    })
  },
  goCheckinManage() {
    wx.navigateTo({ url: `/pages/admin/activity-checkin/activity-checkin?activityId=${this.data.id}` })
  },
  openCancel(e) {
    const item = this.data.registrations.find(row => row.id === e.currentTarget.dataset.id)
    this.setData({ showCancel: true, cancelItem: item, cancelReason: '' })
  },
  closeCancel() {
    this.setData({ showCancel: false, cancelItem: null, cancelReason: '' })
  },
  inputCancelReason(e) {
    this.setData({ cancelReason: e.detail.value })
  },
  cancelRegistration() {
    request({
      url: `/api/registrations/${this.data.cancelItem.id}/cancel`,
      method: 'POST',
      data: { reason: this.data.cancelReason }
    }).then(() => {
      wx.showToast({ title: '已取消报名' })
      this.closeCancel()
      this.load()
    }).catch(() => {})
  }
})
