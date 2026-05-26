const { request, uploadFile } = require('../../../utils/request')

const skillNames = ['摄影', '摄像', '文案', '讲解', '物资搬运', '秩序维护', '活动组织']

function compactSkills(options) {
  return options.filter(item => item.selected).map(item => item.name)
}

function datePart(value) {
  return value ? String(value).slice(0, 10) : ''
}

function timePart(value) {
  return value ? String(value).slice(11, 16) : ''
}

function combineDateTime(date, time) {
  if (!date || !time) return ''
  return `${date} ${time}`
}

Page({
  data: {
    id: null,
    form: {
      status: '已发布',
      auditMode: '管理员审核',
      recruitCount: 10,
      serviceHours: 2,
      category: '校园服务'
    },
    activityDate: '',
    activityClock: '',
    deadlineDate: '',
    deadlineClock: '',
    auditModes: ['管理员审核', '自动通过'],
    statuses: ['已发布', '草稿', '已结束', '已取消'],
    skillOptions: skillNames.map(name => ({ name, selected: false })),
    uploading: false,
    submitting: false
  },
  onLoad(options) {
    if (options.id) {
      this.setData({ id: options.id })
      wx.setNavigationBarTitle({ title: '编辑活动' })
      this.loadActivity(options.id)
    }
  },
  loadActivity(id) {
    wx.showLoading({ title: '加载中' })
    request({ url: `/api/activities/${id}` })
      .then(activity => {
        const skills = (activity.skillRequirements || '').split(',').map(item => item.trim()).filter(Boolean)
        const skillOptions = skillNames.map(name => ({ name, selected: skills.indexOf(name) >= 0 }))
        this.setData({
          skillOptions,
          activityDate: datePart(String(activity.startTime || '').replace('T', ' ')),
          activityClock: timePart(String(activity.startTime || '').replace('T', ' ')),
          deadlineDate: datePart(String(activity.signupDeadline || '').replace('T', ' ')),
          deadlineClock: timePart(String(activity.signupDeadline || '').replace('T', ' ')),
          form: {
            title: activity.name,
            coverImageUrl: activity.coverImageUrl,
            category: activity.category,
            activityTime: String(activity.startTime || '').replace('T', ' ').slice(0, 16),
            signupDeadline: String(activity.signupDeadline || '').replace('T', ' ').slice(0, 16),
            location: activity.location,
            locationAddress: activity.location,
            latitude: activity.latitude,
            longitude: activity.longitude,
            recruitCount: activity.recruitNumber,
            serviceHours: activity.serviceHours,
            requiredSkills: skills,
            description: activity.description,
            requirements: activity.signupRequirement,
            contactName: activity.contactName,
            contactPhone: activity.contactPhone,
            auditMode: activity.reviewMethod === '自动通过' ? '自动通过' : '管理员审核',
            status: activity.status || '已发布'
          }
        })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  input(e) {
    const key = e.currentTarget.dataset.key
    const numeric = key === 'recruitCount' || key === 'serviceHours'
    this.setData({ [`form.${key}`]: numeric ? Number(e.detail.value) : e.detail.value })
  },
  chooseLocation() {
    wx.chooseLocation({
      success: res => {
        const location = res.name || res.address || this.data.form.location
        this.setData({
          'form.location': location,
          'form.locationAddress': res.address || '',
          'form.latitude': res.latitude,
          'form.longitude': res.longitude
        })
      },
      fail: err => {
        const message = err && err.errMsg && err.errMsg.indexOf('auth deny') >= 0 ? '请允许使用位置权限' : '未选择签到地点'
        wx.showToast({ title: message, icon: 'none' })
      }
    })
  },
  pickActivityDate(e) {
    const activityDate = e.detail.value
    this.setData({ activityDate, 'form.activityTime': combineDateTime(activityDate, this.data.activityClock) })
  },
  pickActivityClock(e) {
    const activityClock = e.detail.value
    this.setData({ activityClock, 'form.activityTime': combineDateTime(this.data.activityDate, activityClock) })
  },
  pickDeadlineDate(e) {
    const deadlineDate = e.detail.value
    this.setData({ deadlineDate, 'form.signupDeadline': combineDateTime(deadlineDate, this.data.deadlineClock) })
  },
  pickDeadlineClock(e) {
    const deadlineClock = e.detail.value
    this.setData({ deadlineClock, 'form.signupDeadline': combineDateTime(this.data.deadlineDate, deadlineClock) })
  },
  pickAudit(e) {
    this.setData({ 'form.auditMode': this.data.auditModes[e.detail.value] })
  },
  pickStatus(e) {
    this.setData({ 'form.status': this.data.statuses[e.detail.value] })
  },
  toggleSkill(e) {
    const name = e.currentTarget.dataset.name
    const skillOptions = this.data.skillOptions.map(item => item.name === name ? Object.assign({}, item, { selected: !item.selected }) : item)
    this.setData({ skillOptions, 'form.requiredSkills': compactSkills(skillOptions) })
  },
  chooseCover() {
    const handle = file => {
      const path = file.tempFilePath || file.path
      const size = file.size || 0
      if (size > 5 * 1024 * 1024) {
        wx.showToast({ title: '图片不能超过5MB', icon: 'none' })
        return
      }
      const lower = path.toLowerCase()
      if (!lower.endsWith('.jpg') && !lower.endsWith('.jpeg') && !lower.endsWith('.png') && !lower.endsWith('.webp')) {
        wx.showToast({ title: '仅支持jpg/png/webp', icon: 'none' })
        return
      }
      this.setData({ uploading: true })
      wx.showLoading({ title: '上传中' })
      uploadFile({ url: '/api/admin/activity/image', filePath: path, name: 'file' })
        .then(data => this.setData({ 'form.coverImageUrl': data.coverImageUrl || data.url }))
        .catch(() => {})
        .finally(() => {
          this.setData({ uploading: false })
          wx.hideLoading()
        })
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: res => handle(res.tempFiles[0])
      })
    } else {
      wx.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: res => handle({ tempFilePath: res.tempFilePaths[0], size: 0 })
      })
    }
  },
  submit() {
    if (this.data.submitting || this.data.uploading) return
    const form = Object.assign({}, this.data.form, { requiredSkills: compactSkills(this.data.skillOptions) })
    if (!form.title || !form.activityTime || !form.location || !form.recruitCount || !form.serviceHours) {
      wx.showToast({ title: '请填写必填信息', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    wx.showLoading({ title: '发布中' })
    const method = this.data.id ? 'PUT' : 'POST'
    const url = this.data.id ? `/api/activities/${this.data.id}` : '/api/admin/activities'
    request({ url, method, data: form })
      .then(data => {
        wx.showToast({ title: this.data.id ? '保存成功' : '发布成功' })
        const id = this.data.id || data.id
        setTimeout(() => wx.redirectTo({ url: `/pages/activity-detail/activity-detail?id=${id}` }), 600)
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ submitting: false })
        wx.hideLoading()
      })
  }
})
