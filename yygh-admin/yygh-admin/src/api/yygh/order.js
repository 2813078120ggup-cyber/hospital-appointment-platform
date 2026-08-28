import request from '@/utils/request'

const apiName = '/admin/order/orderInfo'
const compensationApiName = '/admin/order/compensation'

export default {
  getPageList(page, limit, searchObj) {
    return request({
      url: `${apiName}/${page}/${limit}`,
      method: 'get',
      params: searchObj
    })
  },
  show(orderId) {
    return request({
      url: `${apiName}/show/${orderId}`,
      method: 'get'
    })
  },
  getSummary() {
    return request({
      url: `${apiName}/summary`,
      method: 'get'
    })
  },
  getStatusList() {
    return request({
      url: `${apiName}/statusList`,
      method: 'get'
    })
  },
  cancelOrder(orderId, reason) {
    return request({
      url: `${apiName}/cancel/${orderId}`,
      method: 'post',
      data: { reason }
    })
  },
  getCompensationPage(page, limit, params) {
    return request({
      url: `${compensationApiName}/${page}/${limit}`,
      method: 'get',
      params
    })
  },
  getCompensationStatusList() {
    return request({
      url: `${compensationApiName}/statusList`,
      method: 'get'
    })
  },
  retryCompensation(taskId) {
    return request({
      url: `${compensationApiName}/retry/${taskId}`,
      method: 'post'
    })
  }
}
