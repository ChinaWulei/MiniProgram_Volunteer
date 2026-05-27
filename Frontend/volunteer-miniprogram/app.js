App({
  globalData: {
    baseUrl: 'https://api.stu-volunteer.online/',
    token: wx.getStorageSync('token') || '',
    user: wx.getStorageSync('user') || null
  }
})
