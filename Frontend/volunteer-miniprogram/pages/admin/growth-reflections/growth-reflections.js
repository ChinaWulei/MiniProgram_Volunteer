const { request } = require('../../../utils/request')
Page({
  data: { list: [] },
  onShow() { this.load() },
  load() { request({ url: '/api/admin/growth-reflections' }).then(list => this.setData({ list: list || [] })) },
  recommend(e) { request({ url: `/api/admin/growth-reflections/${e.currentTarget.dataset.id}/recommend`, method: 'POST', data: { recommended: e.currentTarget.dataset.recommended } }).then(() => this.load()) }
})
