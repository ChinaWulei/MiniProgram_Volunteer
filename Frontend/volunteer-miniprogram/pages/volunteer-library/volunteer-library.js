const { request } = require('../../utils/request')

function splitTags(tags) {
  return (tags || '').split(',').map(item => item.trim()).filter(Boolean)
}

function normalize(item) {
  return Object.assign({}, item, {
    displayName: item.nickname || item.name || '志愿者',
    avatarText: (item.nickname || item.name || '志').substring(0, 1),
    tags: splitTags(item.skillTags),
    recentActivity: item.recentActivity || '暂无最近活动'
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
