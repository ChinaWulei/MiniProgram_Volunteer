const app = getApp()
const { request, uploadFile } = require('../../utils/request')

const activityCategories = ['迎新服务', '赛事保障', '校园讲解', '社区服务', '校园服务']
const activityReminderTemplateId = '3MyIDa1qmvqiDyydT6aQrnCrYrKvz8aq92raqwL0Qq4'

function bindWechatOpenid() {
  if (!wx.login) return
  wx.login({
    success(res) {
      if (!res.code) return
      request({ url: '/api/user/wechat-openid', method: 'POST', data: { code: res.code }, silent: true }).catch(() => {})
    }
  })
}

const skillNames = ['摄影', '摄像', '文案', '讲解', '物资搬运', '秩序维护', '疾病维护', '活动组织']

function clean(value, fallback) {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return fallback
  return text
}

function normalizeProfile(profile) {
  return Object.assign({}, profile, {
    nicknameText: clean(profile.nickname || profile.name, '未登录'),
    collegeText: clean(profile.college, '数计学院'),
    majorClassText: clean(profile.majorClass, '未填写专业班级'),
    phoneText: clean(profile.phone, '-'),
    availableTimeText: clean(profile.availableTime, '-'),
    bioText: clean(profile.bio, '-'),
    volunteerLevelText: clean(profile.volunteerLevel, 'Lv1'),
    levelNameText: clean(profile.levelName, '新星志愿者')
  })
}

