App({
  globalData: {
    baseUrl: 'https://springboot-4njf-262349-10-1306444430.sh.run.tcloudbase.com',
    token: wx.getStorageSync('token') || '',
    user: wx.getStorageSync('user') || null
  }
})
