<template>
  <div class="dashboard-container">
    <div class="welcome-row">
      <div>
        <h1>运营总览</h1>
        <p>{{ name }}，这里汇总医院、用户和预约订单的实时运营数据。</p>
      </div>
      <el-button :loading="loading" icon="el-icon-refresh" @click="loadDashboard">刷新数据</el-button>
    </div>

    <el-alert
      v-if="loadError"
      :title="loadError"
      type="error"
      show-icon
      :closable="false"
      class="dashboard-alert"
    />

    <el-row v-loading="loading" :gutter="18" class="metric-row">
      <el-col v-for="card in metricCards" :key="card.label" :xs="12" :sm="8" :lg="3">
        <el-card shadow="hover" :class="['metric-card', 'metric-card--' + card.tone]">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">{{ card.value }}</div>
          <div class="metric-note">{{ card.note }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="18">
      <el-col :xs="24" :lg="17">
        <el-card shadow="never">
          <div slot="header" class="card-header">
            <div>
              <strong>最近预约订单</strong>
              <span>用于快速跟进待支付和异常订单</span>
            </div>
            <router-link to="/order/list">查看全部</router-link>
          </div>
          <el-table :data="recentOrders" size="small">
            <el-table-column prop="outTradeNo" label="交易号" min-width="170" />
            <el-table-column prop="patientName" label="就诊人" width="100" />
            <el-table-column label="医院 / 科室" min-width="190">
              <template slot-scope="scope">
                {{ scope.row.hosname }} / {{ scope.row.depname }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="145">
              <template slot-scope="scope">
                <el-tag size="mini" :type="statusTagType(scope.row.orderStatus)">
                  {{ scope.row.param.orderStatusString }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70">
              <template slot-scope="scope">
                <router-link :to="'/order/show/' + scope.row.id">查看</router-link>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!loading && recentOrders.length === 0" class="empty-tip">暂无订单</div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="7">
        <el-card shadow="never" class="quick-card">
          <div slot="header"><strong>常用运营入口</strong></div>
          <router-link v-for="item in quickLinks" :key="item.path" :to="item.path" class="quick-link">
            <div>
              <i :class="item.icon" />
              <span>{{ item.label }}</span>
            </div>
            <i class="el-icon-arrow-right" />
          </router-link>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { mapGetters } from 'vuex'
import hospApi from '@/api/yygh/hosp'
import orderApi from '@/api/yygh/order'
import userInfoApi from '@/api/yygh/userinfo'

export default {
  name: 'Dashboard',
  data() {
    return {
      loading: false,
      loadError: '',
      summary: {
        hospitals: 0,
        users: 0,
        pendingAuth: 0,
        totalOrders: 0,
        todayOrders: 0,
        unpaidOrders: 0,
        cancelProcessingOrders: 0,
        cancelFailedOrders: 0
      },
      recentOrders: [],
      quickLinks: [
        { label: '医院上线管理', path: '/yygh/hospset/hospital/list', icon: 'el-icon-office-building' },
        { label: '预约订单查询', path: '/order/list', icon: 'el-icon-s-order' },
        { label: '取消异常处置', path: '/order/list?cancelStatus=3', icon: 'el-icon-warning-outline' },
        { label: '补偿任务处置', path: '/order/compensation', icon: 'el-icon-refresh' },
        { label: '公告发布管理', path: '/content/notice/list', icon: 'el-icon-document' },
        { label: '帮助中心管理', path: '/content/help/list', icon: 'el-icon-question' },
        { label: '用户认证审批', path: '/user/userInfo/authList', icon: 'el-icon-user' },
        { label: '数据字典维护', path: '/cmn/list', icon: 'el-icon-collection' },
        { label: '预约趋势统计', path: '/statistics/order/index', icon: 'el-icon-data-line' }
      ]
    }
  },
  computed: {
    ...mapGetters(['name']),
    metricCards() {
      return [
        { label: '接入医院', value: this.summary.hospitals, note: '平台医院总数', tone: 'standard' },
        { label: '平台注册用户', value: this.summary.users, note: '当前用户总数', tone: 'standard' },
        { label: '待认证用户', value: this.summary.pendingAuth, note: '需要管理员处理', tone: 'warning' },
        { label: '预约订单', value: this.summary.totalOrders, note: '历史订单总数', tone: 'standard' },
        { label: '今日新增', value: this.summary.todayOrders, note: '今日创建订单', tone: 'standard' },
        { label: '待支付订单', value: this.summary.unpaidOrders, note: '需要持续关注', tone: 'warning' },
        { label: '取消处理中', value: this.summary.cancelProcessingOrders, note: '正在协同医院与退款', tone: 'warning' },
        { label: '取消异常', value: this.summary.cancelFailedOrders, note: '需要人工跟进', tone: 'danger' }
      ]
    }
  },
  created() {
    this.loadDashboard()
  },
  methods: {
    loadDashboard() {
      this.loading = true
      this.loadError = ''
      Promise.all([
        hospApi.getPageList(1, 1, {}),
        userInfoApi.getPageList(1, 1, {}),
        userInfoApi.getPageList(1, 1, { authStatus: 1 }),
        orderApi.getSummary(),
        orderApi.getPageList(1, 5, {})
      ]).then(([hospitals, users, pendingAuth, orders, recent]) => {
        const orderSummary = orders.data.summary || {}
        this.summary = {
          hospitals: hospitals.data.pages.totalElements || 0,
          users: users.data.pageModel.total || 0,
          pendingAuth: pendingAuth.data.pageModel.total || 0,
          totalOrders: orderSummary.totalOrders || 0,
          todayOrders: orderSummary.todayOrders || 0,
          unpaidOrders: orderSummary.unpaidOrders || 0,
          cancelProcessingOrders: orderSummary.cancelProcessingOrders || 0,
          cancelFailedOrders: orderSummary.cancelFailedOrders || 0
        }
        this.recentOrders = recent.data.pageModel.records || []
      }).catch(() => {
        this.loadError = '运营数据加载失败，请确认医院、用户和订单服务均已启动。'
      }).finally(() => {
        this.loading = false
      })
    },
    statusTagType(status) {
      const types = {
        '-1': 'info',
        0: 'warning',
        1: 'success',
        2: 'primary'
      }
      return types[String(status)] || ''
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  min-height: calc(100vh - 50px);
  padding: 28px;
  background: #f4f6f9;
}

.welcome-row,
.card-header,
.quick-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.welcome-row {
  margin-bottom: 22px;

  h1 {
    margin: 0 0 8px;
    color: #1f2d3d;
    font-size: 28px;
  }

  p {
    margin: 0;
    color: #8492a6;
  }
}

.dashboard-alert,
.metric-row {
  margin-bottom: 20px;
}

.metric-card {
  margin-bottom: 18px;
  border-top: 3px solid #409eff;
}

.metric-card--warning {
  border-top-color: #e6a23c;
}

.metric-card--danger {
  border-top-color: #f56c6c;
}

.metric-label,
.metric-note,
.card-header span {
  color: #909399;
  font-size: 12px;
}

.metric-value {
  margin: 10px 0 6px;
  color: #303133;
  font-size: 30px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.card-header span {
  margin-left: 12px;
}

.empty-tip {
  padding: 30px;
  color: #909399;
  text-align: center;
}

.quick-card {
  margin-bottom: 18px;
}

.quick-link {
  padding: 15px 2px;
  color: #303133;
  border-bottom: 1px solid #ebeef5;

  &:last-child {
    border-bottom: 0;
  }

  &:hover {
    color: #409eff;
  }

  div i {
    width: 24px;
    color: #409eff;
  }
}
</style>