Page({
  data: {
    profile: {},
    form: {},
    avatarText: '志',
    skillOptions: skillNames.map(name => ({ name, selected: false })),
    selectedSkills: [],
    activityCategoryOptions: activityCategories.map(name => ({ name, selected: false })),
    selectedActivityCategories: [],
    customActivityCategory: '',
    subscriptionEnabled: false,
    wechatReminderEnabled: true,
    emailReminderEnabled: false,
    reminderEmail: '',
    editing: false,
    isAdmin: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.loadProfile()
    if (!user || user.role !== 'ADMIN') {
      this.loadActivitySubscription()
    }
  },
  loadProfile() {
    wx.showLoading({ title: '加载中' })
    request({ url: '/api/user/profile' })
      .then(profile => {
        profile = normalizeProfile(profile || {})
        const selectedSkills = this.splitTags(profile.skillTags)
        profile.creditRecords = (profile.creditRecords || []).map(item => Object.assign({}, item, {
          changeText: item.changeValue > 0 ? `+${item.changeValue}` : String(item.changeValue),
          createdText: this.formatTime(item.createdAt || item.created_at)
        }))
        this.setData({
          profile,
          form: Object.assign({}, profile),
          selectedSkills,
          skillOptions: this.buildSkillOptions(selectedSkills),
          avatarText: this.avatarText(profile)
        })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  avatarText(profile) {
    return (profile.nickname || profile.name || '志').substring(0, 1)
  },
  splitTags(tags) {
    return (tags || '').split(',').map(item => item.trim()).filter(Boolean)
  },
  formatTime(value) {
    return value ? String(value).replace('T', ' ').slice(0, 16) : ''
  },
  buildSkillOptions(selectedSkills) {
    return skillNames.map(name => ({ name, selected: selectedSkills.indexOf(name) >= 0 }))
  },
  buildActivityCategoryOptions(selectedCategories) {
    const names = activityCategories.slice()
    ;(selectedCategories || []).forEach(name => {
      if (name && names.indexOf(name) < 0) names.push(name)
    })
    return names.map(name => ({ name, selected: selectedCategories.indexOf(name) >= 0 }))
  },
  loadActivitySubscription() {
    request({ url: '/api/activity-subscriptions', silent: true })
      .then(data => {
        const selected = data && data.categories ? data.categories : []
        this.setData({
          selectedActivityCategories: selected,
          activityCategoryOptions: this.buildActivityCategoryOptions(selected),
          subscriptionEnabled: !!(data && data.enabled),
          wechatReminderEnabled: data && data.wechatEnabled !== undefined ? !!data.wechatEnabled : true,
          emailReminderEnabled: !!(data && data.emailEnabled),
          reminderEmail: data && data.email ? data.email : ''
        })
      })
      .catch(() => {})
  },
  chooseAvatar() {
    const handlePath = filePath => {
      wx.showLoading({ title: '上传中' })
      uploadFile({ url: '/api/user/avatar', filePath })
        .then(data => {
          this.setData({ 'profile.avatarUrl': data.avatarUrl, 'form.avatarUrl': data.avatarUrl })
          wx.showToast({ title: '头像已更新' })
        })
        .catch(() => {})
        .finally(() => wx.hideLoading())
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: res => handlePath(res.tempFiles[0].tempFilePath),
        fail: () => wx.showToast({ title: '未选择图片', icon: 'none' })
      })
    } else {
      wx.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: res => handlePath(res.tempFilePaths[0]),
        fail: () => wx.showToast({ title: '未选择图片', icon: 'none' })
      })
    }
  },
  startEdit() {
    const selectedSkills = this.splitTags(this.data.profile.skillTags)
    this.setData({ editing: true, form: Object.assign({}, this.data.profile), selectedSkills, skillOptions: this.buildSkillOptions(selectedSkills) })
  },
  cancelEdit() {
    const selectedSkills = this.splitTags(this.data.profile.skillTags)
    this.setData({ editing: false, form: Object.assign({}, this.data.profile), selectedSkills, skillOptions: this.buildSkillOptions(selectedSkills) })
  },
  input(e) {
    const key = e.currentTarget.dataset.key
    this.setData({ [`form.${key}`]: e.detail.value })
  },
  inputReminderEmail(e) {
    this.setData({ reminderEmail: e.detail.value })
  },
  inputCustomActivityCategory(e) {
    this.setData({ customActivityCategory: e.detail.value })
  },
  toggleWechatReminder(e) {
    this.setData({ wechatReminderEnabled: e.detail.value })
  },
  toggleEmailReminder(e) {
    this.setData({ emailReminderEnabled: e.detail.value })
  },
  toggleSkill(e) {
    const skill = e.currentTarget.dataset.skill
    const selected = this.data.selectedSkills.slice()
    const index = selected.indexOf(skill)
    if (index >= 0) {
      selected.splice(index, 1)
    } else {
      selected.push(skill)
    }
    this.setData({ selectedSkills: selected, skillOptions: this.buildSkillOptions(selected), 'form.skillTags': selected.join(',') })
  },
  toggleActivityCategory(e) {
    const category = e.currentTarget.dataset.category
    const selected = this.data.selectedActivityCategories.slice()
    const index = selected.indexOf(category)
    if (index >= 0) {
      selected.splice(index, 1)
    } else {
      selected.push(category)
    }
    this.setData({
      selectedActivityCategories: selected,
      activityCategoryOptions: this.buildActivityCategoryOptions(selected),
      subscriptionEnabled: selected.length > 0
    })
  },
  addActivityCategory() {
    const category = String(this.data.customActivityCategory || '').trim()
    if (!category) {
      wx.showToast({ title: '请输入活动方向', icon: 'none' })
      return
    }
    if (category.length > 20) {
      wx.showToast({ title: '活动方向过长', icon: 'none' })
      return
    }
    const selected = this.data.selectedActivityCategories.slice()
    if (selected.indexOf(category) < 0) {
      selected.push(category)
    }
    this.setData({
      selectedActivityCategories: selected,
      activityCategoryOptions: this.buildActivityCategoryOptions(selected),
      customActivityCategory: '',
      subscriptionEnabled: selected.length > 0
    })
  },
  saveActivityReminder() {
    const categories = this.data.selectedActivityCategories.slice()
    const email = String(this.data.reminderEmail || '').trim()
    const emailEnabled = !!this.data.emailReminderEnabled
    const wechatWanted = !!this.data.wechatReminderEnabled
    bindWechatOpenid()
    if (!categories.length) {
      request({ url: '/api/activity-subscriptions', method: 'PUT', data: { enabled: false, wechatEnabled: false, emailEnabled: false, email: '', categories: [] } })
        .then(() => {
          this.setData({ subscriptionEnabled: false })
          wx.showToast({ title: '已关闭活动提醒', icon: 'none' })
        })
        .catch(() => {})
      return
    }
    if (!wechatWanted && !emailEnabled) {
      wx.showToast({ title: '请选择提醒方式', icon: 'none' })
      return
    }
    if (emailEnabled && !/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(email)) {
      wx.showToast({ title: '请输入有效邮箱', icon: 'none' })
      return
    }
    const save = wechatEnabled => {
      request({ url: '/api/activity-subscriptions', method: 'PUT', data: { enabled: true, wechatEnabled, emailEnabled, email, categories } })
        .then(() => {
          this.setData({ subscriptionEnabled: true })
          wx.showToast({ title: '已保存提醒设置', icon: 'success' })
        })
        .catch(() => {})
    }
    if (!wechatWanted || !wx.requestSubscribeMessage) {
      save(false)
      return
    }
    wx.requestSubscribeMessage({
      tmplIds: [activityReminderTemplateId],
      success: res => {
        if (res[activityReminderTemplateId] === 'accept') {
          save(true)
        } else {
          if (emailEnabled) save(false)
          wx.showToast({ title: '未授权微信提醒', icon: 'none' })
        }
      },
      fail: () => {
        if (emailEnabled) save(false)
        wx.showToast({ title: '订阅授权失败', icon: 'none' })
      }
    })
  },
  saveProfile() {
    const form = Object.assign({}, this.data.form, { skillTags: this.data.selectedSkills.join(',') })
    wx.showLoading({ title: '保存中' })
    request({ url: '/api/user/profile', method: 'PUT', data: form })
      .then(profile => {
        const selectedSkills = this.splitTags(profile.skillTags)
        this.setData({
          profile,
          form: Object.assign({}, profile),
          editing: false,
          selectedSkills,
          skillOptions: this.buildSkillOptions(selectedSkills),
          avatarText: this.avatarText(profile)
        })
        wx.showToast({ title: '保存成功' })
      })
      .catch(() => {})
      .finally(() => wx.hideLoading())
  },
  myRegs() {
    wx.navigateTo({ url: '/pages/my-registrations/my-registrations' })
  },
  goAdmin() {
    wx.navigateTo({ url: '/pages/admin/admin' })
  },
  goReport() {
    wx.navigateTo({ url: '/pages/report-center/report-center' })
  },
  logout() {
    app.globalData.token = ''
    app.globalData.user = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('user')
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
