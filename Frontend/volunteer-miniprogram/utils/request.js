const app = getApp()

function apiUrl(path) {
  const baseUrl = (app.globalData.baseUrl || '').replace(/\/+$/, '')
  const apiPath = (path || '').startsWith('/') ? path : `/${path || ''}`
  return baseUrl + apiPath
}

function failMessage(err, fallback) {
  const message = err && err.errMsg ? err.errMsg : fallback
  if (message.includes('url not in domain list')) return '请求域名未配置'
  if (message.includes('request:fail')) return message.replace('request:fail ', '').slice(0, 28) || fallback
  return fallback
}

function request(options) {
  const token = app.globalData.token || wx.getStorageSync('token')
  const url = apiUrl(options.url)
  return new Promise((resolve, reject) => {
    wx.request({
      url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'content-type': 'application/json',
        'Authorization': token || ''
      },
      success(res) {
        const body = res.data || {}
        if (body.code === 200) {
          resolve(body.data)
        } else {
          if (!options.silent) {
            wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          }
          reject(body)
        }
      },
      fail(err) {
        console.error('wx.request failed', { url, err })
        if (!options.silent) {
          wx.showToast({ title: failMessage(err, '后端服务未连接'), icon: 'none' })
        }
        reject(err)
      }
    })
  })
}

function uploadFile(options) {
  const token = app.globalData.token || wx.getStorageSync('token')
  const url = apiUrl(options.url)
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url,
      filePath: options.filePath,
      name: options.name || 'file',
      header: { 'Authorization': token || '' },
      success(res) {
        let body = {}
        try {
          body = JSON.parse(res.data || '{}')
        } catch (e) {
          wx.showToast({ title: '上传结果解析失败', icon: 'none' })
          reject(e)
          return
        }
        if (body.code === 200) {
          resolve(body.data)
        } else {
          wx.showToast({ title: body.message || '上传失败', icon: 'none' })
          reject(body)
        }
      },
      fail(err) {
        console.error('wx.uploadFile failed', { url, err })
        wx.showToast({ title: failMessage(err, '上传失败'), icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = { request, uploadFile }
