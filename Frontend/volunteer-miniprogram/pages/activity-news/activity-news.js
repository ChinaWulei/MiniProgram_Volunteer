const { request } = require('../../utils/request')

function normalize(item) {
  return Object.assign({}, item, {
    cover: item.imageUrls && item.imageUrls.length ? item.imageUrls[0] : '',
    timeText: String(item.publishedAt || '').replace('T', ' ').slice(0, 16)
  })
}

Page({
  data: { list: [] },
  onShow() {
    request({ url: '/api/activity-news' })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
      .catch(() => {})
  },
  detail(e) {
    wx.navigateTo({ url: `/pages/activity-news-detail/activity-news-detail?id=${e.currentTarget.dataset.id}` })
  }
})
