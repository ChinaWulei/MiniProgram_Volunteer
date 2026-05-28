const { request } = require('../../../utils/request')

function statusText(status) {
  const map = {
    CHECKED_IN: '正常签到',
    LATE_CHECKED_IN: '迟到',
    MANUAL_CHECKED_IN: '补签',
    ABSENT: '缺勤',
    NOT_CHECKED_IN: '未签到'
  }
  return map[status] || status || '-'
}

function auditText(status) {
  const map = { PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回', SYSTEM: '异常记录' }
  return map[status] || status || '-'
}

function normalize(item) {
  return Object.assign({}, item, {
    originalStatusText: statusText(item.originalStatus || item.original_status),
    newStatusText: statusText(item.newStatus || item.new_status),
    auditStatusText: auditText(item.auditStatus || item.audit_status),
    proofImageUrl: item.proofImageUrl || item.proof_image_url,
    adminRemark: item.adminRemark || item.admin_remark,
    originalCheckinTime: item.originalCheckinTime || item.original_checkin_time,
    newCheckinTime: item.newCheckinTime || item.new_checkin_time
  })
}

Page({
  data: {
    list: [],
    keyword: '',
    auditStatus: '',
    statusOptions: ['全部', '待审核', '异常记录', '已通过', '已驳回'],
    statusValues: ['', 'PENDING', 'SYSTEM', 'APPROVED', 'REJECTED'],
    statusIndex: 0,
    showAudit: false,
    current: null,
    form: { auditStatus: 'APPROVED', newStatus: 'MANUAL_CHECKED_IN', adminRemark: '', newServiceHours: '', hoursReason: '' }
  },
  onShow() {
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({
      url: '/api/admin/checkin-adjustments/pending',
      data: { auditStatus: this.data.auditStatus, keyword: this.data.keyword }
    }).then(list => this.setData({ list: (list || []).map(normalize) }))
      .finally(() => wx.hideLoading())
  },
  inputKeyword(e) {
    this.setData({ keyword: e.detail.value })
  },
  pickStatus(e) {
    const index = Number(e.detail.value)
    this.setData({ statusIndex: index, auditStatus: this.data.statusValues[index] }, () => this.load())
  },
  search() {
    this.load()
  },
  openAudit(e) {
    const item = this.data.list[e.currentTarget.dataset.index]
    this.setData({
      showAudit: true,
      current: item,
      form: { auditStatus: 'APPROVED', newStatus: 'MANUAL_CHECKED_IN', adminRemark: '', newServiceHours: '', hoursReason: '' }
    })
  },
  closeAudit() {
    this.setData({ showAudit: false, current: null })
  },
  setAuditResult(e) {
    this.setData({ 'form.auditStatus': e.currentTarget.dataset.value })
  },
  setNewStatus(e) {
    this.setData({ 'form.newStatus': e.currentTarget.dataset.value })
  },
  inputForm(e) {
    this.setData({ [`form.${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  submitAudit() {
    const current = this.data.current
    const form = this.data.form
    if (!current) return
    const url = form.auditStatus === 'APPROVED' && current.audit_status === 'SYSTEM'
      ? `/api/admin/checkin-adjustments/${current.id}/update-status`
      : `/api/admin/checkin-adjustments/${current.id}/audit`
    request({
      url,
      method: 'POST',
      data: {
        auditStatus: form.auditStatus,
        newStatus: form.newStatus,
        adminRemark: form.adminRemark,
        newServiceHours: form.newServiceHours ? Number(form.newServiceHours) : null,
        hoursReason: form.hoursReason
      }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.setData({ showAudit: false, current: null })
      this.load()
    })
  },
  previewProof(e) {
    const url = e.currentTarget.dataset.url
    if (url) wx.previewImage({ urls: [url] })
  }
})
