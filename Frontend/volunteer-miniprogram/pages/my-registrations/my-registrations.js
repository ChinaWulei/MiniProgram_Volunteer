const { request, uploadFile } = require('../../utils/request')
const app = getApp()

function statusText(status) {
  const map = {
    CHECKED_IN: '正常签到',
    LATE_CHECKED_IN: '迟到',
    MANUAL_CHECKED_IN: '补签通过',
    ABSENT: '缺勤',
    NOT_CHECKED_IN: '未签到'
  }
  return map[status] || status || '未签到'
}

function auditText(status) {
  const map = {
    PENDING: '待审核',
    APPROVED: '已通过',
    REJECTED: '已驳回',
    SYSTEM: '异常记录'
  }
  return map[status] || ''
}

function normalize(item) {
  const checkinStatus = item.checkin_status || item.checkinStatus
  const auditStatus = item.adjustment_status || item.adjustmentStatus
  return Object.assign({}, item, {
    checkinStatus,
    checkinStatusText: statusText(checkinStatus),
    adjustmentStatusText: auditText(auditStatus),
    canApplyAdjustment: ['ABSENT', 'LATE_CHECKED_IN'].indexOf(checkinStatus) >= 0 && auditStatus !== 'PENDING' && auditStatus !== 'APPROVED'
  })
}

Page({
  data: {
    list: [],
    showApply: false,
    applyItem: null,
    applyForm: { reason: '', description: '', proofImageUrl: '' },
    uploading: false,
    submitting: false
  },
  onShow() {
    const token = app.globalData.token || wx.getStorageSync('token')
    if (!token) {
      wx.redirectTo({ url: '/pages/login/login' })
      return
    }
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/user/signup/list' })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?activityId=${e.currentTarget.dataset.id}` })
  },
  openApply(e) {
    const item = this.data.list[e.currentTarget.dataset.index]
    this.setData({
      showApply: true,
      applyItem: item,
      applyForm: { reason: '', description: '', proofImageUrl: '' }
    })
  },
  closeApply() {
    if (this.data.submitting) return
    this.setData({ showApply: false, applyItem: null })
  },
  inputApply(e) {
    this.setData({ [`applyForm.${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  chooseProof() {
    if (this.data.uploading) return
    wx.chooseImage({
      count: 1,
      success: res => {
        const filePath = res.tempFilePaths[0]
        this.setData({ uploading: true })
        uploadFile({ url: '/api/checkin-adjustments/proof', filePath, name: 'file' })
          .then(data => this.setData({ 'applyForm.proofImageUrl': data.proofImageUrl || data.url }))
          .finally(() => this.setData({ uploading: false }))
      }
    })
  },
  submitApply() {
    const item = this.data.applyItem
    const form = this.data.applyForm
    if (!item || this.data.submitting) return
    if (!form.reason.trim()) {
      wx.showToast({ title: '请填写补签原因', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    request({
      url: '/api/checkin-adjustments/apply',
      method: 'POST',
      data: {
        activityId: item.activity_id || item.activityId,
        reason: form.reason,
        description: form.description,
        proofImageUrl: form.proofImageUrl
      }
    }).then(() => {
      wx.showToast({ title: '已提交审核' })
      this.setData({ showApply: false, applyItem: null })
      this.load()
    }).finally(() => this.setData({ submitting: false }))
  }
})
