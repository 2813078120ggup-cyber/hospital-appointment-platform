import request from '@/utils/request'
export default {
    //数据字典列表
    dictList(id) {
      return request ({
        url: `/admin/cmn/dict/findDataById/${id}`,
        method: 'get'
      })
    },
    exportData() {
      return request({
        url: '/admin/cmn/dict/exportData',
        method: 'get',
        responseType: 'blob'
      })
    }
}
