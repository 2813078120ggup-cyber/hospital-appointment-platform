import request from '@/utils/request'
const api_name = '/admin/cmn/dict'
export default {
    //查询所有医院等级，所有区县，所有....
    findByDictCode(dictCode) {
        return request({
            url: `${api_name}/findByDictCode/${dictCode}`,
            method: 'get'
        })
    },
    //查询下层数据
    findByParentId(parentId) {
        return request({
            url: `${api_name}/findChildData/${parentId}`,
            method: 'get'
        })
    }
}