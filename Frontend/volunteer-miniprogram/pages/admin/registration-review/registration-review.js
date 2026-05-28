const { request } = require('../../../utils/request')

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function normalize(item) {
  return Object.assign({}, item, {
    userText: item.nickname || item.userName || '志愿者',
    avatarText: (item.nickname || item.userName || '志').substring(0, 1),
    timeText: `${formatTime(item.startTime)} 至 ${formatTime(item.endTime)}`,
    createdText: formatTime(item.created_at || item.createdAt)
  })
}

Page({
  data: {
    list: [],
    keyword: '',
    status: '',
    statuses: ['全部', '待审核', '已通过', '已拒绝'],
    showCancel: false,
    cancelItem: null,
    cancelReason: ''
  },
  onLoad(options) {
    if (options && options.pending) {
      this.setData({ status: this.data.statuses[1] })
    }
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
    request({ url: '/api/registrations/admin', data: { keyword: this.data.keyword, status: this.data.status } })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  review(e) {
    request({
      url: `/api/registrations/${e.currentTarget.dataset.id}/review`,
      method: 'PUT',
      data: { status: e.currentTarget.dataset.status, reviewRemark: '管理员审核' }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.load()
    }).catch(() => {})
  },
  openCancel(e) {
    const item = this.data.list.find(row => row.id === e.currentTarget.dataset.id)
    this.setData({ showCancel: true, cancelItem: item, cancelReason: '' })
  },
  closeCancel() {
    this.setData({ showCancel: false, cancelItem: null, cancelReason: '' })
  },
  inputReason(e) {
    this.setData({ cancelReason: e.detail.value })
  },
  cancelRegistration() {
    request({
      url: `/api/registrations/${this.data.cancelItem.id}/cancel`,
      method: 'POST',
      data: { reason: this.data.cancelReason }
    }).then(() => {
      wx.showToast({ title: '已取消报名' })
      this.closeCancel()
      this.load()
    }).catch(() => {})
  },
  goActivity(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?id=${e.currentTarget.dataset.id}` })
  },
  goVolunteer(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` })
  }
})
