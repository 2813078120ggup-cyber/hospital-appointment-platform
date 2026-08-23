import request from '@/utils/request'
export default {
    //显示科室日期数据
    getScheduleRule(page, limit, hoscode, depcode) {
        return request({
            url: `/admin/hosp/schedule/getScheduleRule/${page}/${limit}/${hoscode}/${depcode}`,
            method: 'get'
        })
    },
    //查询排班详情
    getScheduleDetail(hoscode,depcode,workDate) {
        return request ({
            url: `/admin/hosp/schedule/getScheduleDetail/${hoscode}/${depcode}/${workDate}`,
            method: 'get'
        })
    }
}