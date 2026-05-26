App({
  globalData: {
    baseUrl: 'http://16.171.3.83:8080',
    token: wx.getStorageSync('token') || '',
    user: wx.getStorageSync('user') || null
  }
})
