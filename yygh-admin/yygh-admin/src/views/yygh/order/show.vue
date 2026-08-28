<template>
  <div v-loading="loading" class="app-container order-detail">
    <div class="page-header">
      <div>
        <h2>订单详情</h2>
        <p>{{ orderInfo.outTradeNo || '正在加载订单信息' }}</p>
      </div>
      <div class="header-actions">
        <el-button
          v-if="canCancel"
          type="danger"
          plain
          icon="el-icon-warning-outline"
          @click="openCancelDialog"
        >运营取消</el-button>
        <el-button icon="el-icon-back" @click="$router.push('/order/list')">返回订单列表</el-button>
      </div>
    </div>

    <el-card shadow="never" class="detail-card">
      <div slot="header" class="card-header">
        <span>预约信息</span>
        <el-tag :type="statusTagType(orderInfo.orderStatus)">
          {{ orderStatusText }}
        </el-tag>
      </div>
      <table class="detail-table">
        <tbody>
          <tr>
            <th>平台交易号</th>
            <td>{{ orderInfo.outTradeNo || '-' }}</td>
            <th>用户 ID</th>
            <td>{{ orderInfo.userId || '-' }}</td>
          </tr>
          <tr>
            <th>就诊人</th>
            <td>{{ orderInfo.patientName || '-' }} / {{ orderInfo.patientPhone || '-' }}</td>
            <th>医院记录号</th>
            <td>{{ orderInfo.hosRecordId || '-' }}</td>
          </tr>
          <tr>
            <th>医院</th>
            <td>{{ orderInfo.hosname || '-' }}</td>
            <th>科室</th>
            <td>{{ orderInfo.depname || '-' }}</td>
          </tr>
          <tr>
            <th>预约时间</th>
            <td>{{ reserveTimeText }}</td>
            <th>医事服务费</th>
            <td>¥ {{ orderInfo.amount || 0 }}</td>
          </tr>
          <tr>
            <th>建议取号</th>
            <td>{{ orderInfo.fetchTime || '-' }}</td>
            <th>取号地点</th>
            <td>{{ orderInfo.fetchAddress || '-' }}</td>
          </tr>
          <tr>
            <th>最晚取消时间</th>
            <td>{{ orderInfo.quitTime || '-' }}</td>
            <th>下单时间</th>
            <td>{{ orderInfo.createTime || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </el-card>

    <el-card v-if="hasCancellationRecord" shadow="never" class="detail-card cancellation-card">
      <div slot="header" class="card-header">
        <span>取消处置记录</span>
        <el-tag :type="cancelStatusTagType">{{ cancelStatusText }}</el-tag>
      </div>
      <table class="detail-table">
        <tbody>
          <tr>
            <th>发起来源</th>
            <td>{{ cancelSourceText }}</td>
            <th>操作人</th>
            <td>{{ orderInfo.cancelOperator || '-' }}</td>
          </tr>
          <tr>
            <th>取消原因</th>
            <td colspan="3">{{ orderInfo.cancelReason || '-' }}</td>
          </tr>
          <tr>
            <th>完成时间</th>
            <td>{{ orderInfo.cancelTime || '-' }}</td>
            <th>异常信息</th>
            <td :class="{ 'danger-text': orderInfo.cancelError }">{{ orderInfo.cancelError || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never" class="detail-card">
          <div slot="header" class="card-header"><span>支付记录</span></div>
          <div v-if="paymentInfo">
            <p><span>支付状态</span>{{ paymentInfo.paymentStatusString }}</p>
            <p><span>支付金额</span>¥ {{ paymentInfo.totalAmount || 0 }}</p>
            <p><span>微信交易号</span>{{ paymentInfo.tradeNo || '-' }}</p>
            <p><span>回调时间</span>{{ paymentInfo.callbackTime || '-' }}</p>
          </div>
          <div v-else class="empty-record">暂无支付记录</div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never" class="detail-card">
          <div slot="header" class="card-header"><span>退款记录</span></div>
          <div v-if="refundInfo">
            <p><span>退款状态</span>{{ refundInfo.refundStatusString }}</p>
            <p><span>退款金额</span>¥ {{ refundInfo.totalAmount || 0 }}</p>
            <p><span>退款交易号</span>{{ refundInfo.tradeNo || '-' }}</p>
            <p><span>回调时间</span>{{ refundInfo.callbackTime || '-' }}</p>
          </div>
          <div v-else class="empty-record">暂无退款记录</div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      title="运营取消预约"
      :visible.sync="cancelDialogVisible"
      width="520px"
      :close-on-click-modal="false"
    >
      <el-alert
        :title="cancelRiskText"
        type="warning"
        :closable="false"
        show-icon
        class="cancel-alert"
      />
      <el-form label-position="top">
        <el-form-item label="取消原因（将写入订单操作记录）" required>
          <el-input
            v-model.trim="cancelReason"
            type="textarea"
            :rows="4"
            maxlength="255"
            show-word-limit
            placeholder="请说明用户诉求、医院通知或异常处置依据，至少 5 个字符"
          />
        </el-form-item>
        <el-checkbox v-model="cancelRiskConfirmed">我已核对订单和就诊人信息，并确认执行取消</el-checkbox>
      </el-form>
      <span slot="footer">
        <el-button @click="cancelDialogVisible = false">暂不处理</el-button>
        <el-button
          type="danger"
          :loading="cancelSubmitting"
          :disabled="!cancelRiskConfirmed"
          @click="submitCancel"
        >确认取消预约</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import orderApi from '@/api/yygh/order'

export default {
  name: 'OrderShow',
  data() {
    return {
      loading: true,
      orderInfo: { param: {}},
      paymentInfo: null,
      refundInfo: null,
      cancelDialogVisible: false,
      cancelReason: '',
      cancelRiskConfirmed: false,
      cancelSubmitting: false
    }
  },
  computed: {
    orderStatusText() {
      return (this.orderInfo.param && this.orderInfo.param.orderStatusString) || '未知'
    },
    reserveTimeText() {
      if (!this.orderInfo.reserveDate) {
        return '-'
      }
      return this.orderInfo.reserveDate + (this.orderInfo.reserveTime === 0 ? ' 上午' : ' 下午')
    },
    canCancel() {
      return Boolean(this.orderInfo.param && this.orderInfo.param.canCancel)
    },
    hasCancellationRecord() {
      return Boolean(this.orderInfo.cancelStatus) || this.orderInfo.orderStatus === -1
    },
    cancelStatusText() {
      const labels = {
        1: '处理中',
        2: '处理成功',
        3: '处理失败'
      }
      return labels[this.orderInfo.cancelStatus] || (this.orderInfo.orderStatus === -1 ? '已取消' : '未申请')
    },
    cancelStatusTagType() {
      const types = {
        1: 'warning',
        2: 'success',
        3: 'danger'
      }
      return types[this.orderInfo.cancelStatus] || 'info'
    },
    cancelSourceText() {
      const labels = {
        1: '用户主动取消',
        2: '平台运营取消'
      }
      return labels[this.orderInfo.cancelSource] || '历史记录'
    },
    cancelRiskText() {
      return this.orderInfo.orderStatus === 1
        ? '该订单已支付，确认后将先取消医院预约，再发起全额退款。'
        : '确认后将取消医院预约并释放对应排班号源。'
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData() {
      this.loading = true
      orderApi.show(this.$route.params.id).then(response => {
        this.orderInfo = response.data.orderInfo || { param: {}}
        this.paymentInfo = response.data.paymentInfo || null
        this.refundInfo = response.data.refundInfo || null
      }).finally(() => {
        this.loading = false
      })
    },
    openCancelDialog() {
      this.cancelReason = ''
      this.cancelRiskConfirmed = false
      this.cancelDialogVisible = true
    },
    submitCancel() {
      if (!this.cancelReason || this.cancelReason.length < 5) {
        this.$message.warning('取消原因至少填写 5 个字符')
        return
      }
      if (!this.cancelRiskConfirmed) {
        this.$message.warning('请先确认已核对订单信息')
        return
      }
      this.cancelSubmitting = true
      orderApi.cancelOrder(this.orderInfo.id, this.cancelReason).then(() => {
        this.$message.success('订单取消处理完成')
        this.cancelDialogVisible = false
        this.fetchData()
      }).finally(() => {
        this.cancelSubmitting = false
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

<style scoped>
.order-detail {
  min-height: calc(100vh - 84px);
  background: #f5f7fa;
}

.page-header,
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.page-header {
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.page-header h2 {
  margin: 0 0 6px;
}

.page-header p,
.empty-record {
  margin: 0;
  color: #909399;
}

.detail-card {
  margin-bottom: 20px;
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  padding: 13px 14px;
  border: 1px solid #ebeef5;
  font-size: 14px;
}

.detail-table th {
  width: 130px;
  color: #606266;
  text-align: right;
  background: #f7f9fc;
}

.detail-card p {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  margin: 0;
  border-bottom: 1px solid #f0f2f5;
}

.detail-card p span {
  color: #909399;
}

.cancellation-card {
  border-left: 3px solid #e6a23c;
}

.cancel-alert {
  margin-bottom: 18px;
}

.danger-text {
  color: #f56c6c;
}
</style>
