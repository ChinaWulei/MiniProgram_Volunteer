const app = getApp()
const { request } = require('../../utils/request')

function time(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function summary(value) {
  const text = String(value || '').replace(/\s+/g, ' ').trim()
  return text.length > 80 ? `${text.slice(0, 80)}...` : text
}

Page({
  data: {
    isAdmin: false,
    list: [],
    loading: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.load()
  },
  load() {
    this.setData({ loading: true })
    request({ url: this.data.isAdmin ? '/api/admin/announcements' : '/api/announcements' })
      .then(list => {
        const items = (list || []).map(item => {
          const images = item.imageUrls || []
          const attachments = item.attachments || []
          return Object.assign({}, item, {
            statusText: item.status === 'PUBLISHED' ? '已发布' : '草稿',
            timeText: time(item.publishedAt || item.createdAt),
            summary: summary(item.content),
            imageCount: images.length,
            attachmentCount: attachments.length
          })
        })
        this.setData({ list: items })
      })
      .finally(() => this.setData({ loading: false }))
  },
  create() {
    wx.navigateTo({ url: '/pages/admin/announcement-edit/announcement-edit' })
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/announcement-detail/announcement-detail?id=${e.currentTarget.dataset.id}` })
  },
  deleteItem(e) {
    const id = e.currentTarget.dataset.id
    const title = e.currentTarget.dataset.title
    wx.showModal({
      title: '删除公告',
      content: `确认删除「${title}」？`,
      confirmText: '删除',
      confirmColor: '#dc2626',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/admin/announcements/${id}`, method: 'DELETE' })
          .then(() => {
            wx.showToast({ title: '已删除' })
            this.load()
          })
      }
    })
  }
})
