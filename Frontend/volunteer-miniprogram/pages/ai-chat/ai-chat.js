const { request } = require('../../utils/request')

const quickQuestions = {
  weekend: '周末有什么适合我的活动？',
  photo: '我适合摄影类志愿吗？',
  points: '如何提升志愿积分？'
}

Page({
  data: {
    inputValue: '',
    sending: false,
    sessionId: '',
    activityId: null,
    activityName: '',
    scrollIntoView: '',
    messages: [
      {
        id: 'welcome',
        role: 'ai',
        content: '你好，我是 AI 志愿助手。你可以问我周末有什么活动、哪些活动适合你、如何提升志愿积分。'
      }
    ]
  },
  onLoad(options) {
    if (options && options.activityId) {
      const activityName = decodeURIComponent(options.activityName || '当前活动')
      this.setData({
        activityId: options.activityId,
        activityName,
        inputValue: `这个活动适合我参加吗？`
      })
    }
  },
  input(e) {
    this.setData({ inputValue: e.detail.value })
  },
  send() {
    const message = (this.data.inputValue || '').trim()
    if (!message || this.data.sending) return
    const userMessage = { id: `u-${Date.now()}`, role: 'user', content: message }
    const loadingMessage = { id: `loading-${Date.now()}`, role: 'ai', content: '正在分析平台活动...', loading: true }
    this.setData({
      inputValue: '',
      sending: true,
      messages: this.data.messages.concat(userMessage, loadingMessage),
      scrollIntoView: loadingMessage.id
    })
    request({
      url: '/api/ai/chat',
      method: 'POST',
      data: {
        message,
        sessionId: this.data.sessionId,
        activityId: this.data.activityId
      }
    }).then(data => {
      const messages = this.data.messages.slice(0, -1)
      const aiMessage = {
        id: `a-${Date.now()}`,
        role: 'ai',
        content: data.answer || data.reply || '暂时无法确认，请稍后再试。',
        intent: data.intent,
        sources: data.sources || [],
        sourceText: (data.sources || []).join('；'),
        activities: data.recommendations || data.recommendedActivities || []
      }
      this.setData({
        sessionId: data.sessionId || this.data.sessionId,
        messages: messages.concat(aiMessage),
        scrollIntoView: aiMessage.id
      })
    }).catch(() => {
      const messages = this.data.messages.slice(0, -1)
      const failMessage = { id: `f-${Date.now()}`, role: 'ai', content: '发送失败，请稍后再试。' }
      this.setData({ messages: messages.concat(failMessage), scrollIntoView: failMessage.id })
      wx.showToast({ title: '发送失败', icon: 'none' })
    }).finally(() => {
      this.setData({ sending: false })
    })
  },
  quickAsk(e) {
    this.setData({ inputValue: quickQuestions[e.currentTarget.dataset.key] || '' })
    this.send()
  },
  goDetail(e) {
    wx.navigateTo({ url: `/pages/activity-detail/activity-detail?activityId=${e.currentTarget.dataset.id}` })
  }
})
