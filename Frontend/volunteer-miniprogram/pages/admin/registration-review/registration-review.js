const { request } = require('../../../utils/request')

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

function normalize(item) {
  return Object.assign({}, item, {
    userText: item.nickname || item.userName || '志愿者',
    avatarText: (item.nickname || item.userName || '志').substring(0, 1),
    timeText: `${formatTime(item.startTime)} 至 ${formatTime(item.endTime)}`,
    createdText: formatTime(item.created_at || item.createdAt),
    displayStatus: item.displayStatus || item.status
  })
}

Page({
  data: {
    allList: [],
    list: [],
    keyword: '',
    status: '',
    statuses: ['全部', '待审核', '已通过', '已拒绝'],
    department: '',
    departments: ['全部', '数学系', '计算机系'],
    activityId: '',
    activityIndex: 0,
    activityName: '全部活动',
    activityOptions: [{ id: '', name: '全部活动' }],
    positionId: '',
    positionIndex: 0,
    positionTabs: [{ id: '', name: '全部岗位' }],
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
    this.loadDepartments()
    this.loadActivities()
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
  pickDepartment(e) {
    const value = this.data.departments[Number(e.detail.value)]
    this.setData({ department: value === '全部' ? '' : value })
    this.load()
  },
  pickActivity(e) {
    const activityIndex = Number(e.detail.value)
    const activity = this.data.activityOptions[activityIndex] || this.data.activityOptions[0]
    const activityId = activity.id || ''
    this.setData({
      activityIndex,
      activityId,
      activityName: activity.name,
      positionId: '',
      positionIndex: 0,
      positionTabs: [{ id: '', name: '全部岗位' }]
    })
    if (activityId) this.loadPositions(activityId)
    this.load()
  },
  pickPosition(e) {
    const positionIndex = Number(e.currentTarget.dataset.index)
    const position = this.data.positionTabs[positionIndex]
    if (!position) return
    this.setData({
      positionIndex,
      positionId: position.id || ''
    })
    this.applyPositionFilter()
  },
  loadDepartments() {
    request({ url: '/api/registrations/admin/departments', silent: true })
      .then(list => {
        const departments = ['数学系', '计算机系']
        ;(list || []).forEach(item => {
          if (item && departments.indexOf(item) < 0) departments.push(item)
        })
        this.setData({ departments: ['全部'].concat(departments) })
      })
      .catch(() => {})
  },
  loadActivities() {
    request({ url: '/api/activities', silent: true })
      .then(list => {
        const activityOptions = [{ id: '', name: '全部活动' }].concat(
          (list || []).map(item => ({
            id: item.id,
            name: item.name || item.title || `活动${item.id}`
          }))
        )
        const activityIndex = Math.max(
          0,
          activityOptions.findIndex(item => String(item.id) === String(this.data.activityId))
        )
        this.setData({
          activityOptions,
          activityIndex,
          activityName: activityOptions[activityIndex].name
        })
      })
      .catch(() => {})
  },
  loadPositions(activityId) {
    request({ url: `/api/activities/${activityId}`, silent: true })
      .then(activity => {
        if (String(this.data.activityId) !== String(activityId)) return
        const positionTabs = [{ id: '', name: '全部岗位' }].concat(
          (activity.positions || []).map(item => ({
            id: item.id,
            name: item.name || `岗位${item.id}`
          }))
        )
        this.setData({ positionTabs, positionId: '', positionIndex: 0 })
        this.applyPositionFilter()
      })
      .catch(() => {})
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({
      url: '/api/registrations/admin',
      data: {
        keyword: this.data.keyword,
        status: this.data.status,
        department: this.data.department,
        activityId: this.data.activityId
      }
    })
      .then(list => {
        this.setData({ allList: (list || []).map(normalize) })
        this.applyPositionFilter()
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  applyPositionFilter() {
    const positionId = this.data.positionId
    const list = positionId
      ? this.data.allList.filter(item =>
        String(item.position_id || item.positionId || '') === String(positionId)
      )
      : this.data.allList
    this.setData({ list })
  },
  review(e) {
    const item = this.data.list.find(row => row.id === e.currentTarget.dataset.id)
    if (item && item.isWaitlisted) {
      wx.showToast({ title: '候补由系统自动递补', icon: 'none' })
      return
    }
    request({
      url: `/api/registrations/${e.currentTarget.dataset.id}/review`,
      method: 'PUT',
      data: { status: e.currentTarget.dataset.status, reviewRemark: '管理员审核' }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.load()
    }).catch(() => {})
  },
  exportApproved(e) {
    const activityId = e.currentTarget.dataset.id
    if (!activityId) return
    const app = getApp()
    const baseUrl = (app.globalData.baseUrl || '').replace(/\/+$/, '')
    const token = app.globalData.token || wx.getStorageSync('token')
    wx.showLoading({ title: '生成名单中' })
    wx.downloadFile({
      url: `${baseUrl}/api/registrations/admin/activities/${activityId}/approved-export`,
      header: { Authorization: token || '' },
      success: res => {
        if (res.statusCode !== 200) {
          wx.showToast({ title: '名单导出失败', icon: 'none' })
          return
        }
        wx.openDocument({
          filePath: res.tempFilePath,
          fileType: 'xlsx',
          showMenu: true,
          fail: () => wx.showToast({ title: '无法打开名单文件', icon: 'none' })
        })
      },
      fail: () => wx.showToast({ title: '名单下载失败', icon: 'none' }),
      complete: () => wx.hideLoading()
    })
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
