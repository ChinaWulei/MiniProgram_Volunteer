const { request } = require('../../../utils/request')

Page({
  data: { list: [] },
  onShow() {
    this.load()
  },
  load() {
    request({ url: '/api/admin/growth-reflections' }).then(list => this.setData({ list: list || [] }))
  },
  setDisplay(e) {
    request({
      url: `/api/admin/growth-reflections/${e.currentTarget.dataset.id}/display`,
      method: 'POST',
      data: { displayEnabled: e.currentTarget.dataset.enabled }
    }).then(() => this.load())
  },
  remove(e) {
    wx.showModal({
      title: '删除成长感悟',
      content: '确认删除这条不合适内容？',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/admin/growth-reflections/${e.currentTarget.dataset.id}`, method: 'DELETE' })
          .then(() => this.load())
      }
    })
  }
})
