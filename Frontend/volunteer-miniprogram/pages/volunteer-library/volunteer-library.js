const { request } = require('../../utils/request')

function splitTags(tags) {
  return clean(tags, '').split(',').map(item => item.trim()).filter(Boolean)
}

function clean(value, fallback) {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return fallback
  return text
}

function normalize(item) {
  const displayName = clean(item.nickname, '') || clean(item.name, '志愿者')
  return Object.assign({}, item, {
    displayName,
    avatarText: displayName.substring(0, 1),
    collegeText: clean(item.college, '未填写学院'),
    campusText: clean(item.campus, '未填写校区'),
    departmentText: clean(item.department, '未填写所属系'),
    majorClassText: clean(item.majorClass, '未填写专业班级'),
    volunteerLevelText: clean(item.volunteerLevel, '普通志愿者'),
    tags: splitTags(item.skillTags),
    recentActivity: clean(item.recentActivity, '暂无最近活动')
  })
}

Page({
  data: {
    list: [],
    keyword: '',
    college: '',
    campus: '',
    department: '',
    majorClass: '',
    skillTag: '',
    sortBy: 'points',
    sortOptions: ['积分优先', '服务时长优先'],
    campusOptions: ['全部', '东海岸校区', '桑浦山校区'],
    departmentOptions: ['全部', '数学系', '计算机系'],
    skillOptions: ['全部', '摄影', '摄像', '文案', '讲解', '物资搬运', '秩序维护', '活动组织']
  },
  onShow() {
    this.load()
  },
  input(e) {
    this.setData({ [e.currentTarget.dataset.key]: e.detail.value })
  },
  pickSort(e) {
    this.setData({ sortBy: Number(e.detail.value) === 1 ? 'hours' : 'points' })
    this.load()
  },
  pickSkill(e) {
    const value = this.data.skillOptions[e.detail.value]
    this.setData({ skillTag: value === '全部' ? '' : value })
    this.load()
  },
  pickCampus(e) {
    const value = this.data.campusOptions[Number(e.detail.value)]
    this.setData({ campus: value === '全部' ? '' : value })
    this.load()
  },
  pickDepartment(e) {
    const value = this.data.departmentOptions[Number(e.detail.value)]
    this.setData({ department: value === '全部' ? '' : value })
    this.load()
  },
  load() {
    wx.showLoading({ title: '加载中' })
    request({
      url: '/api/volunteers',
      data: {
        college: this.data.college,
        campus: this.data.campus,
        department: this.data.department,
        majorClass: this.data.majorClass,
        skillTag: this.data.skillTag,
        keyword: this.data.keyword,
        sortBy: this.data.sortBy
      }
    }).then(list => {
      this.setData({ list: (list || []).map(normalize) })
    }).catch(() => {}).finally(() => wx.hideLoading())
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` })
  },
  goMessages() {
    wx.navigateTo({ url: '/pages/message-center/message-center' })
  }
})
