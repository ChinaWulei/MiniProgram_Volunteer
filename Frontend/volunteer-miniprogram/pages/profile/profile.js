const app = getApp()
const { request, uploadFile } = require('../../utils/request')

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
    editing: false,
    isAdmin: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.loadProfile()
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
  logout() {
    app.globalData.token = ''
    app.globalData.user = null
    wx.removeStorageSync('token')
    wx.removeStorageSync('user')
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
