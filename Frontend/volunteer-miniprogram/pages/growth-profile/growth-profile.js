const { request } = require('../../utils/request')
Page({
  data: { profile: {}, report: '', loadingReport: false },
  onShow() { this.load() },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/growth-profile' }).then(profile => this.setData({ profile: profile || {} })).finally(() => wx.hideLoading())
  },
  generateReport() {
    this.setData({ loadingReport: true })
    request({ url: '/api/growth-profile/report', method: 'POST' })
      .then(data => this.setData({ report: data.report || '' }))
      .finally(() => this.setData({ loadingReport: false }))
  }
})
