const app = getApp()
const { request } = require('../../utils/request')

const colors = ['#0f766e', '#2563eb', '#f59e0b', '#dc2626', '#7c3aed', '#0891b2']

function num(value) {
  return Number(value || 0)
}

function time(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '-'
}

function percent(value) {
  return `${num(value).toFixed(1)}%`
}

Page({
  data: {
    isAdmin: false,
    current: {},
    cards: [],
    trend: [],
    rankList: [],
    categoryStats: [],
    history: [],
    generating: false,
    exporting: false
  },
  onLoad() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.loadHistory()
  },
  loadHistory() {
    request({ url: '/api/report/list' })
      .then(list => {
        const history = (list || []).map(item => Object.assign({}, item, { createdText: time(item.createdAt) }))
        this.setData({ history })
        if (!this.data.current.id && history.length) this.applyReport(history[0])
      })
  },
  generateReport() {
    if (this.data.generating) return
    this.setData({ generating: true })
    request({
      url: '/api/report/generate',
      method: 'POST',
      data: { reportType: this.data.isAdmin ? 'ADMIN' : 'VOLUNTEER' }
    })
      .then(report => {
        wx.showToast({ title: 'Generated' })
        this.applyReport(report)
        this.loadHistory()
      })
      .finally(() => this.setData({ generating: false }))
  },
  exportPdf() {
    if (!this.data.current.id || this.data.exporting) return
    this.setData({ exporting: true })
    request({ url: '/api/report/export', method: 'POST', data: { reportId: this.data.current.id } })
      .then(report => {
        wx.showToast({ title: 'PDF Ready' })
        this.applyReport(report)
        this.openPdf(report.pdfUrl)
        this.loadHistory()
      })
      .finally(() => this.setData({ exporting: false }))
  },
  selectHistory(e) {
    const id = e.currentTarget.dataset.id
    request({ url: `/api/report/detail/${id}` }).then(report => this.applyReport(report))
  },
  openHistoryPdf(e) {
    const id = e.currentTarget.dataset.id
    request({ url: '/api/report/export', method: 'POST', data: { reportId: id } })
      .then(report => {
        this.openPdf(report.pdfUrl)
        this.loadHistory()
      })
  },
  deleteHistory(e) {
    const id = e.currentTarget.dataset.id
    wx.showModal({
      title: 'Delete report',
      content: 'Delete this history report?',
      confirmText: 'Delete',
      confirmColor: '#dc2626',
      success: res => {
        if (!res.confirm) return
        request({ url: `/api/report/${id}`, method: 'DELETE' })
          .then(() => {
            wx.showToast({ title: 'Deleted' })
            if (String(this.data.current.id) === String(id)) this.setData({ current: {}, cards: [], trend: [], rankList: [], categoryStats: [] })
            this.loadHistory()
          })
      }
    })
  },
  openPdf(url) {
    if (!url) return
    wx.showLoading({ title: 'Opening' })
    wx.downloadFile({
      url,
      success: res => {
        if (res.statusCode === 200) {
          wx.openDocument({ filePath: res.tempFilePath, fileType: 'pdf', showMenu: true, fail: () => this.copyUrl(url) })
        } else {
          this.copyUrl(url)
        }
      },
      fail: () => this.copyUrl(url),
      complete: () => wx.hideLoading()
    })
  },
  copyUrl(url) {
    wx.setClipboardData({ data: url })
  },
  applyReport(report) {
    const stats = report && report.stats ? report.stats : {}
    this.setData({
      current: report || {},
      cards: this.buildCards(stats),
      trend: this.buildTrend(stats),
      rankList: this.buildRanks(stats),
      categoryStats: this.buildCategories(stats)
    })
  },
  buildCards(stats) {
    const overview = stats.overview || {}
    const adjustments = stats.adjustments || {}
    if (this.data.isAdmin) {
      return [
        { label: 'Activities', value: num(overview.activityCount) },
        { label: 'Volunteers', value: num(overview.volunteerCount) },
        { label: 'Registrations', value: num(overview.registrationCount) },
        { label: 'Participants', value: num(overview.actualParticipantCount) },
        { label: 'Total Hours', value: num(overview.totalServiceHours) },
        { label: 'Attendance', value: percent(overview.averageAttendanceRate) },
        { label: 'Absence', value: percent(overview.absenceRate) },
        { label: 'Adjustment Pass', value: percent(overview.adjustmentPassRate) }
      ]
    }
    return [
      { label: 'Total Hours', value: num(overview.totalServiceHours) },
      { label: 'Activities', value: num(overview.participatedActivityCount) },
      { label: 'Completed', value: num(overview.completedActivityCount) },
      { label: 'Attendance', value: percent(overview.attendanceRate) },
      { label: 'Late', value: num(overview.lateCount) },
      { label: 'Absent', value: num(overview.absentCount) },
      { label: 'Adjustments', value: num(adjustments.adjustmentCount) },
      { label: 'Approved', value: num(adjustments.adjustmentApprovedCount) }
    ]
  },
  buildTrend(stats) {
    const rows = stats.monthlyTrend || []
    const maxHours = Math.max.apply(null, rows.map(row => num(row.serviceHours)).concat([1]))
    const maxCount = Math.max.apply(null, rows.map(row => num(row.activityCount)).concat([1]))
    return rows.map(row => ({
      label: String(row.label || '').slice(5),
      hoursHeight: Math.max(8, Math.round(num(row.serviceHours) * 150 / maxHours)),
      countHeight: Math.max(8, Math.round(num(row.activityCount) * 150 / maxCount))
    }))
  },
  buildRanks(stats) {
    const rows = this.data.isAdmin ? (stats.hotActivities || []) : (stats.recentActivities || [])
    const max = Math.max.apply(null, rows.map(row => num(row.registrationCount || row.serviceHours)).concat([1]))
    return rows.slice(0, 8).map(row => {
      const value = num(row.registrationCount || row.serviceHours)
      return {
        name: row.name || row.activityName || '-',
        value,
        percent: Math.round(value * 100 / max)
      }
    })
  },
  buildCategories(stats) {
    const rows = this.data.isAdmin ? (stats.activityTypeDistribution || []) : (stats.categoryStats || [])
    const total = rows.reduce((sum, row) => sum + num(row.count), 0) || 1
    return rows.map((row, index) => ({
      category: row.category || 'Other',
      count: num(row.count),
      percent: Math.round(num(row.count) * 100 / total),
      color: colors[index % colors.length]
    }))
  },
  onShareAppMessage() {
    return {
      title: 'AI Volunteer Service Report',
      path: '/pages/report-center/report-center'
    }
  }
})
