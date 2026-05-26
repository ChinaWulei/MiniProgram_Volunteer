const { request, uploadFile } = require('../../../utils/request')

Page({
  data: {
    activityId: null,
    newsId: null,
    form: { title: '', content: '', resultSummary: '', status: 'DRAFT', imageUrls: [] },
    generating: false,
    saving: false
  },
  onLoad(options) {
    this.setData({ activityId: Number(options.activityId) })
    this.generate()
  },
  input(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  chooseImages() {
    const handleFiles = files => {
      files.forEach(file => this.uploadOne(file.tempFilePath || file.path))
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({ count: 9, mediaType: ['image'], sourceType: ['album', 'camera'], success: res => handleFiles(res.tempFiles) })
    } else {
      wx.chooseImage({ count: 9, success: res => handleFiles(res.tempFilePaths.map(path => ({ tempFilePath: path }))) })
    }
  },
  uploadOne(path) {
    wx.showLoading({ title: '上传中' })
    uploadFile({ url: '/api/admin/activity-news/images', filePath: path, name: 'files' })
      .then(data => {
        const imageUrls = this.data.form.imageUrls.concat(data.urls || [])
        this.setData({ 'form.imageUrls': imageUrls })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  removeImage(e) {
    const index = e.currentTarget.dataset.index
    const imageUrls = this.data.form.imageUrls.slice()
    imageUrls.splice(index, 1)
    this.setData({ 'form.imageUrls': imageUrls })
  },
  preview(e) {
    wx.previewImage({ urls: this.data.form.imageUrls, current: e.currentTarget.dataset.url })
  },
  generate() {
    if (this.data.generating) return
    this.setData({ generating: true })
    wx.showLoading({ title: '生成中' })
    request({ url: `/api/admin/activities/${this.data.activityId}/news/generate`, method: 'POST' })
      .then(data => this.setData({ 'form.title': data.title, 'form.content': data.content }))
      .catch(() => {})
      .finally(() => { this.setData({ generating: false }); wx.hideLoading() })
  },
  saveDraft() {
    return this.save('DRAFT')
  },
  publish() {
    this.save('PUBLISHED').then(newsId => {
      request({ url: `/api/admin/activity-news/${newsId}/publish`, method: 'POST' })
        .then(() => {
          wx.showToast({ title: '发布成功' })
          setTimeout(() => wx.redirectTo({ url: `/pages/activity-news-detail/activity-news-detail?id=${newsId}` }), 600)
        })
        .catch(() => {})
    })
  },
  save(status) {
    if (this.data.saving) return Promise.reject()
    const form = Object.assign({}, this.data.form, { activityId: this.data.activityId, status })
    if (!form.title || !form.content) {
      wx.showToast({ title: '请填写标题和正文', icon: 'none' })
      return Promise.reject()
    }
    this.setData({ saving: true })
    return request({ url: '/api/admin/activity-news', method: 'POST', data: form })
      .then(data => {
        this.setData({ newsId: data.id })
        if (status === 'DRAFT') wx.showToast({ title: '草稿已保存' })
        return data.id
      })
      .finally(() => this.setData({ saving: false }))
  }
})
