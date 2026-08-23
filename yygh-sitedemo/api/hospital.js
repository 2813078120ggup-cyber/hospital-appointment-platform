import request from '@/utils/request'
const api_name = `/api/hosp/hospital`
export default {
    //医院列表
    getPageList(page, limit, searchObj) {
        return request({
            url: `${api_name}/${page}/${limit}`,
            method: 'get',
            params: searchObj
        })
    },
    //根据名称模糊查询
    getByHosname(hosname) {
        return request({
            url: `${api_name}/findByHosname/${hosname}`,
            method: 'get'
        })
    },
    //根据医院编号获取医院详情
    show(hoscode) {
        return request({
            url: `${api_name}/${hoscode}`,
            method: 'get'
        })
    },
    //根据医院编号获取科室列表
    findDepartment(hoscode) {
        return request({
            url: `${api_name}/department/${hoscode}`,
            method: 'get'
        })
    },
    //显示科室预约日期数据
    getBookingScheduleRule(page, limit, hoscode, depcode) {
        return request({
            url: `${api_name}/auth/getBookingScheduleRule/${page}/${limit}/${hoscode}/${depcode}`,
            method: 'get'
        })
    },
    //显示日期医生详情数据
    findScheduleList(hoscode, depcode, workDate) {
        return request({
            url: `${api_name}/auth/findScheduleList/${hoscode}/${depcode}/${workDate}`,
            method: 'get'
        })
    },
    //根据排班id查询排班数据
    getSchedule(id) {
        return request({
            url: `${api_name}/getSchedule/${id}`,
            method: 'get'
            })
    }
}