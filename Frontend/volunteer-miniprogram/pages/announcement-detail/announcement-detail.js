const { request } = require('../../utils/request')

Page({
  data: { id: null, detail: null },
  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },
  load() {
    request({ url: `/api/announcements/${this.data.id}` })
      .then(detail => this.setData({ detail: Object.assign({ imageUrls: [], attachments: [] }, detail || {}) }))
  },
  previewImage(e) {
    wx.previewImage({ current: e.currentTarget.dataset.url, urls: this.data.detail.imageUrls || [] })
  },
  openAttachment(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    wx.setClipboardData({ data: url })
  }
})
