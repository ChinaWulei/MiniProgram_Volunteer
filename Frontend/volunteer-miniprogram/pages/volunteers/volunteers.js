const { request } = require('../../utils/request')
Page({
  data: { list: [], keyword: '', major: '', skill: '' },
  onShow() { this.load() },
  input(e) { this.setData({ [e.currentTarget.dataset.k]: e.detail.value }) },
  load() {
    request({ url: '/api/volunteers', data: { keyword: this.data.keyword, major: this.data.major, skill: this.data.skill } })
      .then(list => this.setData({ list }))
  }
})
