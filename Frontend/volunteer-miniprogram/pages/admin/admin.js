const { request } = require('../../utils/request')
Page({
  data: { stats: {} },
  onShow() { this.load() },
  load() { request({ url: '/api/admin/statistics' }).then(stats => this.setData({ stats })) },
  create() { wx.navigateTo({ url: '/pages/admin/activity-publish/activity-publish' }) },
  manageActivities() { wx.navigateTo({ url: '/pages/admin/activity-manage/activity-manage' }) },
  volunteers() { wx.navigateTo({ url: '/pages/volunteers/volunteers' }) },
  reviewList() { wx.navigateTo({ url: '/pages/admin/registration-review/registration-review' }) },
  goVolunteer(e) { wx.navigateTo({ url: `/pages/volunteer-detail/volunteer-detail?id=${e.currentTarget.dataset.id}` }) },
  review(e) {
    request({
      url: `/api/registrations/${e.currentTarget.dataset.id}/review`,
      method: 'PUT',
      data: { status: e.currentTarget.dataset.status, reviewRemark: '管理员审核' }
    }).then(() => { wx.showToast({ title: '已处理' }); this.load() })
  }
})
