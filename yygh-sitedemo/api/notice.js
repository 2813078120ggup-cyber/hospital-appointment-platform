import request from '@/utils/request'

const apiName = '/api/cmn/notice'

export default {
  getPublishedList(noticeType, limit = 20) {
    return request({
      url: `${apiName}/list`,
      method: 'get',
      params: {
        noticeType,
        limit
      }
    })
  }
}
