const app = getApp()
const { request } = require('../../utils/request')

Page({
  data: {
    mode: 'login',
    form: { role: 'VOLUNTEER', college: '数计学院' }
  },
  input(e) {
    this.setData({ [`form.${e.currentTarget.dataset.k}`]: e.detail.value })
  },
  toggle() {
    this.setData({ mode: this.data.mode === 'login' ? 'register' : 'login' })
  },
  submit() {
    const url = this.data.mode === 'login' ? '/api/auth/login' : '/api/auth/register'
    request({ url, method: 'POST', data: this.data.form }).then(data => {
      app.globalData.token = data.token
      app.globalData.user = data.user
      wx.setStorageSync('token', data.token)
      wx.setStorageSync('user', data.user)
      wx.showToast({ title: '登录成功' })
      wx.reLaunch({ url: '/pages/home/home' })
    })
  }
})
