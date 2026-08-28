<template>
<div v-loading="loading" class="app-container">
    <h4>基本信息</h4>
    <table class="table table-striped table-condenseda table-bordered" width="100%">
        <tbody>
            <tr>
                <th width="15%">医院名称</th>
                <td width="35%"><b style="font-size: 14px">{{ hospital.hosname }}</b> | {{ hospitalParam.hostypeString }}</td>
                <th width="15%">医院logo</th>
                <td width="35%">
                    <img :src="'data:image/jpeg;base64,'+hospital.logoData" width="80">
                </td>
            </tr>
            <tr>
                <th>医院编码</th>
                <td>{{ hospital.hoscode }}</td>
                <th>地址</th>
                <td>{{ hospitalParam.fullAddress }}</td>
            </tr>
            <tr>
                <th>坐车路线</th>
                <td colspan="3">{{ hospital.route }}</td>
            </tr>
            <tr>
                <th>医院简介</th>
                <td colspan="3">{{ hospital.intro }}</td>
            </tr>
        </tbody>
        </table>
        <h4>预约规则信息</h4>
        <table class="table table-striped table-condenseda table-bordered" width="100%">
        <tbody>
            <tr>
                <th width="15%">预约周期</th>
                <td width="35%">{{ bookingRule.cycle }}天</td>
                <th width="15%">放号时间</th>
                <td width="35%">{{ bookingRule.releaseTime }}</td>
            </tr>
            <tr>
                <th>停挂时间</th>
                <td>{{ bookingRule.stopTime }}</td>
                <th>退号时间</th>
                <td>{{ bookingRule.quitDay == -1 ? '就诊前一工作日' : '就诊当日' }}{{ bookingRule.quitTime }} 前取消</td>
            </tr>
            <tr>
                <th>预约规则</th>
                <td colspan="3">
                <ol>
                <li v-for="item in bookingRules" :key="item">{{ item }}</li>
                </ol>
                </td>
            </tr>
        <br>
            <el-row>
            <el-button @click="back">返回</el-button>
            </el-row>
        </tbody>
    </table>
</div>
</template>
<script>
import hospApi from '@/api/yygh/hosp'
export default {
    data() {
        return {
            loading: true,
            hospital: {},  //医院信息
            bookingRule: {} //预约信息
        }
    },
    computed: {
        hospitalParam() {
            return this.hospital.param || {}
        },
        bookingRules() {
            return this.bookingRule.rule || []
        }
    },
    created() {
        //获取路由id
        const id = this.$route.params.id
        //调用方法，根据id查询医院详情
        this.fetachHospDetail(id)
    },
    methods:{
        //根据id查询医院详情
        fetachHospDetail(id) {
            hospApi.getHospById(id)
                .then(response => {
                    this.hospital = response.data.hospital
                    this.bookingRule = response.data.bookingRule
                }).finally(() => {
                    this.loading = false
                })
        },
        //返回医院列表
        back() {
            this.$router.push({ path: '/yygh/hospset/hospital/list' })
        }
    }
}
</script>
