const { request } = require('../../utils/request')

Page({
  data: {
    question: '',
    loading: false,
    answer: '',
    sources: []
  },
  input(e) {
    this.setData({ question: e.detail.value })
  },
  ask() {
    const question = (this.data.question || '').trim()
    if (!question || this.data.loading) return
    this.setData({ loading: true })
    wx.showLoading({ title: '检索规则中' })
    request({ url: '/api/rag/ask', method: 'POST', data: { question } })
      .then(data => this.setData({ answer: data.answer || '', sources: data.sources || [] }))
      .catch(() => {})
      .finally(() => {
        this.setData({ loading: false })
        wx.hideLoading()
      })
  }
})
