<template>
  <div class="app-container">
    <div class="page-heading">
      <div>
        <h2>帮助中心管理</h2>
        <p>维护用户端可搜索的预约、支付和就诊指引，只有已发布文章会对外展示。</p>
      </div>
      <router-link to="/content/help/add">
        <el-button type="primary" icon="el-icon-plus">新建帮助文章</el-button>
      </router-link>
    </div>

    <el-form :inline="true" :model="searchObj">
      <el-form-item>
        <el-input
          v-model.trim="searchObj.keyword"
          clearable
          placeholder="标题、摘要或关键词"
          @keyup.enter.native="fetchData(1)"
        />
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.categoryCode" clearable placeholder="帮助分类">
          <el-option
            v-for="category in categories"
            :key="category.code"
            :label="category.name"
            :value="category.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchObj.status" clearable placeholder="发布状态">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="fetchData(1)">查询</el-button>
        <el-button @click="resetSearch">清空</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="list" border stripe>
      <el-table-column type="index" label="序号" width="60" align="center" />
      <el-table-column label="文章内容" min-width="340">
        <template slot-scope="scope">
          <div class="article-title">{{ scope.row.title }}</div>
          <div class="article-summary">{{ scope.row.summary || scope.row.content }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="categoryName" label="分类" width="120" align="center" />
      <el-table-column label="状态" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'info'" size="small">
            {{ scope.row.status === 1 ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="70" align="center" />
      <el-table-column prop="publishTime" label="发布时间" width="165">
        <template slot-scope="scope">{{ scope.row.publishTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="235" fixed="right">
        <template slot-scope="scope">
          <router-link :to="'/content/help/edit/' + scope.row.id">
            <el-button type="text">编辑</el-button>
          </router-link>
          <el-button
            type="text"
            :class="{ 'warning-action': scope.row.status === 1 }"
            @click="changeStatus(scope.row)"
          >
            {{ scope.row.status === 1 ? '下线' : '发布' }}
          </el-button>
          <el-button type="text" class="danger-action" @click="remove(scope.row)">删除</el-button>
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
import helpApi from '@/api/yygh/help'

export default {
  name: 'HelpArticleList',
  data() {
    return {
      loading: false,
      page: 1,
      limit: 10,
      total: 0,
      list: [],
      categories: [],
      searchObj: {
        keyword: '',
        categoryCode: '',
        status: null
      }
    }
  },
  created() {
    this.fetchCategories()
    this.fetchData()
  },
  methods: {
    fetchCategories() {
      helpApi.getCategories().then(response => {
        this.categories = response.data.list || []
      })
    },
    fetchData(page = this.page) {
      this.page = page
      this.loading = true
      helpApi.getPageList(this.page, this.limit, this.searchObj).then(response => {
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
        categoryCode: '',
        status: null
      }
      this.fetchData(1)
    },
    changeStatus(row) {
      const nextStatus = row.status === 1 ? 0 : 1
      const action = nextStatus === 1 ? '发布' : '下线'
      this.$confirm(`确认${action}“${row.title}”吗？`, `${action}帮助文章`, {
        type: nextStatus === 1 ? 'success' : 'warning',
        confirmButtonText: action,
        cancelButtonText: '取消'
      }).then(() => helpApi.updateStatus(row.id, nextStatus))
        .then(() => {
          this.$message.success(`${action}成功`)
          this.fetchData()
        }).catch(() => {})
    },
    remove(row) {
      this.$confirm(`删除后用户端将不再显示“${row.title}”，确认继续吗？`, '删除帮助文章', {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }).then(() => helpApi.remove(row.id))
        .then(() => {
          this.$message.success('删除成功')
          this.fetchData(this.list.length === 1 && this.page > 1 ? this.page - 1 : this.page)
        }).catch(() => {})
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

.page-heading p,
.article-summary {
  color: #909399;
}

.page-heading p {
  margin: 0;
}

.article-title {
  margin-bottom: 6px;
  color: #303133;
  font-weight: 600;
}

.article-summary {
  overflow: hidden;
  font-size: 12px;
  line-height: 18px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.pagination {
  margin-top: 20px;
}

.warning-action {
  color: #e6a23c;
}

.danger-action {
  color: #f56c6c;
}
</style>
