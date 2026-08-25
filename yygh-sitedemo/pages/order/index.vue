<template>
  <div class="nav-container page-component">
    <!-- 左侧导航 -->
    <div class="nav left-nav">
      <div class="nav-item">
        <span class="v-link clickable dark" onclick="javascript:window.location='/user'">实名认证</span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark" onclick="javascript:window.location='/order'">挂号订单</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" onclick="javascript:window.location='/patient'">就诊人管理</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark">修改账号信息</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark">意见反馈</span>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="page-container">
      <div class="personal-order order-list">
        <div class="title">挂号订单</div>

        <div v-if="!isLoggedIn" class="order-state">
          <i class="el-icon-user-solid order-state-icon"></i>
          <div class="order-state-title">登录后查看挂号订单</div>
          <div class="order-state-description">登录后即可查看预约记录和订单详情</div>
          <div class="v-button order-state-button" @click="openLogin">立即登录</div>
        </div>

        <div v-else-if="loading" class="order-state">
          <i class="el-icon-loading order-state-icon is-loading"></i>
          <div class="order-state-title">正在加载订单</div>
          <div class="order-state-description">请稍候，正在获取你的预约记录</div>
        </div>

        <div v-else-if="errorMessage" class="order-state order-state-error">
          <i class="el-icon-warning-outline order-state-icon"></i>
          <div class="order-state-title">订单加载失败</div>
          <div class="order-state-description">{{ errorMessage }}</div>
          <div class="v-button order-state-button" @click="loadOrders">重新加载</div>
        </div>

        <div v-else-if="orderList.length === 0" class="order-state">
          <i class="el-icon-document order-state-icon"></i>
          <div class="order-state-title">暂无挂号订单</div>
          <div class="order-state-description">完成预约后，订单会显示在这里</div>
          <div class="v-button order-state-button" @click="goHome">去预约</div>
        </div>

        <div v-else class="order-table-wrapper">
          <el-table :data="orderList" stripe class="order-table">
            <el-table-column label="就诊日期" width="130">
              <template slot-scope="scope">
                {{ formatDate(scope.row.reserveDate) }} {{ formatReserveTime(scope.row.reserveTime) }}
              </template>
            </el-table-column>
            <el-table-column prop="hosname" label="就诊医院" min-width="180" show-overflow-tooltip></el-table-column>
            <el-table-column prop="depname" label="就诊科室" min-width="150" show-overflow-tooltip></el-table-column>
            <el-table-column prop="patientName" label="就诊人" width="100"></el-table-column>
            <el-table-column label="订单状态" width="145">
              <template slot-scope="scope">
                <span :class="['order-status', statusClass(scope.row.orderStatus)]">
                  {{ statusText(scope.row) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="show(scope.row.id)">查看详情</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-if="total > limit" class="pagination-wrapper">
            <el-pagination
              :current-page="page"
              :page-size="limit"
              :total="total"
              layout="prev, pager, next"
              @current-change="handlePageChange">
            </el-pagination>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import cookie from 'js-cookie'
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'
import '~/assets/css/personal.css'
import orderInfoApi from '@/api/orderinfo'

const ORDER_STATUS_TEXT = {
  '-1': '取消预约',
  0: '预约成功，待支付',
  1: '已支付',
  2: '已取号'
}

export default {
  data() {
    return {
      isLoggedIn: false,
      loading: false,
      errorMessage: '',
      orderList: [],
      page: 1,
      limit: 10,
      total: 0
    }
  },
  mounted() {
    this.isLoggedIn = !!cookie.get('token')
    if (this.isLoggedIn) {
      this.loadOrders()
    }
  },
  methods: {
    loadOrders() {
      if (!this.isLoggedIn) return
      this.loading = true
      this.errorMessage = ''
      orderInfoApi.getPageList(this.page, this.limit).then(response => {
        const pageData = response && response.data && (response.data.pageModel || response.data.page)
        const data = response && response.data
        this.orderList = (pageData && (pageData.records || pageData.list)) || (data && data.list) || []
        this.total = Number((pageData && pageData.total) || (data && data.total) || this.orderList.length)
      }).catch(error => {
        this.orderList = []
        this.total = 0
        this.errorMessage = (error && error.message) || '暂时无法获取订单，请稍后重试'
      }).finally(() => {
        this.loading = false
      })
    },
    handlePageChange(page) {
      this.page = page
      this.loadOrders()
    },
    show(orderId) {
      if (!orderId) return
      window.location.href = '/order/show?orderId=' + orderId
    },
    openLogin() {
      if (typeof window !== 'undefined' && window.loginEvent) {
        window.loginEvent.$emit('loginDialogEvent')
      } else {
        window.location.href = '/'
      }
    },
    goHome() {
      window.location.href = '/'
    },
    formatDate(value) {
      if (!value) return '-'
      return String(value).slice(0, 10)
    },
    formatReserveTime(value) {
      if (value === 0 || value === '0') return '上午'
      if (value === 1 || value === '1') return '下午'
      return ''
    },
    statusText(order) {
      if (order && order.param && order.param.orderStatusString) {
        return order.param.orderStatusString
      }
      return ORDER_STATUS_TEXT[order && order.orderStatus] || '状态未知'
    },
    statusClass(status) {
      if (status === -1 || status === '-1') return 'is-cancelled'
      if (status === 0 || status === '0') return 'is-pending'
      if (status === 1 || status === '1') return 'is-paid'
      if (status === 2 || status === '2') return 'is-completed'
      return 'is-unknown'
    }
  }
}
</script>

<style>
.order-list {
  min-height: 500px;
}

.order-list .title {
  margin-bottom: 40px;
}

.order-table-wrapper {
  width: 100%;
}

.order-table {
  width: 100%;
  font-size: 13px;
}

.order-table .el-table__header th {
  background-color: #e8f2ff;
  color: #666;
  font-weight: 400;
}

.order-table .el-table__body td {
  color: #333;
}

.order-status {
  display: inline-block;
  line-height: 22px;
  white-space: nowrap;
}

.order-status.is-pending,
.order-status.is-paid {
  color: #4490f1;
}

.order-status.is-completed {
  color: #00a870;
}

.order-status.is-cancelled,
.order-status.is-unknown {
  color: #999;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.order-state {
  min-height: 340px;
  padding-top: 70px;
  text-align: center;
  color: #999;
}

.order-state-icon {
  display: block;
  color: #b8d5f7;
  font-size: 48px;
  margin-bottom: 20px;
}

.order-state-icon.is-loading {
  color: #4990f1;
}

.order-state-title {
  color: #333;
  font-size: 16px;
  line-height: 24px;
}

.order-state-description {
  margin-top: 8px;
  color: #999;
  font-size: 13px;
}

.order-state-button {
  width: 120px;
  height: 36px;
  margin: 24px auto 0;
  font-size: 13px;
}

.order-state-error .order-state-icon {
  color: #f2b35b;
}
</style>
