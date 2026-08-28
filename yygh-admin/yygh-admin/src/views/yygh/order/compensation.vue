<template>
  <div class="app-container">
    <div class="page-heading">
      <div>
        <h2>订单补偿任务</h2>
        <p>查看取消、库存和医院支付状态同步任务；死信任务可人工重新投递。</p>
      </div>
      <el-button icon="el-icon-refresh" @click="fetchData(page)">刷新</el-button>
    </div>

    <el-form :inline="true" :model="searchObj">
      <el-form-item>
        <el-select v-model="searchObj.status" clearable placeholder="任务状态" style="width: 150px;">
          <el-option
            v-for="item in statusList"
            :key="item.status"
            :label="item.label"
            :value="item.status"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.taskType" clearable placeholder="任务类型" style="width: 230px;">
          <el-option label="取消流程（含退款）" value="CANCEL_FLOW" />
          <el-option label="号源回补" value="STOCK_RESTORE" />
          <el-option label="号源消息同步" value="STOCK_SYNC" />
          <el-option label="医院支付状态同步" value="HOSPITAL_PAYMENT_SYNC" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="fetchData(1)">查询</el-button>
        <el-button @click="resetSearch">清空</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="任务类型" min-width="180">
        <template slot-scope="scope">{{ taskTypeLabel(scope.row.taskType) }}</template>
      </el-table-column>
      <el-table-column prop="orderId" label="订单 ID" width="100" />
      <el-table-column label="状态" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">
            {{ statusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="attemptCount" label="尝试次数" width="100" align="center" />
      <el-table-column prop="nextRetryTime" label="下次重试" min-width="165" />
      <el-table-column label="最近错误" min-width="260">
        <template slot-scope="scope">
          <span class="error-text">{{ scope.row.lastError || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="updateTime" label="更新时间" min-width="165" />
      <el-table-column label="操作" width="120" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.status === 3"
            type="text"
            :loading="retryingId === scope.row.id"
            @click="retry(scope.row)"
          >
            重新投递
          </el-button>
          <span v-else class="muted-text">自动处理</span>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      class="pagination"
      :current-page="page"
      :page-size="limit"
      :total="total"
      layout="total, prev, pager, next, jumper"
      @current-change="fetchData"
    />
  </div>
</template>

<script>
import orderApi from '@/api/yygh/order'

export default {
  name: 'OrderCompensation',
  data() {
    return {
      loading: false,
      retryingId: null,
      page: 1,
      limit: 10,
      total: 0,
      list: [],
      statusList: [],
      searchObj: {
        status: null,
        taskType: ''
      }
    }
  },
  created() {
    this.loadStatusList()
    this.fetchData()
  },
  methods: {
    loadStatusList() {
      orderApi.getCompensationStatusList().then(response => {
        this.statusList = response.data.list || []
      })
    },
    fetchData(page = this.page) {
      this.page = page
      this.loading = true
      orderApi.getCompensationPage(this.page, this.limit, this.searchObj).then(response => {
        const pageModel = response.data.pageModel || {}
        this.list = pageModel.records || []
        this.total = pageModel.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    resetSearch() {
      this.searchObj = { status: null, taskType: '' }
      this.fetchData(1)
    },
    retry(row) {
      this.$confirm('确认重新投递该死信任务吗？', '提示', {
        type: 'warning'
      }).then(() => {
        this.retryingId = row.id
        return orderApi.retryCompensation(row.id)
      }).then(() => {
        this.$message.success('任务已重新投递')
        this.fetchData()
      }).finally(() => {
        this.retryingId = null
      })
    },
    statusLabel(status) {
      const item = this.statusList.find(entry => entry.status === status)
      return item ? item.label : '未知'
    },
    statusTagType(status) {
      return { 0: 'warning', 1: '', 2: 'success', 3: 'danger' }[status] || 'info'
    },
    taskTypeLabel(type) {
      return {
        CANCEL_FLOW: '取消流程（含退款）',
        STOCK_RESTORE: '号源回补',
        STOCK_SYNC: '号源消息同步',
        HOSPITAL_PAYMENT_SYNC: '医院支付状态同步'
      }[type] || type || '未知'
    }
  }
}
</script>

<style scoped>
.page-heading {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-heading h2 {
  margin: 0 0 6px;
  color: #303133;
}

.page-heading p,
.muted-text {
  margin: 0;
  color: #909399;
  font-size: 12px;
}

.error-text {
  color: #f56c6c;
  word-break: break-all;
}

.pagination {
  padding: 30px 0;
  text-align: center;
}
</style>
