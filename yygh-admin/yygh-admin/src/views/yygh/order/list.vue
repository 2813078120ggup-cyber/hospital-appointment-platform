<template>
  <div class="app-container">
    <div class="page-header">
      <div>
        <h2>预约订单</h2>
        <p>查询平台订单及其当前履约状态。</p>
      </div>
      <el-button icon="el-icon-refresh" @click="fetchData(page)">刷新</el-button>
    </div>

    <el-form :inline="true" class="filter-form">
      <el-form-item>
        <el-input v-model.trim="searchObj.outTradeNo" clearable placeholder="平台交易号" />
      </el-form-item>
      <el-form-item>
        <el-input v-model.trim="searchObj.patientName" clearable placeholder="就诊人姓名" />
      </el-form-item>
      <el-form-item>
        <el-input v-model.trim="searchObj.keyword" clearable placeholder="医院或科室" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.orderStatus" clearable placeholder="订单状态">
          <el-option
            v-for="item in statusList"
            :key="item.status"
            :label="item.comment"
            :value="String(item.status)"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.cancelStatus" clearable placeholder="取消处理状态">
          <el-option label="未申请" value="0" />
          <el-option label="处理中" value="1" />
          <el-option label="处理成功" value="2" />
          <el-option label="处理失败" value="3" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="searchObj.reserveDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="就诊日期"
        />
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="searchObj.createTimeBegin"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          placeholder="下单开始时间"
        />
      </el-form-item>
      <el-form-item>
        <el-date-picker
          v-model="searchObj.createTimeEnd"
          type="datetime"
          value-format="yyyy-MM-dd HH:mm:ss"
          placeholder="下单截止时间"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="fetchData(1)">查询</el-button>
        <el-button @click="resetData">清空</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="listLoading" :data="list" border fit>
      <el-table-column label="序号" width="65" align="center">
        <template slot-scope="scope">{{ (page - 1) * limit + scope.$index + 1 }}</template>
      </el-table-column>
      <el-table-column prop="outTradeNo" label="平台交易号" min-width="190" />
      <el-table-column label="就诊人" min-width="130">
        <template slot-scope="scope">
          <div>{{ scope.row.patientName || '-' }}</div>
          <div class="secondary-text">{{ scope.row.patientPhone || '-' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="预约信息" min-width="220">
        <template slot-scope="scope">
          <div>{{ scope.row.hosname }}</div>
          <div class="secondary-text">{{ scope.row.depname }} · {{ scope.row.title || '普通号' }}</div>
        </template>
      </el-table-column>
      <el-table-column label="就诊时间" min-width="130">
        <template slot-scope="scope">
          {{ scope.row.reserveDate }} {{ scope.row.reserveTime === 0 ? '上午' : '下午' }}
        </template>
      </el-table-column>
      <el-table-column label="金额" width="90" align="right">
        <template slot-scope="scope">¥ {{ scope.row.amount || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="150" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.orderStatus)">
            {{ scope.row.param.orderStatusString || '未知' }}
          </el-tag>
          <div v-if="scope.row.cancelStatus === 1" class="processing-text">取消处理中</div>
          <div v-else-if="scope.row.cancelStatus === 3" class="failed-text">取消处理失败</div>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="下单时间" min-width="165" />
      <el-table-column label="操作" width="90" align="center" fixed="right">
        <template slot-scope="scope">
          <router-link :to="'/order/show/' + scope.row.id">
            <el-button type="text">查看</el-button>
          </router-link>
        </template>
      </el-table-column>
    </el-table>

    <div v-if="!listLoading && list.length === 0" class="empty-tip">暂无符合条件的订单</div>

    <el-pagination
      :current-page="page"
      :page-size="limit"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      layout="total, sizes, prev, pager, next, jumper"
      class="pagination"
      @current-change="fetchData"
      @size-change="changeSize"
    />
  </div>
</template>

<script>
import orderApi from '@/api/yygh/order'

export default {
  name: 'OrderList',
  data() {
    return {
      listLoading: false,
      list: [],
      page: 1,
      limit: 10,
      total: 0,
      statusList: [],
      searchObj: {
        cancelStatus: this.$route.query.cancelStatus || ''
      }
    }
  },
  created() {
    this.loadStatusList()
    this.fetchData()
  },
  methods: {
    loadStatusList() {
      orderApi.getStatusList().then(response => {
        this.statusList = response.data.list || []
      })
    },
    fetchData(page = 1) {
      this.page = page
      this.listLoading = true
      orderApi.getPageList(this.page, this.limit, this.searchObj).then(response => {
        const pageModel = response.data.pageModel
        this.list = pageModel.records || []
        this.total = pageModel.total || 0
      }).finally(() => {
        this.listLoading = false
      })
    },
    changeSize(size) {
      this.limit = size
      this.fetchData(1)
    },
    resetData() {
      this.searchObj = {}
      this.fetchData(1)
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

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0 0 6px;
  color: #303133;
}

.page-header p,
.secondary-text {
  margin: 0;
  color: #909399;
  font-size: 12px;
  line-height: 20px;
}

.filter-form {
  padding: 18px 18px 0;
  margin-bottom: 20px;
  background: #f7f9fc;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.empty-tip {
  padding: 24px;
  color: #909399;
  text-align: center;
}

.pagination {
  padding: 30px 0;
  text-align: center;
}

.processing-text,
.failed-text {
  margin-top: 6px;
  font-size: 12px;
}

.processing-text {
  color: #e6a23c;
}

.failed-text {
  color: #f56c6c;
}
</style>
