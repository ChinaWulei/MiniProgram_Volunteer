const { request } = require('../../utils/request')

function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function isImage(file) {
  return ['jpg', 'jpeg', 'png', 'webp'].indexOf(String(file.fileType || '').toLowerCase()) >= 0
}

Page({
  data: {
    id: null,
    detail: null
  },
  onLoad(options) {
    this.setData({ id: options.id })
    this.load()
  },
  load() {
    request({ url: `/api/notifications/${this.data.id}` })
      .then(data => {
        const attachments = (data.attachments || []).map(file => Object.assign({}, file, {
          isImage: isImage(file)
        }))
        const typeLabel = data.type === 'REGISTRATION_PROMOTED' ? '递补录取通知' : '活动通知'
        wx.setNavigationBarTitle({ title: `${typeLabel}详情` })
        this.setData({
          detail: Object.assign({}, data, {
            typeLabel,
            createdAtText: formatTime(data.createdAt),
            images: attachments.filter(file => file.isImage),
            files: attachments.filter(file => !file.isImage)
          })
        })
      })
  },
  previewImage(e) {
    const urls = (this.data.detail.images || []).map(item => item.url)
    wx.previewImage({ current: e.currentTarget.dataset.url, urls })
  },
  openAttachment(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    wx.showLoading({ title: '打开中' })
    wx.downloadFile({
      url,
      success: res => {
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            showMenu: true,
            fail: () => wx.setClipboardData({ data: url })
          })
        } else {
          wx.setClipboardData({ data: url })
        }
      },
      fail: () => wx.setClipboardData({ data: url }),
      complete: () => wx.hideLoading()
    })
  }
})
