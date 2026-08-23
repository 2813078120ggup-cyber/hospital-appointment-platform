import request from '@/utils/request'
const api_name = `/api/user`
export default {
    //登录接口
    login(userInfo) {
        return request({
            url: `${api_name}/login`,
            method: `post`,
            data: userInfo
        })
    },
    //获取用户信息
    getUserInfo() {
        return request({
            url: `${api_name}/auth/getUserInfo`,
            method: `get`
        })
    },
    //用户认证
    saveUserAuah(userAuah) {
        return request({
             url: `${api_name}/auth/userAuth`,
             method: 'post',
             data: userAuah
        })
    }
}