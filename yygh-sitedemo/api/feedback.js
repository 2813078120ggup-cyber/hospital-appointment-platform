import request from '@/utils/request'
const api_name = `/api/hosp/feedback`
export default {
    //提交意见反馈
    save(feedback) {
        return request({
            url: `${api_name}/auth/save`,
            method: 'post',
            data: feedback
        })
    }
}
