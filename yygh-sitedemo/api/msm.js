import request from '@/utils/request'
const api_name = `/api/msm`
export default {
    //发送短信
    sendCode(mobile) {
        return request({
            url: `${api_name}/send/${mobile}`,
            method: `get`
        })
    }
}