const { request } = require('../../utils/request')

const PIE_COLORS = ['#0f766e', '#2563eb', '#f59e0b', '#ef4444', '#8b5cf6', '#14b8a6', '#64748b', '#22c55e']

function pad(number) {
  return number < 10 ? `0${number}` : `${number}`
}

function formatDate(date) {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

function addDays(date, days) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next
}

function monthStart(date) {
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

Page({
  data: {
    stats: {},
    creditRules: [],
    rangeMode: '30d',
    rangeText: '',
    startDate: '',
    endDate: '',
    rangeOptions: [
      { key: '30d', label: '近30天' },
      { key: 'month', label: '本月' },
      { key: 'all', label: '全部' }
    ]
  },
  onShow() {
    if (!this.data.startDate && !this.data.endDate) this.applyRange('30d', false)
    this.load()
  },
  load() {
    request({
      url: '/api/admin/statistics',
      data: {
        startDate: this.data.startDate,
        endDate: this.data.endDate
      }
    }).then(stats => this.setData({ stats: this.prepareCharts(stats || {}) }))
    request({ url: '/api/admin/credit-rules', silent: true })
      .then(creditRules => this.setData({ creditRules: creditRules || [] }))
      .catch(() => {})
  },
  prepareCharts(stats) {
    const trend = stats.activityTrend || []
    const trendMax = Math.max.apply(null, trend.map(item => Number(item.count) || 0).concat([1]))
    stats.activityTrend = trend.map((item, index) => Object.assign({}, item, {
      height: Math.max(8, Math.round((Number(item.count) || 0) * 120 / trendMax)),
      pointLeft: trend.length <= 1 ? 0 : Math.round(index * 100 / (trend.length - 1)),
      pointBottom: Math.max(8, Math.round((Number(item.count) || 0) * 120 / trendMax))
    }))

    const skills = stats.skillStats || []
    const skillMax = Math.max.apply(null, skills.map(item => Number(item.count) || 0).concat([1]))
    stats.skillStats = skills.map(item => Object.assign({}, item, {
      percent: Math.round((Number(item.count) || 0) * 100 / skillMax)
    }))

    const hours = stats.hoursRank || []
    const hourMax = Math.max.apply(null, hours.map(item => Number(item.totalHours) || 0).concat([1]))
    stats.hoursRank = hours.map(item => Object.assign({}, item, {
      percent: Math.round((Number(item.totalHours) || 0) * 100 / hourMax)
    }))

    const checkins = stats.checkinStats || []
    stats.checkinStats = checkins.map(item => Object.assign({}, item, {
      rate: Number(item.checkinRate) || 0
    }))

    const categories = stats.categoryStats || []
    const total = categories.reduce((sum, item) => sum + (Number(item.count) || 0), 0)
    let cursor = 0
    const segments = []
    stats.categoryStats = categories.map((item, index) => {
      const count = Number(item.count) || 0
      const percent = total ? Math.round(count * 1000 / total) / 10 : 0
      const start = cursor
      const end = total ? cursor + count * 100 / total : cursor
      cursor = end
      const color = PIE_COLORS[index % PIE_COLORS.length]
      segments.push(`${color} ${start}% ${end}%`)
      return Object.assign({}, item, { percent, color })
    })
    stats.categoryPieGradient = segments.length ? `conic-gradient(${segments.join(',')})` : 'conic-gradient(#e5e7eb 0% 100%)'
    stats.categoryTotal = total
    return stats
  },
  applyRange(mode, shouldLoad = true) {
    const today = new Date()
    let startDate = ''
    let endDate = ''
    let rangeText = '全部数据'
    if (mode === '30d') {
      startDate = formatDate(addDays(today, -29))
      endDate = formatDate(today)
      rangeText = `${startDate} 至 ${endDate}`
    } else if (mode === 'month') {
      startDate = formatDate(monthStart(today))
      endDate = formatDate(today)
      rangeText = `${startDate} 至 ${endDate}`
    }
    this.setData({ rangeMode: mode, startDate, endDate, rangeText })
    if (shouldLoad) this.load()
  },
  selectRange(e) {
    this.applyRange(e.currentTarget.dataset.key)
  },
  pickStart(e) {
    this.setData({ rangeMode: 'custom', startDate: e.detail.value })
    this.updateCustomRange()
  },
  pickEnd(e) {
    this.setData({ rangeMode: 'custom', endDate: e.detail.value })
    this.updateCustomRange()
  },
  updateCustomRange() {
    const start = this.data.startDate
    const end = this.data.endDate
    this.setData({ rangeText: start || end ? `${start || '不限'} 至 ${end || '不限'}` : '全部数据' })
    this.load()
  },
  create() { wx.navigateTo({ url: '/pages/admin/activity-publish/activity-publish' }) },
  manageActivities() { wx.navigateTo({ url: '/pages/admin/activity-manage/activity-manage' }) },
  volunteers() { wx.navigateTo({ url: '/pages/volunteers/volunteers' }) },
  reviewList() { wx.navigateTo({ url: '/pages/admin/registration-review/registration-review' }) },
  checkinAdjustments() { wx.navigateTo({ url: '/pages/admin/checkin-adjustments/checkin-adjustments' }) },
  aiReports() { wx.navigateTo({ url: '/pages/report-center/report-center' }) },
  ruleFiles() { wx.navigateTo({ url: '/pages/rule-files/rule-files' }) },
  announcements() { wx.navigateTo({ url: '/pages/announcements/announcements' }) },
  goAnnouncement() { wx.navigateTo({ url: '/pages/admin/announcement-edit/announcement-edit' }) },
  goStat(e) {
    const type = e.currentTarget.dataset.type
    const urls = {
      activities: '/pages/admin/activity-manage/activity-manage',
      volunteers: '/pages/volunteers/volunteers',
      hours: '/pages/volunteers/volunteers?sort=hours',
      pending: '/pages/admin/registration-review/registration-review?pending=1'
    }
    if (urls[type]) wx.navigateTo({ url: urls[type] })
  },
  goVolunteer(e) { wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` }) },
  review(e) {
    request({
      url: `/api/registrations/${e.currentTarget.dataset.id}/review`,
      method: 'PUT',
      data: { status: e.currentTarget.dataset.status, reviewRemark: '管理员审核' }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.load()
    }).catch(err => {
      wx.showToast({ title: (err && err.message) || '处理失败', icon: 'none' })
    })
  },
  inputRule(e) {
    const index = e.currentTarget.dataset.index
    const key = e.currentTarget.dataset.key
    this.setData({ [`creditRules[${index}].${key}`]: key === 'changeValue' ? Number(e.detail.value) : e.detail.value })
  },
  saveRule(e) {
    const rule = this.data.creditRules[e.currentTarget.dataset.index]
    request({ url: '/api/admin/credit-rules', method: 'POST', data: rule })
      .then(() => wx.showToast({ title: '规则已保存' }))
      .catch(() => {})
  }
})
