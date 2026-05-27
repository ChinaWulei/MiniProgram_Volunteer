const app = getApp()
const { request } = require('../../utils/request')

function toNumber(value) {
  const number = Number(value)
  return Number.isNaN(number) ? 0 : number
}

function getRemainingSlots(activity) {
  return Math.max(toNumber(activity.recruitNumber) - toNumber(activity.registeredNumber), 0)
}

function getStatusClass(status) {
  if (status === '已满员') return 'full'
  if (status === '已结束') return 'ended'
  return 'open'
}

function getRecommendReason(activity) {
  const remaining = getRemainingSlots(activity)
  const registered = toNumber(activity.registeredNumber)
  if (activity.status === '报名中' && remaining > 0 && remaining <= 3) return '名额紧张'
  if (registered >= 8) return '报名热度高'
  if (activity.status === '报名中') return '即将截止'
  return activity.category || '精选活动'
}

function normalizeActivity(activity) {
  const remainingSlots = getRemainingSlots(activity)
  return Object.assign({}, activity, {
    remainingSlots,
    startTimeText: formatTime(activity.startTime),
    endTimeText: formatTime(activity.endTime),
    statusClass: getStatusClass(activity.status),
    reason: getRecommendReason(activity)
  })
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

Page({
  data: {
    user: null,
    isAdmin: false,
    activeTab: 'home',
    showActivityHall: false,
    activityList: [],
    recommendedActivities: [],
    keyword: '',
    category: '',
    status: '',
    unreadCount: 0,
    categories: ['全部', '迎新服务', '赛事保障', '校园讲解', '社区服务'],
    statuses: ['全部', '报名中', '已满员', '已结束'],
    stats: [
      { label: '累计服务时长', value: '0h', icon: '时' },
      { label: '本月活动数', value: 0, icon: '月' },
      { label: '我的积分', value: 0, icon: '分' },
      { label: '待签到活动', value: 0, icon: '签' }
    ]
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ user, isAdmin: user && user.role === 'ADMIN' })
    this.loadActivities()
    this.loadUnreadCount()
    this.startUnreadPolling()
  },
  onHide() {
    this.stopUnreadPolling()
  },
  onUnload() {
    this.stopUnreadPolling()
  },
  goLogin() { wx.navigateTo({ url: '/pages/login/login' }) },
  goActivities() {
    this.setData({ showActivityHall: true, activeTab: 'hall' })
    this.loadActivities()
  },
  setKeyword(e) { this.setData({ keyword: e.detail.value }) },
  pickCategory(e) {
    const value = this.data.categories[e.detail.value]
    this.setData({ category: value === '全部' ? '' : value })
    this.loadActivities()
  },
  pickStatus(e) {
    const value = this.data.statuses[e.detail.value]
    this.setData({ status: value === '全部' ? '' : value })
    this.loadActivities()
  },
  loadActivities() {
    request({
      url: '/api/activities',
      data: {
        keyword: this.data.showActivityHall ? this.data.keyword : '',
        category: this.data.showActivityHall ? this.data.category : '',
        status: this.data.showActivityHall ? this.data.status : ''
      }
    }).then(list => {
      const activities = (list || []).map(normalizeActivity)
      this.setData({
        activityList: activities,
        recommendedActivities: this.getRecommendedActivities(activities),
        stats: this.getStats(activities)
      })
      this.loadRecommendedActivities()
    })
  },
  loadRecommendedActivities() {
    const token = app.globalData.token || wx.getStorageSync('token')
    if (!token || this.data.isAdmin) return
    request({ url: '/api/activities/recommend', silent: true })
      .then(list => {
        const recommendedActivities = (list || []).map(item => Object.assign(normalizeActivity(item), {
          reason: '技能与时间匹配'
        }))
        if (recommendedActivities.length) this.setData({ recommendedActivities })
      })
      .catch(() => {})
  },
  loadUnreadCount() {
    const token = app.globalData.token || wx.getStorageSync('token')
    if (!token) {
      this.setData({ unreadCount: 0 })
      return
    }
    Promise.all([
      request({ url: '/api/chat/conversations', silent: true }).catch(() => []),
      request({ url: '/api/notifications/unread-count', silent: true }).catch(() => ({ unreadCount: 0 })),
      request({ url: '/api/chat/activity-invites/unread-count', silent: true }).catch(() => ({ unreadCount: 0 }))
    ]).then(([list, notice, invites]) => {
      const chatUnread = (list || []).reduce((sum, item) => sum + toNumber(item.unreadCount), 0)
      this.setData({ unreadCount: chatUnread + toNumber(notice.unreadCount) + toNumber(invites.unreadCount) })
    }).catch(() => {})
  },
  startUnreadPolling() {
    this.stopUnreadPolling()
    this.unreadTimer = setInterval(() => this.loadUnreadCount(), 12000)
  },
  stopUnreadPolling() {
    if (this.unreadTimer) {
      clearInterval(this.unreadTimer)
      this.unreadTimer = null
    }
  },
  getRecommendedActivities(activities) {
    return activities
      .slice()
      .sort((a, b) => {
        const hotA = toNumber(a.registeredNumber) + (a.status === '报名中' ? 10 : 0) - a.remainingSlots
        const hotB = toNumber(b.registeredNumber) + (b.status === '报名中' ? 10 : 0) - b.remainingSlots
        return hotB - hotA
      })
      .slice(0, 4)
  },
  getStats(activities) {
    if (this.data.isAdmin) {
      const openCount = activities.filter(item => item.status === '报名中' || item.status === '已发布').length
      const totalRegistrations = activities.reduce((sum, item) => sum + toNumber(item.registeredNumber), 0)
      const totalRecruit = activities.reduce((sum, item) => sum + toNumber(item.recruitNumber), 0)
      const endedCount = activities.filter(item => item.status === '已结束').length
      return [
        { label: '活动总数', value: activities.length, icon: '活' },
        { label: '发布中', value: openCount, icon: '发' },
        { label: '报名人数', value: totalRegistrations, icon: '报' },
        { label: '已结束', value: endedCount, icon: '结' }
      ]
    }
    const now = new Date()
    const month = now.getMonth()
    const year = now.getFullYear()
    const monthCount = activities.filter(item => {
      const date = new Date((item.startTime || '').replace(/-/g, '/'))
      return !Number.isNaN(date.getTime()) && date.getFullYear() === year && date.getMonth() === month
    }).length
    const pendingCheckIn = activities.filter(item => item.status === '报名中').length
    const points = this.data.user && this.data.user.points ? this.data.user.points : pendingCheckIn * 5
    const serviceHours = this.data.user && this.data.user.serviceHours ? this.data.user.serviceHours : monthCount * 2

    return [
      { label: '累计服务时长', value: `${serviceHours}h`, icon: '时' },
      { label: '本月活动数', value: monthCount, icon: '月' },
      { label: '我的积分', value: points, icon: '分' },
      { label: '待签到活动', value: pendingCheckIn, icon: '签' }
    ]
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${e.currentTarget.dataset.id}` })
  },
  goMine() {
    const token = app.globalData.token || wx.getStorageSync('token')
    wx.navigateTo({ url: token ? '/pages/my-registrations/my-registrations' : '/pages/login/login' })
  },
  goMineOrManage() {
    if (this.data.isAdmin) {
      this.goActivityManage()
      return
    }
    this.goMine()
  },
  goProfile() { wx.navigateTo({ url: '/pages/profile/profile' }) },
  goVolunteers() { wx.navigateTo({ url: '/pages/volunteer-library/volunteer-library' }) },
  goMessages() { wx.navigateTo({ url: '/pages/message-center/message-center' }) },
  goNews() { wx.navigateTo({ url: '/pages/activity-news/activity-news' }) },
  goAi() { wx.navigateTo({ url: '/pages/ai-chat/ai-chat' }) },
  goAdmin() { wx.navigateTo({ url: '/pages/admin/admin' }) },
  goActivityPublish() { wx.navigateTo({ url: '/pages/admin/activity-publish/activity-publish' }) },
  goActivityManage() { wx.navigateTo({ url: '/pages/admin/activity-manage/activity-manage' }) }
})
