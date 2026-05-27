const { request, uploadFile } = require('../../../utils/request')

Page({
  data: {
    title: '',
    content: '',
    imageUrls: [],
    attachments: []
  },
  inputTitle(e) { this.setData({ title: e.detail.value }) },
  inputContent(e) { this.setData({ content: e.detail.value }) },
  chooseImages() {
    wx.chooseImage({
      count: 9,
      success: res => {
        const tasks = (res.tempFilePaths || []).map(filePath =>
          uploadFile({ url: '/api/admin/announcements/images', filePath, name: 'files' })
        )
        Promise.all(tasks).then(results => {
          const urls = results.reduce((all, item) => all.concat(item.urls || []), [])
          this.setData({ imageUrls: this.data.imageUrls.concat(urls) })
        })
      }
    })
  },
  chooseAttachment() {
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf', 'docx', 'txt'],
      success: res => {
        const file = (res.tempFiles || [])[0]
        const filePath = file && (file.path || file.tempFilePath)
        if (!filePath) {
          wx.showToast({ title: '文件路径无效，请重新选择', icon: 'none' })
          return
        }
        wx.showLoading({ title: '上传解析中' })
        uploadFile({ url: '/api/admin/announcements/attachments', filePath, name: 'file' })
          .then(data => this.setData({ attachments: this.data.attachments.concat([data]) }))
          .finally(() => wx.hideLoading())
      }
    })
  },
  previewImage(e) {
    wx.previewImage({ current: e.currentTarget.dataset.url, urls: this.data.imageUrls })
  },
  payload(status) {
    return {
      title: this.data.title,
      content: this.data.content,
      imageUrls: this.data.imageUrls,
      ruleFileIds: this.data.attachments.map(item => item.id),
      status
    }
  },
  saveDraft() {
    request({ url: '/api/admin/announcements', method: 'POST', data: this.payload('DRAFT') })
      .then(() => wx.showToast({ title: '已保存' }))
  },
  publish() {
    request({ url: '/api/admin/announcements', method: 'POST', data: this.payload('PUBLISHED') })
      .then(data => request({ url: `/api/admin/announcements/${data.id}/publish`, method: 'POST' }))
      .then(() => {
        wx.showToast({ title: '已发布' })
        setTimeout(() => wx.navigateBack(), 600)
      })
  }
})
