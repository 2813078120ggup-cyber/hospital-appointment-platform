import request from '@/utils/request'
const api_name = `/api/user/wx`
export default {
    //返回微信登录二维码参数
    getLoginParam() {
        return request({
        url: `${api_name}/getLoginParam`,
        method: `get`
        })
    },
    //微信支付二维码
    createNative(orderId) {
        return request({
            url: `/api/order/weixin/createNative/${orderId}`,
            method: 'get'
        })
    },
    //查询订单支付状态
    queryPayStatus(orderId) {
        return request({
            url: `/api/order/weixin/queryPayStatus/${orderId}`,
            method: 'get'
        })
    },
    //取消预约
    cancelOrder(orderId) {
        return request({
            url: `/api/order/orderInfo/auth/cancelOrder/${orderId}`,
            method: 'get'
        })
    },
}