const app = getApp()
const { request, uploadFile } = require('../../utils/request')

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

Page({
  data: {
    isAdmin: false,
    files: [],
    uploading: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.load()
  },
  load() {
    request({ url: '/api/rule-files' })
      .then(list => this.setData({ files: (list || []).map(item => Object.assign({}, item, { createdText: formatTime(item.createdAt) })) }))
      .catch(() => {})
  },
  chooseFile() {
    if (!this.data.isAdmin || this.data.uploading) return
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'docx', 'txt'],
      success: res => {
        const file = res.tempFiles && res.tempFiles[0]
        if (!file) return
        this.upload(file.path, file.name)
      }
    })
  },
  upload(path, name) {
    this.setData({ uploading: true })
    wx.showLoading({ title: '解析入库中' })
    uploadFile({ url: '/api/rule-files', filePath: path, name: 'file', formData: { name } })
      .then(() => {
        wx.showToast({ title: '上传成功' })
        this.load()
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ uploading: false })
        wx.hideLoading()
      })
  },
  download(e) {
    const id = e.currentTarget.dataset.id
    request({ url: `/api/rule-files/${id}/download` })
      .then(data => {
        wx.setClipboardData({ data: data.url })
        wx.showToast({ title: '下载链接已复制', icon: 'none' })
      })
      .catch(() => {})
  },
  goQa() {
    wx.navigateTo({ url: '/pages/rule-qa/rule-qa' })
  }
})
