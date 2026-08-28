import request from '@/utils/request'

const apiName = '/api/cmn/help'

export default {
  getPublishedList(keyword = '', categoryCode = '', limit = 100) {
    return request({
      url: `${apiName}/list`,
      method: 'get',
      params: {
        keyword,
        categoryCode,
        limit
      }
    })
  },
  getCategories() {
    return request({
      url: `${apiName}/categories`,
      method: 'get'
    })
  },
  show(id) {
    return request({
      url: `${apiName}/${id}`,
      method: 'get'
    })
  }
}
