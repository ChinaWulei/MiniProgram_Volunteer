const { request } = require('../../utils/request')
Page({
  data: { list: [], keyword: '', major: '', skill: '', sortBy: '' },
  onLoad(options) {
    if (options && options.sort) {
      this.setData({ sortBy: options.sort })
    }
  },
  onShow() { this.load() },
  input(e) { this.setData({ [e.currentTarget.dataset.k]: e.detail.value }) },
  load() {
    request({ url: '/api/volunteers', data: { keyword: this.data.keyword, majorClass: this.data.major, skillTag: this.data.skill, sortBy: this.data.sortBy } })
      .then(list => this.setData({ list }))
  },
  goVolunteer(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` })
  }
})
