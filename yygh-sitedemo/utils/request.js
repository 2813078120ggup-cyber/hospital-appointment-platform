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
// 功能完善：登录过期时提示并自动退出账号，清除本地登录态后回到首页。
const SESSION_EXPIRED_CODES = [50008, 50012, 50014]
let redirectingToHome = false
function handleSessionExpired() {
    if (redirectingToHome) return
    redirectingToHome = true
    Message({
        message: '登录过期',
        type: 'error',
        duration: 3 * 1000
    })
    // 仅客户端执行退出账号：清除 cookie 登录态并跳转，SSR 渲染阶段跳过。
    if (typeof window !== 'undefined') {
        cookie.set('name', '', { path: '/' })
        cookie.set('token', '', { path: '/' })
        window.location.href = '/'
    }
}

// http response 拦截器
service.interceptors.response.use(
    response => {
        if (response.data.code !== 20000) {
            if (SESSION_EXPIRED_CODES.indexOf(response.data.code) !== -1) {
                handleSessionExpired()
                return Promise.reject(response.data)
            }
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
        // 网关认证失败返回 401 + code 50008，同样按登录过期退出账号。
        const errData = error.response && error.response.data
        if (errData && SESSION_EXPIRED_CODES.indexOf(errData.code) !== -1) {
            handleSessionExpired()
            return Promise.reject(errData)
        }
        // Network errors (for example, when the gateway is not running) do not
        // have a response. Rejecting undefined makes Nuxt's SSR renderer fail
        // later with a misleading renderResourceHints error.
        return Promise.reject(error.response || error)
})
export default service
