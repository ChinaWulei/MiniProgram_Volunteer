const { request } = require('../../../utils/request')

const statusTabs = [
  { label: '全部', value: '' },
  { label: '已签到', value: 'CHECKED_IN' },
  { label: '补签', value: 'MANUAL_CHECKED_IN' },
  { label: '未签到', value: 'NOT_CHECKED_IN' },
  { label: '迟到', value: 'LATE_CHECKED_IN' },
  { label: '缺勤', value: 'ABSENT' }
]

function statusText(status) {
  const map = {
    CHECKED_IN: '已签到',
    MANUAL_CHECKED_IN: '补签',
    NOT_CHECKED_IN: '未签到',
    LATE_CHECKED_IN: '迟到',
    ABSENT: '缺勤'
  }
  return map[status] || '未签到'
}

Page({
  data: {
    activityId: null,
    stats: {},
    list: [],
    tabs: statusTabs,
    status: '',
    keyword: '',
    showManual: false,
    manualUserId: null,
    manualReason: '',
    cancelUser: null,
    cancelReason: '',
    showCancel: false
  },
  onLoad(options) {
    this.setData({ activityId: options.activityId })
    this.load()
  },
  input(e) {
    this.setData({ keyword: e.detail.value })
  },
  pickStatus(e) {
    this.setData({ status: e.currentTarget.dataset.status })
    this.loadList()
  },
  load() {
    this.loadStats()
    this.loadList()
  },
  loadStats() {
    request({ url: `/api/admin/activities/${this.data.activityId}/checkin/statistics` })
      .then(stats => this.setData({ stats }))
      .catch(() => {})
  },
  loadList() {
    request({ url: `/api/admin/activities/${this.data.activityId}/checkin/list`, data: { status: this.data.status, keyword: this.data.keyword } })
      .then(list => this.setData({ list: (list || []).map(item => Object.assign({}, item, { statusText: statusText(item.status), avatarText: (item.nickname || item.name || '志').substring(0, 1) })) }))
      .catch(() => {})
  },
  openManual(e) {
    this.setData({ showManual: true, manualUserId: e.currentTarget.dataset.id, manualReason: '' })
  },
  closeManual() {
    this.setData({ showManual: false, manualUserId: null, manualReason: '' })
  },
  inputReason(e) {
    this.setData({ manualReason: e.detail.value })
  },
  submitManual() {
    request({
      url: `/api/admin/activities/${this.data.activityId}/checkin/manual`,
      method: 'POST',
      data: { userId: this.data.manualUserId, reason: this.data.manualReason }
    }).then(() => {
      wx.showToast({ title: '补签成功' })
      this.closeManual()
      this.load()
    }).catch(() => {})
  },
  openCancel(e) {
    this.setData({ showCancel: true, cancelUser: e.currentTarget.dataset, cancelReason: '' })
  },
  closeCancel() {
    this.setData({ showCancel: false, cancelUser: null, cancelReason: '' })
  },
  inputCancelReason(e) {
    this.setData({ cancelReason: e.detail.value })
  },
  submitCancel() {
    const regId = this.data.cancelUser && this.data.cancelUser.regid
    request({ url: `/api/registrations/${regId}/cancel`, method: 'POST', data: { reason: this.data.cancelReason } })
      .then(() => {
        wx.showToast({ title: '已取消报名' })
        this.closeCancel()
        this.load()
      })
      .catch(() => {})
  }
})
