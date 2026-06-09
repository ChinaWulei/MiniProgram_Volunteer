const { request } = require('../../utils/request')

function clean(value, fallback) {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return fallback
  return text
}

function normalize(item) {
  return Object.assign({}, item, {
    nameText: clean(item.name || item.nickname, '志愿者'),
    campusText: clean(item.campus, '未填写校区'),
    departmentText: clean(item.department, '未填写所属系'),
    majorClassText: clean(item.majorClass, '未填写专业班级'),
    phoneText: clean(item.phone, '-'),
    skillTagsText: clean(item.skillTags, '暂无技能标签'),
    availableTimeText: clean(item.availableTime, '-')
  })
}

Page({
  data: {
    list: [], keyword: '', major: '', skill: '', sortBy: '',
    campus: '', department: '',
    campusOptions: ['全部', '东海岸校区', '桑浦山校区'],
    departmentOptions: ['全部', '数学系', '计算机系']
  },
  onLoad(options) {
    if (options && options.sort) {
      this.setData({ sortBy: options.sort })
    }
  },
  onShow() { this.load() },
  input(e) { this.setData({ [e.currentTarget.dataset.k]: e.detail.value }) },
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
    request({
      url: '/api/volunteers',
      data: {
        keyword: this.data.keyword,
        campus: this.data.campus,
        department: this.data.department,
        majorClass: this.data.major,
        skillTag: this.data.skill,
        sortBy: this.data.sortBy
      }
    })
      .then(list => this.setData({ list: (list || []).map(normalize) }))
  },
  goVolunteer(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` })
  }
})
