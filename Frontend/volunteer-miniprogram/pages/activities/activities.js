const { request } = require('../../utils/request')

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function normalize(item) {
  return Object.assign({}, item, {
    startTimeText: formatTime(item.startTime),
    endTimeText: formatTime(item.endTime),
    aiExpanded: false,
    aiLoading: false,
    aiAnalysis: ''
  })
}

Page({
  data: {
    list: [],
    keyword: '',
    category: '',
    status: '',
    categories: ['全部', '迎新服务', '赛事保障', '校园讲解', '社区服务'],
    statuses: ['全部', '报名中', '已满员', '已结束']
  },
  onShow() {
    this.load()
  },
  setKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },
  pickCategory(e) {
    const value = this.data.categories[e.detail.value]
    this.setData({ category: value === '全部' ? '' : value })
  },
  pickStatus(e) {
    const value = this.data.statuses[e.detail.value]
    this.setData({ status: value === '全部' ? '' : value })
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/activities', data: { keyword: this.data.keyword, category: this.data.category, status: this.data.status } })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?activityId=${e.currentTarget.dataset.id}` })
  },
  toggleAiAnalysis(e) {
    const id = e.currentTarget.dataset.id
    const index = this.data.list.findIndex(item => String(item.id) === String(id))
    if (index < 0) return
    const item = this.data.list[index]
    if (item.aiExpanded) {
      this.setData({ [`list[${index}].aiExpanded`]: false })
      return
    }
    this.setData({ [`list[${index}].aiExpanded`]: true })
    if (item.aiAnalysis) return
    this.setData({ [`list[${index}].aiLoading`]: true })
    request({ url: `/api/activities/${id}/ai-analysis`, method: 'POST', silent: true })
      .then(data => {
        this.setData({ [`list[${index}].aiAnalysis`]: (data && data.analysis) || 'AI暂未返回分析结果' })
      })
      .catch(err => {
        this.setData({ [`list[${index}].aiAnalysis`]: (err && err.message) || 'AI分析失败，请稍后重试' })
      })
      .finally(() => this.setData({ [`list[${index}].aiLoading`]: false }))
  }
})
