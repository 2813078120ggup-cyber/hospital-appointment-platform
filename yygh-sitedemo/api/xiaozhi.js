import axios from 'axios'
import cookie from 'js-cookie'

const service = axios.create({
  baseURL: process.env.XIAOZHI_API_BASE_URL || 'http://localhost:8080',
  timeout: 90000
})

service.interceptors.request.use(config => {
  const token = cookie.get('token')
  if (token) {
    config.headers.token = token
  }
  return config
})

function errorMessage (error) {
  const data = error && error.response && error.response.data
  if (data && data.message) {
    return data.message
  }
  if (error && error.code === 'ECONNABORTED') {
    return '回答等待时间较长，请稍后重新发送'
  }
  return '暂时无法连接硅谷小智，请确认 Agent 服务已启动'
}

export default {
  status () {
    return service.get('/xiaozhi/status').then(response => response.data)
  },

  chat (memoryId, message) {
    return service.post('/xiaozhi/chat', {
      memoryId,
      message
    }).then(response => response.data).catch(error => Promise.reject(new Error(errorMessage(error))))
  }
}
