const { request } = require('../../utils/request')

Page({
  data: {
    stats: {},
    creditRules: []
  },
  onShow() {
    this.load()
  },
  load() {
    request({ url: '/api/admin/statistics' }).then(stats => this.setData({ stats: this.prepareCharts(stats || {}) }))
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
    return stats
  },
  create() { wx.navigateTo({ url: '/pages/admin/activity-publish/activity-publish' }) },
  manageActivities() { wx.navigateTo({ url: '/pages/admin/activity-manage/activity-manage' }) },
  volunteers() { wx.navigateTo({ url: '/pages/volunteers/volunteers' }) },
  reviewList() { wx.navigateTo({ url: '/pages/admin/registration-review/registration-review' }) },
  goVolunteer(e) { wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` }) },
  review(e) {
    request({
      url: `/api/registrations/${e.currentTarget.dataset.id}/review`,
      method: 'PUT',
      data: { status: e.currentTarget.dataset.status, reviewRemark: '管理员审核' }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.load()
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
