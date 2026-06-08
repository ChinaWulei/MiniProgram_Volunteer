const { request } = require('../../../utils/request')

Page({
  data: {
    activities: [],
    keyword: '',
    loadingActivityId: null
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
    const id = Number(e.currentTarget.dataset.id)
    const index = this.data.activities.findIndex(item => Number(item.id) === id)
    if (index < 0) return
    const activity = this.data.activities[index]
    if (activity.expanded) {
      this.setData({ [`activities[${index}].expanded`]: false })
      return
    }
    this.setData({ [`activities[${index}].expanded`]: true })
    if (activity.feedbackLoaded) return
    this.loadFeedback(id, index)
  },
  loadFeedback(activityId, index) {
    this.setData({ loadingActivityId: activityId })
    request({ url: `/api/activities/${activityId}/evaluations` }).then(list => {
      const feedback = (list || []).map(item => Object.assign({}, item, {
        targetText: item.targetType === 'VOLUNTEER'
          ? `志愿者${item.targetUserName ? `：${item.targetUserName}` : ''}`
          : item.targetType === 'LEADER' ? '活动负责人' : '活动',
        canAdopt: item.targetType === 'ACTIVITY' || item.targetType === 'LEADER'
      }))
      const activityFeedback = feedback.filter(item => item.canAdopt)
      const total = activityFeedback.reduce((s, i) => s + Number(i.score || 0), 0)
      this.setData({
        [`activities[${index}].feedback`]: feedback,
        [`activities[${index}].feedbackLoaded`]: true,
        [`activities[${index}].averageScore`]: activityFeedback.length ? (total / activityFeedback.length).toFixed(1) : '0.0'
      })
    }).finally(() => this.setData({ loadingActivityId: null }))
  },
  summary(e) {
    const id = Number(e.currentTarget.dataset.id)
    const index = this.data.activities.findIndex(item => Number(item.id) === id)
    if (index < 0) return
    this.setData({ [`activities[${index}].summaryLoading`]: true })
    request({ url: `/api/activities/${id}/evaluations/feedback/ai-summary`, method: 'POST' })
      .then(data => this.setData({
        [`activities[${index}].summaryText`]: data.summary || '',
        [`activities[${index}].summaryItems`]: this.formatSummary(data.summary || '')
      }))
      .finally(() => this.setData({ [`activities[${index}].summaryLoading`]: false }))
  },
  formatSummary(text) {
    const raw = String(text || '').replace(/\r/g, '\n').trim()
    if (!raw) return []
    const normalized = raw
      .replace(/(总体评价|整体满意度|主要亮点|主要优点|集中问题|主要问题|改进建议|典型反馈)[:：]/g, '\n$1：')
      .replace(/([。；;])\s*(?=(总体评价|整体满意度|主要亮点|主要优点|集中问题|主要问题|改进建议|典型反馈)[:：])/g, '$1\n')
    const lines = normalized.split('\n')
      .map(item => item.replace(/^[-*•\d.、\s]+/, '').trim())
      .filter(Boolean)
    if (lines.length > 1) return lines
    return raw.split(/[。；;]/).map(item => item.trim()).filter(Boolean)
  },
  deleteEvaluation(e) {
    const activityId = Number(e.currentTarget.dataset.activityId)
    const id = Number(e.currentTarget.dataset.id)
    const index = this.data.activities.findIndex(item => Number(item.id) === activityId)
    if (index < 0 || !id) return
    wx.showModal({
      title: '删除评价',
      content: '确定删除这条评价吗？删除后对应提炼经验也会移除。',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/activities/${activityId}/evaluations/${id}`, method: 'DELETE' })
          .then(() => {
            wx.showToast({ title: '已删除' })
            this.setData({
              [`activities[${index}].feedbackLoaded`]: false,
              [`activities[${index}].evaluationCount`]: Math.max(0, Number(this.data.activities[index].evaluationCount || 0) - 1),
              [`activities[${index}].summaryText`]: '',
              [`activities[${index}].summaryItems`]: []
            })
            this.loadFeedback(activityId, index)
          })
          .catch(() => {})
      }
    })
  }
})
