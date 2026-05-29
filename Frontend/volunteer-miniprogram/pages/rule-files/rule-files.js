const app = getApp()
const { request } = require('../../utils/request')

function formatSize(value) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024) return `${(size / 1024 / 1024).toFixed(1)}MB`
  if (size >= 1024) return `${(size / 1024).toFixed(1)}KB`
  return `${size}B`
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function statusText(status) {
  if (status === 'READY') return 'Ready'
  if (status === 'PROCESSING') return 'Processing'
  if (status === 'FAILED') return 'Failed'
  return status || '-'
}

Page({
  data: {
    list: [],
    readyCount: 0,
    loading: false,
    isAdmin: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.load()
  },
  load() {
    this.setData({ loading: true })
    request({ url: '/api/rule-files' })
      .then(list => {
        const items = (list || []).map(item => {
          const status = item.status || ''
          return Object.assign({}, item, {
            fileTypeText: (item.fileType || 'FILE').toUpperCase(),
            statusText: statusText(status),
            statusClass: status.toLowerCase(),
            sizeText: formatSize(item.fileSize),
            createdText: formatTime(item.createdAt)
          })
        })
        this.setData({
          list: items,
          readyCount: items.filter(item => item.status === 'READY').length
        })
      })
      .finally(() => this.setData({ loading: false }))
  },
  openFile(e) {
    const id = e.currentTarget.dataset.id
    request({ url: `/api/rule-files/${id}/download` })
      .then(data => {
        const url = data && data.url
        if (!url) return
        wx.showLoading({ title: 'Opening' })
        wx.downloadFile({
          url,
          success: res => {
            if (res.statusCode === 200) {
              wx.openDocument({
                filePath: res.tempFilePath,
                showMenu: true,
                fail: () => this.copyUrl(url)
              })
            } else {
              this.copyUrl(url)
            }
          },
          fail: () => this.copyUrl(url),
          complete: () => wx.hideLoading()
        })
      })
  },
  copyUrl(url) {
    wx.setClipboardData({ data: url })
  },
  deleteFile(e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name
    wx.showModal({
      title: 'Delete rule file',
      content: `Delete "${name}"? Its vector chunks will also be removed from rule retrieval.`,
      confirmText: 'Delete',
      confirmColor: '#dc2626',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/rule-files/${id}`, method: 'DELETE' })
          .then(() => {
            wx.showToast({ title: 'Deleted' })
            this.load()
          })
      }
    })
  }
})
