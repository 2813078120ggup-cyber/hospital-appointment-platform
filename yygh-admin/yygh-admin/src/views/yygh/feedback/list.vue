<template>
  <div class="app-container">
    <div class="page-heading">
      <div>
        <h2>意见反馈</h2>
        <p>查看并处理用户提交的平台反馈与医院反馈。</p>
      </div>
    </div>

    <el-form :inline="true" :model="searchObj">
      <el-form-item>
        <el-input v-model.trim="searchObj.keyword" clearable placeholder="反馈人/电话/医院/内容" style="width: 220px;" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.type" clearable placeholder="反馈类型">
          <el-option label="平台反馈" :value="1" />
          <el-option label="医院反馈" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.status" clearable placeholder="处理状态">
          <el-option label="待处理" :value="0" />
          <el-option label="已处理" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="fetchData(1)">查询</el-button>
        <el-button @click="resetSearch">清空</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="类型" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.type === 2 ? 'warning' : ''" size="small">
            {{ scope.row.type === 2 ? '医院反馈' : '平台反馈' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="反馈人" width="120">
        <template slot-scope="scope">{{ scope.row.userName || '-' }}</template>
      </el-table-column>
      <el-table-column prop="phone" label="联系电话" width="130">
        <template slot-scope="scope">{{ scope.row.phone || '-' }}</template>
      </el-table-column>
      <el-table-column label="关联医院" min-width="160">
        <template slot-scope="scope">{{ scope.row.hosname || '-' }}</template>
      </el-table-column>
      <el-table-column label="反馈内容" min-width="260">
        <template slot-scope="scope">
          <div class="feedback-content">{{ scope.row.content }}</div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '已处理' : '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="提交时间" width="165">
        <template slot-scope="scope">{{ scope.row.createTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" @click="openDetail(scope.row)">查看</el-button>
          <el-button v-if="scope.row.status !== 1" type="text" @click="openHandle(scope.row)">处理</el-button>
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

    <el-dialog title="反馈详情" :visible.sync="detailVisible" width="560px" append-to-body>
      <div class="detail-list">
        <div class="detail-item">
          <span class="detail-label">反馈类型</span>
          <span>{{ currentRow.type === 2 ? '医院反馈' : '平台反馈' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">反馈人</span>
          <span>{{ currentRow.userName || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">联系电话</span>
          <span>{{ currentRow.phone || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">关联医院</span>
          <span>{{ currentRow.hosname || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">反馈内容</span>
          <span class="feedback-content">{{ currentRow.content }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">提交时间</span>
          <span>{{ currentRow.createTime || '-' }}</span>
        </div>
        <div class="detail-item">
          <span class="detail-label">处理回复</span>
          <span>{{ currentRow.reply || '未回复' }}</span>
        </div>
      </div>
    </el-dialog>

    <el-dialog title="处理反馈" :visible.sync="handleVisible" width="560px" append-to-body>
      <el-form :model="handleForm" label-width="90px">
        <el-form-item label="反馈内容">
          <div class="feedback-content">{{ currentRow.content }}</div>
        </el-form-item>
        <el-form-item label="处理回复">
          <el-input
            v-model="handleForm.reply"
            type="textarea"
            :rows="5"
            maxlength="500"
            show-word-limit
            placeholder="请输入处理回复（选填）"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" :loading="handleLoading" @click="submitHandle">确认处理</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import feedbackApi from '@/api/yygh/feedback'

export default {
  name: 'FeedbackList',
  data() {
    return {
      loading: false,
      handleLoading: false,
      page: 1,
      limit: 10,
      total: 0,
      list: [],
      searchObj: {
        keyword: '',
        type: null,
        status: null
      },
      detailVisible: false,
      handleVisible: false,
      currentRow: {},
      handleForm: {
        status: 1,
        reply: ''
      }
    }
  },
  created() {
    this.fetchData()
  },
  methods: {
    fetchData(page = this.page) {
      this.page = page
      this.loading = true
      feedbackApi.getPageList(this.page, this.limit, this.searchObj).then(response => {
        const pageModel = response.data.pageModel || {}
        this.list = pageModel.records || []
        this.total = pageModel.total || 0
      }).finally(() => {
        this.loading = false
      })
    },
    resetSearch() {
      this.searchObj = {
        keyword: '',
        type: null,
        status: null
      }
      this.fetchData(1)
    },
    openDetail(row) {
      this.currentRow = row
      this.detailVisible = true
    },
    openHandle(row) {
      this.currentRow = row
      this.handleForm = {
        status: 1,
        reply: row.reply || ''
      }
      this.handleVisible = true
    },
    submitHandle() {
      this.handleLoading = true
      feedbackApi.handle(this.currentRow.id, this.handleForm).then(() => {
        this.$message.success('处理成功')
        this.handleVisible = false
        this.fetchData()
      }).finally(() => {
        this.handleLoading = false
      })
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
}

.page-heading p {
  margin: 0;
  color: #909399;
}

.feedback-content {
  color: #303133;
  font-size: 13px;
  line-height: 20px;
  white-space: pre-wrap;
  word-break: break-all;
}

.detail-list {
  padding: 0 10px;
}

.detail-item {
  display: flex;
  margin-bottom: 14px;
  line-height: 22px;
}

.detail-label {
  flex: 0 0 80px;
  color: #909399;
}

.pagination {
  margin-top: 20px;
}
</style>
