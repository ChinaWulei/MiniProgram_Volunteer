const { request } = require('../../../utils/request')
Page({
  data: { activities: [], keyword: '', currentId: null, feedback: [], averageScore: '0.0', summaryText: '', summaryLoading: false },
  onShow() { this.loadActivities() },
  inputKeyword(e) { this.setData({ keyword: e.detail.value }) },
  loadActivities() { request({ url: '/api/activities', data: { keyword: this.data.keyword } }).then(list => this.setData({ activities: list || [] })) },
  selectActivity(e) { this.setData({ currentId: e.currentTarget.dataset.id, summaryText: '' }); this.loadFeedback() },
  loadFeedback() {
    request({ url: `/api/activities/${this.data.currentId}/evaluations/feedback` }).then(list => {
      const feedback = list || []
      const total = feedback.reduce((s, i) => s + Number(i.score || 0), 0)
      this.setData({ feedback, averageScore: feedback.length ? (total / feedback.length).toFixed(1) : '0.0' })
    })
  },
  summary() {
    this.setData({ summaryLoading: true })
    request({ url: `/api/activities/${this.data.currentId}/evaluations/feedback/ai-summary`, method: 'POST' })
      .then(data => this.setData({ summaryText: data.summary || '' }))
      .finally(() => this.setData({ summaryLoading: false }))
  },
  adopt(e) { request({ url: `/api/activities/${this.data.currentId}/evaluations/${e.currentTarget.dataset.id}/adopt/${e.currentTarget.dataset.type}`, method: 'POST' }).then(() => wx.showToast({ title: '已采纳' })) },
  unadopt(e) { request({ url: `/api/activities/${this.data.currentId}/evaluations/${e.currentTarget.dataset.id}/adopt/${e.currentTarget.dataset.type}`, method: 'DELETE' }).then(() => wx.showToast({ title: '已取消' })) }
})
