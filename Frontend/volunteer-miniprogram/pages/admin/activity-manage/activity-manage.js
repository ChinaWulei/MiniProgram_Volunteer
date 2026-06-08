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
    aiSummaryLoading: false,
    showNotice: false,
    noticeActivity: null,
    noticeForm: { title: '', content: '', scope: 'APPROVED' },
    noticeScopes: [
      { label: '已通过/已完成志愿者', value: 'APPROVED' },
      { label: '全部报名志愿者', value: 'ALL' },
      { label: '仅已完成志愿者', value: 'COMPLETED' }
    ],
    noticeScopeIndex: 0,
    noticeScopeLabel: '已通过/已完成志愿者',
    noticeSending: false
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
  openNotice(e) {
    const id = e.currentTarget.dataset.id
    const activity = this.data.list.find(item => item.id === id)
    this.setData({
      showNotice: true,
      noticeActivity: activity || { id },
      noticeForm: {
        title: activity ? `${activity.title}活动通知` : '活动通知',
        content: '',
        scope: 'APPROVED'
      },
      noticeScopeIndex: 0,
      noticeScopeLabel: '已通过/已完成志愿者'
    })
  },
  closeNotice() {
    if (this.data.noticeSending) return
    this.setData({ showNotice: false, noticeActivity: null })
  },
  inputNotice(e) {
    this.setData({ [`noticeForm.${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  pickNoticeScope(e) {
    const index = Number(e.detail.value || 0)
    const scope = this.data.noticeScopes[index] || this.data.noticeScopes[0]
    this.setData({
      noticeScopeIndex: index,
      noticeScopeLabel: scope.label,
      'noticeForm.scope': scope.value
    })
  },
  sendNotice() {
    const activity = this.data.noticeActivity
    const form = this.data.noticeForm
    if (!activity || this.data.noticeSending) return
    if (!form.title.trim()) {
      wx.showToast({ title: '请填写标题', icon: 'none' })
      return
    }
    if (!form.content.trim()) {
      wx.showToast({ title: '请填写内容', icon: 'none' })
      return
    }
    this.setData({ noticeSending: true })
    request({
      url: `/api/admin/activities/${activity.id}/notifications`,
      method: 'POST',
      data: form
    })
      .then(data => {
        wx.showToast({ title: `已发送${data.sentCount || 0}人`, icon: 'none' })
        this.setData({ showNotice: false, noticeActivity: null })
      })
      .catch(() => {})
      .finally(() => this.setData({ noticeSending: false }))
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
