const { request } = require('../../../utils/request')
Page({
  data: { list: [] },
  onShow() { this.load() },
  load() { request({ url: '/api/admin/experiences' }).then(list => this.setData({ list: list || [] })) },
  toggle(e) { request({ url: `/api/admin/experiences/${e.currentTarget.dataset.id}/enabled`, method: 'POST', data: { enabled: e.currentTarget.dataset.enabled } }).then(() => this.load()) }
})
