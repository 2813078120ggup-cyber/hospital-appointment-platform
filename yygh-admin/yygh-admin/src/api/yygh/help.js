import request from '@/utils/request'

const apiName = '/admin/cmn/help'

export default {
  getPageList(page, limit, searchObj) {
    return request({
      url: `${apiName}/${page}/${limit}`,
      method: 'get',
      params: searchObj
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
      url: `${apiName}/show/${id}`,
      method: 'get'
    })
  },
  save(article) {
    return request({
      url: apiName,
      method: 'post',
      data: article
    })
  },
  update(article) {
    return request({
      url: apiName,
      method: 'put',
      data: article
    })
  },
  updateStatus(id, status) {
    return request({
      url: `${apiName}/updateStatus/${id}/${status}`,
      method: 'put'
    })
  },
  remove(id) {
    return request({
      url: `${apiName}/${id}`,
      method: 'delete'
    })
  }
}
