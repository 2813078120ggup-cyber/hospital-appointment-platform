import request from '@/utils/request'

const apiName = '/admin/hosp/feedback'

export default {
  getPageList(page, limit, searchObj) {
    return request({
      url: `${apiName}/${page}/${limit}`,
      method: 'get',
      params: searchObj
    })
  },
  show(id) {
    return request({
      url: `${apiName}/show/${id}`,
      method: 'get'
    })
  },
  handle(id, data) {
    return request({
      url: `${apiName}/handle/${id}`,
      method: 'put',
      data
    })
  }
}
