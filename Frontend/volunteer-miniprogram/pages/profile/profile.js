const app = getApp()
const { request, uploadFile } = require('../../utils/request')

const activityCategories = ['迎新服务', '赛事保障', '校园讲解', '社区服务', '校园服务']
const activityReminderTemplateId = '3MyIDa1qmvqiDyydT6aQrnCrYrKvz8aq92raqwL0Qq4'
const activityReviewTemplateId = 'llzSkIXycuVZvp_JsaB36o80EhkKdAdFHbrvffvlSrQ'

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
    campusText: clean(profile.campus, '未填写校区'),
    departmentText: clean(profile.department, '未填写所属系'),
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
    ,campusOptions: ['东海岸校区', '桑浦山校区', '其他校区']
    ,departmentOptions: ['数学系', '计算机系']
    ,exams: []
    ,courses: []
    ,weekdayOptions: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
    ,parsingSchedule: false
  },
  onShow() {
    const user = app.globalData.user || wx.getStorageSync('user')
    this.setData({ isAdmin: user && user.role === 'ADMIN' })
    this.loadProfile()
    if (!user || user.role !== 'ADMIN') {
      this.loadActivitySubscription()
      this.loadExams()
      this.loadCourses()
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
  pickCampus(e) {
    this.setData({ 'form.campus': this.data.campusOptions[Number(e.detail.value)] })
  },
  pickDepartment(e) {
    this.setData({ 'form.department': this.data.departmentOptions[Number(e.detail.value)] })
  },
  loadExams() {
    request({ url: '/api/exam-schedules', silent: true })
      .then(exams => this.setData({ exams: (exams || []).map(item => Object.assign({}, item, {
        startTime: this.formatTime(item.startTime),
        endTime: this.formatTime(item.endTime)
      })) }))
      .catch(() => {})
  },
  addExam() {
    this.setData({ exams: this.data.exams.concat({ courseName: '', startTime: '', endTime: '', location: '' }) })
  },
  inputExam(e) {
    this.setData({ [`exams[${e.currentTarget.dataset.index}].${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  removeExam(e) {
    const exams = this.data.exams.slice()
    exams.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ exams })
  },
  saveExams() {
    const exams = this.data.exams.filter(item => item.courseName && item.startTime && item.endTime)
    request({ url: '/api/exam-schedules', method: 'PUT', data: { exams } })
      .then(() => wx.showToast({ title: '考试安排已保存' }))
      .catch(() => {})
  },
  loadCourses() {
    request({ url: '/api/course-schedules', silent: true })
      .then(courses => this.setData({ courses: (courses || []).map(item => this.normalizeCourse(item)) }))
      .catch(() => {})
  },
  normalizeCourse(item) {
    const weekday = Number(item.weekday) || 1
    return Object.assign({}, item, {
      weekday,
      weekdayText: this.data.weekdayOptions[weekday - 1],
      startTime: String(item.startTime || '').slice(0, 5),
      endTime: String(item.endTime || '').slice(0, 5)
    })
  },
  addCourse() {
    this.setData({
      courses: this.data.courses.concat({
        courseName: '', weekday: 1, weekdayText: '周一',
        startTime: '08:00', endTime: '09:40', location: ''
      })
    })
  },
  inputCourse(e) {
    this.setData({ [`courses[${e.currentTarget.dataset.index}].${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  pickCourseWeekday(e) {
    const index = Number(e.currentTarget.dataset.index)
    const optionIndex = Number(e.detail.value)
    this.setData({
      [`courses[${index}].weekday`]: optionIndex + 1,
      [`courses[${index}].weekdayText`]: this.data.weekdayOptions[optionIndex]
    })
  },
  pickCourseTime(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({ [`courses[${index}].${e.currentTarget.dataset.key}`]: e.detail.value })
  },
  removeCourse(e) {
    const courses = this.data.courses.slice()
    courses.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ courses })
  },
  saveCourses() {
    const courses = this.data.courses
    const incomplete = courses.some(item =>
      !item.courseName || !item.weekday || !item.startTime || !item.endTime
    )
    if (incomplete) {
      wx.showToast({ title: '请完整填写课程名称和时间', icon: 'none' })
      return
    }
    if (courses.some(item => item.endTime <= item.startTime)) {
      wx.showToast({ title: '课程结束时间须晚于开始时间', icon: 'none' })
      return
    }
    request({ url: '/api/course-schedules', method: 'PUT', data: { courses } })
      .then(() => wx.showToast({ title: '课程安排已保存' }))
      .catch(() => {})
  },
  uploadScheduleImage() {
    if (this.data.parsingSchedule) return
    const handle = filePath => {
      this.setData({ parsingSchedule: true })
      wx.showLoading({ title: 'AI识别课表中' })
      uploadFile({ url: '/api/course-schedules/parse-image', filePath })
        .then(data => {
          const parsed = (data.courses || []).map(item => this.normalizeCourse(item))
          this.setData({ courses: parsed })
          wx.showModal({
            title: '识别完成',
            content: `识别出 ${parsed.length} 个上课时段，请核对后点击“保存课程安排”。`,
            showCancel: false
          })
        })
        .catch(() => {})
        .finally(() => {
          this.setData({ parsingSchedule: false })
          wx.hideLoading()
        })
    }
    if (wx.chooseMedia) {
      wx.chooseMedia({
        count: 1,
        mediaType: ['image'],
        sourceType: ['album', 'camera'],
        success: res => handle(res.tempFiles[0].tempFilePath)
      })
    } else {
      wx.chooseImage({
        count: 1,
        sourceType: ['album', 'camera'],
        success: res => handle(res.tempFilePaths[0])
      })
    }
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
      tmplIds: [activityReminderTemplateId, activityReviewTemplateId],
      success: res => {
        if (res[activityReminderTemplateId] === 'accept' || res[activityReviewTemplateId] === 'accept') {
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
        profile = normalizeProfile(profile || {})
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
  myEvaluations() {
    wx.navigateTo({ url: '/pages/my-evaluations/my-evaluations' })
  },
  growthProfile() {
    wx.navigateTo({ url: '/pages/growth-profile/growth-profile' })
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
