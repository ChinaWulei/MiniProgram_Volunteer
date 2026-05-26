const { request } = require('../../utils/request')
Page({
  data: {
    id: null,
    form: { status: '报名中', recruitNumber: 10 }
  },
  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      request({ url: `/api/activities/${options.id}` }).then(form => {
        form.startTime = (form.startTime || '').replace(' ', 'T')
        form.endTime = (form.endTime || '').replace(' ', 'T')
        this.setData({ form })
      })
    }
  },
  input(e) {
    const k = e.currentTarget.dataset.k
    this.setData({ [`form.${k}`]: k === 'recruitNumber' ? Number(e.detail.value) : e.detail.value })
  },
  submit() {
    const method = this.data.id ? 'PUT' : 'POST'
    const url = this.data.id ? `/api/activities/${this.data.id}` : '/api/activities'
    request({ url, method, data: this.data.form }).then(() => {
      wx.showToast({ title: '保存成功' })
      wx.navigateBack()
    })
  }
})
