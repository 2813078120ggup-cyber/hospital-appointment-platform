import axios from 'axios'
import { MessageBox, Message } from 'element-ui'
//引入js-cookie
import cookie from 'js-cookie'

// 创建axios实例
const service = axios.create({
    baseURL: process.env.NUXT_ENV_API_BASE_URL || 'http://localhost:8222',
    timeout: 15000 // 请求超时时间
})
// http request 拦截器
service.interceptors.request.use(
    config => {
    // token 先不处理，后续使用时在完善
    if (cookie.get('token')) {
        //从cookie获取token，放到请求头里面
        config.headers['token'] = cookie.get('token')
    }
    return config
},
  err => {
    return Promise.reject(err)
})
// http response 拦截器
service.interceptors.response.use(
    response => {
        if (response.data.code !== 20000) {
            Message({
                message: response.data.message,
                type: 'error',
                duration: 5 * 1000
            })
            return Promise.reject(response.data)
        } else {
            return response.data
        }
    },
    error => {
        // Network errors (for example, when the gateway is not running) do not
        // have a response. Rejecting undefined makes Nuxt's SSR renderer fail
        // later with a misleading renderResourceHints error.
        return Promise.reject(error.response || error)
})
export default service
