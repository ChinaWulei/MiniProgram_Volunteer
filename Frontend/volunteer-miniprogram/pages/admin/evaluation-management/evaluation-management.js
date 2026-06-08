const { request } = require('../../../utils/request')

Page({
  data: {
    activities: [],
    keyword: '',
    currentId: null,
    currentName: '',
    feedback: [],
    averageScore: '0.0',
    summaryText: '',
    summaryLoading: false
  },
  onShow() {
    this.loadActivities()
  },
  inputKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },
  loadActivities() {
    request({ url: '/api/activities', data: { keyword: this.data.keyword } }).then(list => {
      const activities = list || []
      Promise.all(activities.map(item =>
        request({ url: `/api/activities/${item.id}/evaluations`, silent: true })
          .then(feedback => Object.assign({}, item, { evaluationCount: (feedback || []).length }))
          .catch(() => Object.assign({}, item, { evaluationCount: 0 }))
      )).then(rows => {
        rows.sort((a, b) => Number(b.evaluationCount || 0) - Number(a.evaluationCount || 0))
        this.setData({ activities: rows })
      })
    })
  },
  selectActivity(e) {
    this.setData({
      currentId: e.currentTarget.dataset.id,
      currentName: e.currentTarget.dataset.name || '',
      summaryText: ''
    })
    this.loadFeedback()
  },
  loadFeedback() {
    request({ url: `/api/activities/${this.data.currentId}/evaluations` }).then(list => {
      const feedback = (list || []).map(item => Object.assign({}, item, {
        targetText: item.targetType === 'VOLUNTEER'
          ? `志愿者${item.targetUserName ? `：${item.targetUserName}` : ''}`
          : item.targetType === 'LEADER' ? '活动负责人' : '活动',
        canAdopt: item.targetType === 'ACTIVITY' || item.targetType === 'LEADER'
      }))
      const activityFeedback = feedback.filter(item => item.canAdopt)
      const total = activityFeedback.reduce((s, i) => s + Number(i.score || 0), 0)
      this.setData({
        feedback,
        averageScore: activityFeedback.length ? (total / activityFeedback.length).toFixed(1) : '0.0'
      })
    })
  },
  summary() {
    this.setData({ summaryLoading: true })
    request({ url: `/api/activities/${this.data.currentId}/evaluations/feedback/ai-summary`, method: 'POST' })
      .then(data => this.setData({ summaryText: data.summary || '' }))
      .finally(() => this.setData({ summaryLoading: false }))
  }
})
