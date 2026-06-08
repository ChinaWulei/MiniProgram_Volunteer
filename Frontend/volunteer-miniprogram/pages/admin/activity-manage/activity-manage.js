const { request } = require('../../../utils/request')

function normalize(item) {
  return Object.assign({}, item, {
    title: item.title || item.name,
    timeText: `${String(item.startTime || '').replace('T', ' ').slice(0, 16)} 至 ${String(item.endTime || '').replace('T', ' ').slice(0, 16)}`,
    remainingNumber: Math.max(Number(item.recruitNumber || 0) - Number(item.registeredNumber || 0), 0)
  })
}

Page({
  data: {
    list: [],
    keyword: '',
    status: '',
    statuses: ['全部', '已发布', '草稿', '已结束', '已取消', '报名中', '已满员'],
    showSummary: false,
    summaryText: '',
    showFeedback: false,
    feedbackActivityId: null,
    feedbackActivityName: '',
    feedbackList: [],
    feedbackCount: 0,
    feedbackAverage: '0.0',
    aiFeedbackSummary: '',
    aiSummaryLoading: false
  },
  onShow() {
    this.load()
  },
  input(e) {
    this.setData({ keyword: e.detail.value })
  },
  pickStatus(e) {
    const value = this.data.statuses[e.detail.value]
    this.setData({ status: value === '全部' ? '' : value })
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/activities', data: { keyword: this.data.keyword, status: this.data.status } })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  publish() {
    wx.navigateTo({ url: '/pages/admin/activity-publish/activity-publish' })
  },
  edit(e) {
    wx.navigateTo({ url: `/pages/admin/activity-publish/activity-publish?id=${e.currentTarget.dataset.id}` })
  },
  detail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${e.currentTarget.dataset.id}` })
  },
  checkin(e) {
    wx.navigateTo({ url: `/pages/admin/activity-checkin/activity-checkin?activityId=${e.currentTarget.dataset.id}` })
  },
  finish(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '结束活动',
      content: '确认将该活动标记为已结束？',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/admin/activities/${id}/finish`, method: 'POST' })
          .then(() => {
            wx.showToast({ title: '已结束' })
            wx.navigateTo({ url: `/pages/admin/activity-news-edit/activity-news-edit?activityId=${id}` })
          })
          .catch(() => {})
      }
    })
  },
  remove(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: '删除活动',
      content: '删除后不可恢复，确认删除？',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/admin/activities/${id}`, method: 'DELETE' })
          .then(() => { wx.showToast({ title: '已删除' }); this.load() })
          .catch(() => {})
      }
    })
  },
  summary(e) {
    wx.navigateTo({ url: `/pages/admin/activity-news-edit/activity-news-edit?activityId=${e.currentTarget.dataset.id}` })
  },
  feedback(e) {
    const id = e.currentTarget.dataset.id
    const activity = this.data.list.find(item => item.id === id)
    this.setData({
      showFeedback: true,
      feedbackActivityId: id,
      feedbackActivityName: activity ? activity.title : '活动',
      feedbackList: [],
      feedbackCount: 0,
      feedbackAverage: '0.0',
      aiFeedbackSummary: ''
    })
    wx.showLoading({ title: '加载评价' })
    request({ url: `/api/activities/${id}/evaluations/feedback` })
      .then(list => {
        const feedbackList = (list || []).map(item => ({
          ...item,
          targetText: item.targetType === 'LEADER' ? '活动负责人' : '活动',
          contentText: item.content || '未填写文字评价',
          createdText: String(item.createdAt || '').replace('T', ' ').slice(0, 16)
        }))
        const total = feedbackList.reduce((sum, item) => sum + Number(item.score || 0), 0)
        this.setData({
          feedbackList,
          feedbackCount: feedbackList.length,
          feedbackAverage: feedbackList.length ? (total / feedbackList.length).toFixed(1) : '0.0'
        })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  generateFeedbackSummary() {
    if (!this.data.feedbackCount || this.data.aiSummaryLoading) return
    this.setData({ aiSummaryLoading: true })
    request({
      url: `/api/activities/${this.data.feedbackActivityId}/evaluations/feedback/ai-summary`,
      method: 'POST'
    })
      .then(data => this.setData({ aiFeedbackSummary: data.summary || '暂无总结' }))
      .catch(() => {})
      .finally(() => this.setData({ aiSummaryLoading: false }))
  },
  closeFeedback() {
    this.setData({
      showFeedback: false,
      feedbackActivityId: null,
      feedbackList: [],
      aiFeedbackSummary: ''
    })
  },
  stopTap() {},
  closeSummary() {
    this.setData({ showSummary: false, summaryText: '' })
  },
  copySummary() {
    wx.setClipboardData({ data: this.data.summaryText })
  }
})
