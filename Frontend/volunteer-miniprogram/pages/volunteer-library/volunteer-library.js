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
    majorClass: '',
    skillTag: '',
    sortBy: 'points',
    sortOptions: ['积分优先', '服务时长优先'],
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
  load() {
    wx.showLoading({ title: '加载中' })
    request({
      url: '/api/volunteers',
      data: {
        college: this.data.college,
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
