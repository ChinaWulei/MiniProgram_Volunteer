const { request } = require('../../utils/request')

Page({
  data: { news: null, timeText: '' },
  onLoad(options) {
    request({ url: `/api/activity-news/${options.id}` })
      .then(news => this.setData({ news, timeText: String(news.publishedAt || '').replace('T', ' ').slice(0, 16) }))
      .catch(() => {})
  },
  preview(e) {
    wx.previewImage({ urls: this.data.news.imageUrls || [], current: e.currentTarget.dataset.url })
  }
})
