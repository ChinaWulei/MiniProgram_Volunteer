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
    summaryText: ''
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
  closeSummary() {
    this.setData({ showSummary: false, summaryText: '' })
  },
  copySummary() {
    wx.setClipboardData({ data: this.data.summaryText })
  }
})
