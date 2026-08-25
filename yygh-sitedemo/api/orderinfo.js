import request from '@/utils/request'
const api_name = `/api/order/orderInfo`
export default {
    //生成挂号订单
    submitOrder(scheduleId, patientId) {
        return request({
            url: `${api_name}/auth/submitOrder/${scheduleId}/${patientId}`,
            method: 'post'
        })
    },
    //订单详情
    getOrders(orderId) {
        return request({
            url: `${api_name}/auth/getOrders/${orderId}`,
            method: `get`
        })
    },
    //当前登录用户的挂号订单分页列表
    getPageList(page, limit, searchObj) {
        return request({
            url: `${api_name}/auth/${page}/${limit}`,
            method: 'get',
            params: searchObj || {}
        })
    },
}
