<template>
    <div class="app-container">
    <!--表单-->
    <el-form :inline="true" class="demo-form-inline">
        <el-form-item>
            <el-input v-model="searchObj.hosname" placeholder="点击输入医院名称"/>
        </el-form-item>
        <el-form-item>
            <el-date-picker
                v-model="searchObj.reserveDateBegin"
                type="date"
                placeholder="选择开始日期"
                value-format="yyyy-MM-dd"/>
        </el-form-item>
        <el-form-item>
            <el-date-picker
                v-model="searchObj.reserveDateEnd"
                type="date"
                placeholder="选择截止日期"
                value-format="yyyy-MM-dd"/>
        </el-form-item>
        <el-button
            :disabled="btnDisabled"
            type="primary"
            icon="el-icon-search"
            @click="showChart()">查询</el-button>
        <el-button @click="resetSearch">未来 7 天</el-button>
    </el-form>
    <div v-loading="loading" class="chart-container">
        <div id="chart" ref="chart" 
            class="chart" style="height:500px;width:100%"/>
        <div v-if="!loading && xData.length === 0" class="empty-tip">当前条件下暂无预约数据</div>
    </div>
    </div>
</template>
<script>
import echarts from 'echarts'
import statisticsApi from '@/api/yygh/sta'
export default {
    data() {
        return {
            searchObj: {
                hosname: '',
                reserveDateBegin: '',
                reserveDateEnd: ''
            },
            btnDisabled: false,
            loading: false,
            chart: null,
            title: '平台',
            xData: [], // x轴数据
            yData: [] // y轴数据
        }
    },
    created() {
        this.setNextSevenDays()
    },
    mounted() {
        this.showChart()
    },
    beforeDestroy() {
        if (this.chart) {
            this.chart.dispose()
        }
    },
    methods: {
        // 初始化图表数据
        showChart() {
            this.loading = true
            this.btnDisabled = true
            statisticsApi.getCountMap(this.searchObj).then(response => {
                this.yData = response.data.countList
                this.xData = response.data.dateList
                this.setChartData()
            }).finally(() => {
                this.loading = false
                this.btnDisabled = false
            })
        },
        setChartData() {
            // 基于准备好的dom，初始化echarts实例
            if (!this.chart) {
                this.chart = echarts.init(this.$refs.chart)
            }
            // 指定图表的配置项和数据
            var option = {
                title: {
                    text: this.title + '挂号量统计'
                },
                tooltip: {},
                legend: {
                    data: [this.title]
                },
                xAxis: {
                    data: this.xData
                },
                yAxis: {
                    minInterval: 1
                },
                series: [{
                    name: this.title,
                    type: 'line',
                    data: this.yData
                }]
            }
            // 使用刚指定的配置项和数据显示图表。
            this.chart.setOption(option, true)
        },
        resetSearch() {
            this.searchObj.hosname = ''
            this.setNextSevenDays()
            this.showChart()
        },
        setNextSevenDays() {
            const begin = new Date()
            const end = new Date()
            end.setDate(begin.getDate() + 6)
            this.searchObj.reserveDateBegin = this.formatDate(begin)
            this.searchObj.reserveDateEnd = this.formatDate(end)
        },
        formatDate(date) {
            const month = String(date.getMonth() + 1).padStart(2, '0')
            const day = String(date.getDate()).padStart(2, '0')
            return `${date.getFullYear()}-${month}-${day}`
        }
    }
}
</script>
<style scoped>
.chart-container {
    position: relative;
    min-height: 500px;
}

.empty-tip {
    position: absolute;
    top: 50%;
    width: 100%;
    color: #909399;
    text-align: center;
}
</style>
