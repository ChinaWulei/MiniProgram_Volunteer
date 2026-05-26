const { request } = require('../../utils/request')

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function normalize(item) {
  return Object.assign({}, item, {
    startTimeText: formatTime(item.startTime),
    endTimeText: formatTime(item.endTime)
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
  }
})
