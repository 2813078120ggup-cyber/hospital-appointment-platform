import request from '@/utils/request'

const api_name = '/admin/hosp/hospitalSet'

export default {
    //带条件带分页查询
    pageInfo(page, limit, searchObj) {
        return request({
            url: `${api_name}/findPageQueryHospSet/${page}/${limit}`,
            method: 'post',
            data: searchObj //用json方式传递
        })
    },
    //删除
    delHospSet(id) {
        return request({
            url: `${api_name}/remove/${id}`,
            method: 'delete'
        })
    },
    //添加医院设置
    addHospSet(hospitalSet) {
        return request({
            url: `${api_name}/addHospSet`,
            method: 'post',
            data: hospitalSet //用json方式传递
        })
    },
    //根据id查询医院设置
    getHospSetById(id) {
        return request({
            url: `${api_name}/getHospSetById/${id}`,
            method: 'get'
        })
    },
    //修改医院设置
    updateHospSet(hospitalSet) {
        return request({
            url: `${api_name}/updateHospSet`,
            method: 'put',
            data: hospitalSet //用json方式传递
        })
    },
    //批量删除
    removeRows(idList) {
        return request({
            url: `${api_name}/batchRemove`,
            method: 'delete',
            data: idList
        })
    },
    //锁定和取消锁定
    lockHospSet(id, status) {
        return request({
            url: `${api_name}/lockHospitalSet/${id}/${status}`,
            method: 'put'
        })
    }
}
