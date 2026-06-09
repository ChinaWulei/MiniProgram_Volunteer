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

function uniqueSkills(existing, generated) {
  const names = existing.map(item => item.name)
  ;(generated || []).forEach(name => {
    if (name && names.indexOf(name) < 0) {
      existing.push({ name, selected: true })
      names.push(name)
    }
  })
  return existing.map(item => (generated || []).indexOf(item.name) >= 0 ? Object.assign({}, item, { selected: true }) : item)
}

function positionForm(item) {
  const startTime = String(item.startTime || '').replace('T', ' ').slice(0, 16)
  const endTime = String(item.endTime || '').replace('T', ' ').slice(0, 16)
  const rehearsalStartTime = String(item.rehearsalStartTime || '').replace('T', ' ').slice(0, 16)
  const rehearsalEndTime = String(item.rehearsalEndTime || '').replace('T', ' ').slice(0, 16)
  return Object.assign({}, item, {
    startTime,
    endTime,
    rehearsalStartTime,
    rehearsalEndTime,
    startDate: datePart(startTime),
    startClock: timePart(startTime),
    endDate: datePart(endTime),
    endClock: timePart(endTime),
    rehearsalStartDate: datePart(rehearsalStartTime),
    rehearsalStartClock: timePart(rehearsalStartTime),
    rehearsalEndDate: datePart(rehearsalEndTime),
    rehearsalEndClock: timePart(rehearsalEndTime)
  })
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
    activityEndDate: '',
    activityEndClock: '',
    signupStartDate: '',
    signupStartClock: '',
    deadlineDate: '',
    deadlineClock: '',
    checkinStartDate: '',
    checkinStartClock: '',
    checkinEndDate: '',
    checkinEndClock: '',
    auditModes: ['管理员审核', '自动通过'],
    statuses: ['已发布', '草稿', '已结束', '已取消'],
    skillOptions: skillNames.map(name => ({ name, selected: false })),
    generatedFields: {},
    aiPanelVisible: false,
    aiPrompt: '',
    aiGenerating: false,
    aiCoverGenerating: false,
    aiLoadingText: 'AI正在生成活动内容与封面...',
    uploading: false,
    submitting: false,
    positions: [],
    priorityRules: [],
    priorityRuleTypes: ['历史活动', '系别', '校区', '技能', '最低信用分', '最低服务时长']
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
          activityEndDate: datePart(String(activity.endTime || '').replace('T', ' ')),
          activityEndClock: timePart(String(activity.endTime || '').replace('T', ' ')),
          signupStartDate: datePart(String(activity.signupStartTime || '').replace('T', ' ')),
          signupStartClock: timePart(String(activity.signupStartTime || '').replace('T', ' ')),
          deadlineDate: datePart(String(activity.signupDeadline || '').replace('T', ' ')),
          deadlineClock: timePart(String(activity.signupDeadline || '').replace('T', ' ')),
          checkinStartDate: datePart(String(activity.checkinStartTime || '').replace('T', ' ')),
          checkinStartClock: timePart(String(activity.checkinStartTime || '').replace('T', ' ')),
          checkinEndDate: datePart(String(activity.checkinEndTime || '').replace('T', ' ')),
          checkinEndClock: timePart(String(activity.checkinEndTime || '').replace('T', ' ')),
          form: {
            title: activity.name,
            coverImageUrl: activity.coverImageUrl,
            category: activity.category,
            activityTime: String(activity.startTime || '').replace('T', ' ').slice(0, 16),
            endTime: String(activity.endTime || '').replace('T', ' ').slice(0, 16),
            signupStartTime: String(activity.signupStartTime || '').replace('T', ' ').slice(0, 16),
            signupDeadline: String(activity.signupDeadline || '').replace('T', ' ').slice(0, 16),
            checkinStartTime: String(activity.checkinStartTime || '').replace('T', ' ').slice(0, 16),
            checkinEndTime: String(activity.checkinEndTime || '').replace('T', ' ').slice(0, 16),
            location: activity.location,
            locationAddress: activity.location,
            latitude: activity.latitude,
            longitude: activity.longitude,
            recruitCount: activity.recruitNumber,
            serviceHours: activity.serviceHours,
            requiredSkills: skills,
            description: activity.description,
            requirements: activity.signupRequirement,
            tips: activity.tips,
            contactName: activity.contactName,
            contactPhone: activity.contactPhone,
            auditMode: activity.reviewMethod === '自动通过' ? '自动通过' : '管理员审核',
            status: activity.status || '已发布'
          },
          positions: (activity.positions || []).map(item => positionForm({
            id: item.id,
            name: item.name,
            startTime: item.startTime,
            endTime: item.endTime,
            recruitNumber: item.recruitNumber,
            requirements: item.requirements || '',
            requiresRehearsal: !!item.requiresRehearsal,
            rehearsalStartTime: item.rehearsalStartTime,
            rehearsalEndTime: item.rehearsalEndTime
          })),
          priorityRules: (activity.priorityRules || []).map(item => ({
            id: item.id,
            ruleType: item.ruleType || '历史活动',
            ruleValue: item.ruleValue || '',
            weight: Number(item.weight) || 10
          }))
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
  addPosition() {
    this.setData({
      positions: this.data.positions.concat(positionForm({
        name: '',
        startTime: this.data.form.activityTime || '',
        endTime: this.data.form.endTime || '',
        recruitNumber: 1,
        requirements: '',
        requiresRehearsal: false,
        rehearsalStartTime: '',
        rehearsalEndTime: ''
      }))
    })
  },
  inputPosition(e) {
    const key = e.currentTarget.dataset.key
    const value = key === 'recruitNumber' ? Number(e.detail.value) : e.detail.value
    this.setData({ [`positions[${e.currentTarget.dataset.index}].${key}`]: value })
  },
  togglePositionRehearsal(e) {
    const index = e.currentTarget.dataset.index
    const enabled = e.detail.value
    const updates = { [`positions[${index}].requiresRehearsal`]: enabled }
    if (!enabled) {
      Object.assign(updates, {
        [`positions[${index}].rehearsalStartDate`]: '',
        [`positions[${index}].rehearsalStartClock`]: '',
        [`positions[${index}].rehearsalEndDate`]: '',
        [`positions[${index}].rehearsalEndClock`]: '',
        [`positions[${index}].rehearsalStartTime`]: '',
        [`positions[${index}].rehearsalEndTime`]: ''
      })
    }
    this.setData(updates)
  },
  pickPositionDate(e) {
    this.updatePositionTimePart(e.currentTarget.dataset.index, e.currentTarget.dataset.field, 'Date', e.detail.value)
  },
  pickPositionClock(e) {
    this.updatePositionTimePart(e.currentTarget.dataset.index, e.currentTarget.dataset.field, 'Clock', e.detail.value)
  },
  updatePositionTimePart(index, field, part, value) {
    const item = this.data.positions[index]
    const dateKey = `${field}Date`
    const clockKey = `${field}Clock`
    const date = part === 'Date' ? value : item[dateKey]
    const clock = part === 'Clock' ? value : item[clockKey]
    this.setData({
      [`positions[${index}].${field}${part}`]: value,
      [`positions[${index}].${field}Time`]: combineDateTime(date, clock)
    })
  },
  removePosition(e) {
    const positions = this.data.positions.slice()
    positions.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ positions })
  },
  addPriorityRule() {
    this.setData({
      priorityRules: this.data.priorityRules.concat({
        ruleType: '历史活动',
        ruleValue: '',
        weight: 20
      })
    })
  },
  pickPriorityRuleType(e) {
    const index = Number(e.currentTarget.dataset.index)
    this.setData({
      [`priorityRules[${index}].ruleType`]: this.data.priorityRuleTypes[Number(e.detail.value)],
      [`priorityRules[${index}].ruleValue`]: ''
    })
  },
  inputPriorityRule(e) {
    const key = e.currentTarget.dataset.key
    const value = key === 'weight' ? Number(e.detail.value) : e.detail.value
    this.setData({ [`priorityRules[${e.currentTarget.dataset.index}].${key}`]: value })
  },
  removePriorityRule(e) {
    const priorityRules = this.data.priorityRules.slice()
    priorityRules.splice(Number(e.currentTarget.dataset.index), 1)
    this.setData({ priorityRules })
  },
  openAiGenerate() {
    this.setData({ aiPanelVisible: true })
  },
  closeAiGenerate() {
    if (this.data.aiGenerating) return
    this.setData({ aiPanelVisible: false })
  },
  noop() {},
  inputAiPrompt(e) {
    this.setData({ aiPrompt: e.detail.value })
  },
  aiGenerate(e) {
    if (this.data.aiGenerating) return
    const mode = e.currentTarget.dataset.mode || 'FULL'
    const prompt = (this.data.aiPrompt || '').trim()
    if (!prompt) {
      wx.showToast({ title: '请输入活动需求', icon: 'none' })
      return
    }
    const isCover = mode === 'COVER'
    this.setData({
      aiGenerating: true,
      aiCoverGenerating: isCover || mode === 'FULL',
      aiLoadingText: isCover ? 'AI正在生成活动封面...' : 'AI正在生成活动内容与封面...'
    })
    request({
      url: '/api/admin/activities/ai-generate',
      method: 'POST',
      data: Object.assign({}, this.data.form, {
        prompt,
        mode,
        skills: compactSkills(this.data.skillOptions)
      })
    })
      .then(data => {
        this.applyAiGenerated(data || {})
        wx.showToast({ title: '已填充生成内容', icon: 'success' })
      })
      .catch(() => {})
      .finally(() => {
        this.setData({ aiGenerating: false, aiCoverGenerating: false })
      })
  },
  applyAiGenerated(data) {
    const form = Object.assign({}, this.data.form)
    const generatedFields = Object.assign({}, this.data.generatedFields)
    const mappings = [
      ['title', 'title'],
      ['category', 'category'],
      ['description', 'description'],
      ['requirements', 'requirements'],
      ['recruitCount', 'recruitCount'],
      ['serviceHours', 'serviceHours'],
      ['tips', 'tips']
    ]
    mappings.forEach(([source, target]) => {
      if (data[source] !== undefined && data[source] !== null && data[source] !== '') {
        form[target] = data[source]
        generatedFields[target] = true
      }
    })
    if (data.coverUrl) {
      form.coverImageUrl = data.coverUrl
      generatedFields.coverImageUrl = true
    }
    let skillOptions = this.data.skillOptions
    if (Array.isArray(data.skills) && data.skills.length) {
      skillOptions = uniqueSkills(skillOptions.slice(), data.skills)
      form.requiredSkills = compactSkills(skillOptions)
      generatedFields.requiredSkills = true
    }
    this.setData({ form, skillOptions, generatedFields, aiPanelVisible: false })
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
  pickActivityEndDate(e) {
    const activityEndDate = e.detail.value
    this.setData({ activityEndDate, 'form.endTime': combineDateTime(activityEndDate, this.data.activityEndClock) })
  },
  pickActivityEndClock(e) {
    const activityEndClock = e.detail.value
    this.setData({ activityEndClock, 'form.endTime': combineDateTime(this.data.activityEndDate, activityEndClock) })
  },
  pickSignupStartDate(e) {
    const signupStartDate = e.detail.value
    this.setData({ signupStartDate, 'form.signupStartTime': combineDateTime(signupStartDate, this.data.signupStartClock) })
  },
  pickSignupStartClock(e) {
    const signupStartClock = e.detail.value
    this.setData({ signupStartClock, 'form.signupStartTime': combineDateTime(this.data.signupStartDate, signupStartClock) })
  },
  pickDeadlineDate(e) {
    const deadlineDate = e.detail.value
    this.setData({ deadlineDate, 'form.signupDeadline': combineDateTime(deadlineDate, this.data.deadlineClock) })
  },
  pickDeadlineClock(e) {
    const deadlineClock = e.detail.value
    this.setData({ deadlineClock, 'form.signupDeadline': combineDateTime(this.data.deadlineDate, deadlineClock) })
  },
  pickCheckinStartDate(e) {
    const checkinStartDate = e.detail.value
    this.setData({ checkinStartDate, 'form.checkinStartTime': combineDateTime(checkinStartDate, this.data.checkinStartClock) })
  },
  pickCheckinStartClock(e) {
    const checkinStartClock = e.detail.value
    this.setData({ checkinStartClock, 'form.checkinStartTime': combineDateTime(this.data.checkinStartDate, checkinStartClock) })
  },
  pickCheckinEndDate(e) {
    const checkinEndDate = e.detail.value
    this.setData({ checkinEndDate, 'form.checkinEndTime': combineDateTime(checkinEndDate, this.data.checkinEndClock) })
  },
  pickCheckinEndClock(e) {
    const checkinEndClock = e.detail.value
    this.setData({ checkinEndClock, 'form.checkinEndTime': combineDateTime(this.data.checkinEndDate, checkinEndClock) })
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
    const invalidPosition = this.data.positions.find(item =>
      !item.name || !item.startTime || !item.endTime || !item.recruitNumber ||
      (item.requiresRehearsal && (!item.rehearsalStartTime || !item.rehearsalEndTime))
    )
    if (invalidPosition) {
      wx.showToast({ title: '请完整填写岗位及彩排时间', icon: 'none' })
      return
    }
    const form = Object.assign({}, this.data.form, {
      requiredSkills: compactSkills(this.data.skillOptions),
      positions: this.data.positions.filter(item => item.name && item.startTime && item.endTime && item.recruitNumber > 0),
      priorityRules: this.data.priorityRules
    })
    const invalidPriorityRule = form.priorityRules.find(item =>
      !item.ruleType || !String(item.ruleValue || '').trim() || !item.weight || item.weight < 1 || item.weight > 100
    )
    if (invalidPriorityRule) {
      wx.showToast({ title: '请完整填写优先条件和分值', icon: 'none' })
      return
    }
    if (!form.title || !form.activityTime || !form.location || !form.recruitCount || !form.serviceHours) {
      wx.showToast({ title: '请填写必填信息', icon: 'none' })
      return
    }
    if (form.endTime && form.activityTime && new Date(form.endTime.replace(/-/g, '/')).getTime() <= new Date(form.activityTime.replace(/-/g, '/')).getTime()) {
      wx.showToast({ title: '结束时间必须晚于活动时间', icon: 'none' })
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
