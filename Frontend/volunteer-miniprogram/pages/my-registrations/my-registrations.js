const { request } = require('../../utils/request')
const app = getApp()

Page({
  data: { list: [] },
  onShow() {
    const token = app.globalData.token || wx.getStorageSync('token')
    if (!token) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/user/signup/list' })
      .then(list => this.setData({ list: list || [] }))
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?activityId=${e.currentTarget.dataset.id}` })
  }
})
