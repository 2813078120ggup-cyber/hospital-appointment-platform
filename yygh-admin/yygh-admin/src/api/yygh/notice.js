import request from '@/utils/request'

const apiName = '/admin/cmn/notice'

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
  save(notice) {
    return request({
      url: apiName,
      method: 'post',
      data: notice
    })
  },
  update(notice) {
    return request({
      url: apiName,
      method: 'put',
      data: notice
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
