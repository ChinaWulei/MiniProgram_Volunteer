const { request } = require('../../utils/request')
Page({
  data: { activityId: null, list: [] },
  onLoad(options) {
    this.setData({ activityId: options.activityId })
    request({ url: `/api/match/activity/${options.activityId}` }).then(list => this.setData({ list }))
  }
})
