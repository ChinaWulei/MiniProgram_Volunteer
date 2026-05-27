const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    id: null,
    activity: null,
    skills: [],
    checkin: {},
    registrations: [],
    showCancel: false,
    cancelItem: null,
    cancelReason: '',
    profile: null,
    evaluationScore: 5,
    evaluationContent: '',
    evaluationTargetUserId: null,
    isAdmin: false,
    buttonText: '立即报名',
    buttonDisabled: false
  },
  onLoad(options) {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ id: options.activityId || options.id, isAdmin: user && user.role === 'ADMIN' })
    this.load()
    if (user && user.role !== 'ADMIN') this.loadProfile()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({ url: `/api/activity/${this.data.id}` })
      .then(activity => {
        activity.startTimeText = this.formatTime(activity.startTime)
        activity.endTimeText = this.formatTime(activity.endTime)
        activity.signupStartTimeText = this.formatTime(activity.signupStartTime)
        activity.signupDeadlineText = this.formatTime(activity.signupDeadline)
        activity.checkinStartTimeText = this.formatTime(activity.checkinStartTime)
        activity.checkinEndTimeText = this.formatTime(activity.checkinEndTime)
        this.setData({ activity, skills: this.splitTags(activity.skillRequirements) })
        this.refreshButton(activity)
        if (!this.data.isAdmin) this.loadCheckinStatus()
        if (this.data.isAdmin) this.loadRegistrations()
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  loadRegistrations() {
    request({ url: '/api/registrations/admin', data: { activityId: this.data.id }, silent: true })
      .then(list => {
        const registrations = (list || []).map(item => Object.assign({}, item, {
          userId: item.userId || item.user_id,
          activityId: item.activityId || item.activity_id,
          userText: item.nickname || item.userName || '志愿者',
          avatarText: (item.nickname || item.userName || '志').substring(0, 1),
          createdText: this.formatTime(item.created_at || item.createdAt),
          evaluationScore: item.evaluationScore || 5,
          evaluationContent: item.evaluationContent || ''
        }))
        this.setData({ registrations })
      })
      .catch(() => {})
  },
  loadProfile() {
    request({ url: '/api/user/profile', silent: true })
      .then(profile => this.setData({ profile }))
      .catch(() => {})
  },
  formatTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : ''
  },
  loadCheckinStatus() {
    request({ url: `/api/activity/${this.data.id}/checkin/status`, silent: true })
      .then(checkin => this.setData({ checkin }))
      .catch(() => {})
  },
  splitTags(tags) {
    return (tags || '').split(',').map(item => item.trim()).filter(Boolean)
  },
  refreshButton(activity) {
    let buttonText = '立即报名'
    let buttonDisabled = false
    const now = new Date()
    if (activity.status === '已结束') {
      buttonText = '活动已结束'
      buttonDisabled = true
    } else if (activity.signupStartTime && new Date(String(activity.signupStartTime).replace(/-/g, '/')) > now) {
      buttonText = '报名未开始'
      buttonDisabled = true
    } else if (activity.signupDeadline && new Date(String(activity.signupDeadline).replace(/-/g, '/')) < now) {
      buttonText = '报名已截止'
      buttonDisabled = true
    } else if (activity.status === '已满员' || activity.remainingNumber <= 0) {
      buttonText = '名额已满'
      buttonDisabled = true
    } else if (activity.signupStatus === '待审核') {
      buttonText = '待审核'
      buttonDisabled = true
    } else if (activity.signupStatus === '已通过') {
      buttonText = '已通过'
      buttonDisabled = true
    }
    this.setData({ buttonText, buttonDisabled })
  },
  join() {
    if (this.data.buttonDisabled) return
    const credit = this.data.profile && this.data.profile.creditScore
    if (credit !== undefined && credit < 70) {
      wx.showModal({ title: '信用分受限', content: '当前信用分低于70分，暂不能报名，请联系管理员处理。', showCancel: false })
      return
    }
    const submit = () => {
      wx.showLoading({ title: '报名中' })
      request({ url: `/api/activity/${this.data.id}/signup`, method: 'POST' })
        .then(() => {
          wx.showToast({ title: '报名成功' })
          this.load()
        })
        .catch(() => {})
        .finally(() => wx.hideLoading())
    }
    if (credit !== undefined && credit < 80) {
      wx.showModal({
        title: '信用分提醒',
        content: `当前信用分 ${credit}，请确认能够按时参加并完成签到。`,
        success: res => { if (res.confirm) submit() }
      })
      return
    }
    submit()
  },
  contactAdmin() {
    const adminId = this.data.activity && this.data.activity.createdBy
    if (!adminId) {
      wx.showToast({ title: '暂无管理员联系人', icon: 'none' })
      return
    }
    request({ url: '/api/chat/conversations', method: 'POST', data: { targetUserId: adminId } })
      .then(data => wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${data.conversationId}&peerId=${adminId}&peerName=${encodeURIComponent('活动管理员')}` }))
      .catch(() => {})
  },
  goMatch() {
    wx.navigateTo({ url: `/pages/match/match?activityId=${this.data.id}` })
  },
  goVolunteer(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.userid}` })
  },
  chatVolunteer(e) {
    const userId = e.currentTarget.dataset.userid
    const name = e.currentTarget.dataset.name || '志愿者'
    request({ url: '/api/chat/conversations', method: 'POST', data: { targetUserId: userId } })
      .then(data => wx.navigateTo({ url: `/pages/chat-room/chat-room?conversationId=${data.conversationId}&peerId=${userId}&peerName=${encodeURIComponent(name)}` }))
      .catch(() => {})
  },
  reviewRegistration(e) {
    const id = e.currentTarget.dataset.id
    const status = e.currentTarget.dataset.status
    request({
      url: `/api/registrations/${id}/review`,
      method: 'PUT',
      data: { status, reviewRemark: '管理员审核' }
    }).then(() => {
      wx.showToast({ title: '已处理' })
      this.load()
    }).catch(() => {})
  },
  edit() {
    wx.navigateTo({ url: `/pages/admin/activity-publish/activity-publish?id=${this.data.id}` })
  },
  checkin() {
    wx.getLocation({
      type: 'gcj02',
      success: res => {
        wx.showLoading({ title: '签到中' })
        request({
          url: '/api/activity/checkin',
          method: 'POST',
          data: { activityId: this.data.id, latitude: res.latitude, longitude: res.longitude }
        }).then(checkin => {
          this.setData({ checkin })
          wx.showToast({ title: '签到成功' })
        }).catch(() => {}).finally(() => wx.hideLoading())
      },
      fail: () => wx.showToast({ title: '定位失败，请检查授权', icon: 'none' })
    })
  },
  goCheckinManage() {
    wx.navigateTo({ url: `/pages/admin/activity-checkin/activity-checkin?activityId=${this.data.id}` })
  },
  openCancel(e) {
    const item = this.data.registrations.find(row => row.id === e.currentTarget.dataset.id)
    this.setData({ showCancel: true, cancelItem: item, cancelReason: '' })
  },
  closeCancel() {
    this.setData({ showCancel: false, cancelItem: null, cancelReason: '' })
  },
  inputCancelReason(e) {
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
  setEvalScore(e) {
    this.setData({ evaluationScore: Number(e.detail.value) + 1 })
  },
  setRegEvalScore(e) {
    const id = e.currentTarget.dataset.id
    const index = this.data.registrations.findIndex(item => item.id === id)
    if (index >= 0) this.setData({ [`registrations[${index}].evaluationScore`]: Number(e.detail.value) })
  },
  inputEvaluation(e) {
    this.setData({ evaluationContent: e.detail.value })
  },
  inputRegEvaluation(e) {
    const id = e.currentTarget.dataset.id
    const index = this.data.registrations.findIndex(item => item.id === id)
    if (index >= 0) this.setData({ [`registrations[${index}].evaluationContent`]: e.detail.value })
  },
  pickEvaluationVolunteer(e) {
    this.setData({ evaluationTargetUserId: e.currentTarget.dataset.userid })
  },
  submitEvaluation() {
    const data = {
      targetType: this.data.isAdmin ? 'VOLUNTEER' : 'ACTIVITY',
      targetUserId: this.data.isAdmin ? this.data.evaluationTargetUserId : null,
      score: this.data.evaluationScore,
      content: this.data.evaluationContent
    }
    if (this.data.isAdmin && !data.targetUserId) {
      wx.showToast({ title: '请选择志愿者', icon: 'none' })
      return
    }
    request({ url: `/api/activities/${this.data.id}/evaluations`, method: 'POST', data })
      .then(() => {
        wx.showToast({ title: '评价成功' })
        this.setData({ evaluationContent: '', evaluationScore: 5, evaluationTargetUserId: null })
      })
      .catch(() => {})
  },
  submitRegEvaluation(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.registrations.find(row => row.id === id)
    if (!item) return
    this.submitVolunteerEvaluation(item)
  },
  submitAllEvaluations() {
    const items = this.data.registrations.filter(item => item.userId)
    if (!items.length) {
      wx.showToast({ title: '暂无可提交评价', icon: 'none' })
      return
    }
    wx.showLoading({ title: '提交中' })
    Promise.all(items.map(item => this.submitVolunteerEvaluation(item, true).catch(err => err)))
      .then(() => {
        wx.showToast({ title: '评价已提交' })
        this.loadRegistrations()
      })
      .finally(() => wx.hideLoading())
  },
  submitVolunteerEvaluation(item, silent) {
    return request({
      url: `/api/activities/${this.data.id}/evaluations`,
      method: 'POST',
      silent: !!silent,
      data: {
        targetType: 'VOLUNTEER',
        targetUserId: item.userId,
        score: item.evaluationScore || 5,
        content: item.evaluationContent || ''
      }
    }).then(() => {
      if (!silent) wx.showToast({ title: '评价已提交' })
    })
  }
})
