const { request } = require('../../utils/request')

function clean(value, fallback = '') {
  if (value === null || value === undefined) return fallback
  const text = String(value).trim()
  if (!text || text === 'null' || text === 'undefined') return fallback
  return text
}

function normalizeMatch(item) {
  const volunteer = item.volunteer || {}
  const name = clean(volunteer.name || volunteer.nickname, '志愿者')
  const majorClass = clean(volunteer.majorClass)
  const skillTags = clean(volunteer.skillTags, '暂无技能标签')
  return Object.assign({}, item, {
    volunteer: Object.assign({}, volunteer, {
      name,
      majorClassText: majorClass,
      skillTagsText: skillTags,
      metaText: [majorClass, skillTags].filter(Boolean).join(' · ')
    }),
    reason: clean(item.reason, '暂无匹配说明')
  })
}

Page({
  data: { activityId: null, list: [], loading: false },
  onLoad(options) {
    this.setData({ activityId: options.activityId })
    this.load()
  },
  load() {
    this.setData({ loading: true })
    request({ url: `/api/match/activity/${this.data.activityId}` })
      .then(list => this.setData({ list: (list || []).map(normalizeMatch) }))
      .catch(() => this.setData({ list: [] }))
      .finally(() => this.setData({ loading: false }))
  },
  showRule() {
    wx.showModal({
      title: '推荐说明',
      content: '推荐 Top 5 不设置最低分阈值，会从志愿者库中按综合评分排序取前 5 名。综合评分=技能重合度60%+时间匹配20%+信用评分20%。如果这里为空，通常是志愿者库暂无数据，或当前账号没有管理员权限。',
      showCancel: false
    })
  },
  goVolunteer(e) {
    wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` })
  }
})
