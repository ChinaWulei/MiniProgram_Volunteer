const { request } = require('../../utils/request')
Page({
  data: { list: [] },
  onShow() { this.load() },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/evaluations/my' })
      .then(list => this.setData({ list: (list || []).map(item => Object.assign({}, item, { createdText: String(item.createdAt || item.created_at || '').replace('T', ' ').slice(0, 16) })) }))
      .finally(() => wx.hideLoading())
  }
})
