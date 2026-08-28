<template>
  <div class="help-center page-component">
    <section class="help-hero" aria-labelledby="help-title">
      <div class="help-hero-copy">
        <span class="help-eyebrow">预约服务指南</span>
        <h1 id="help-title">帮助中心</h1>
        <p>从预约挂号到到院就诊，快速找到常见问题的处理方法。</p>
      </div>
      <form class="help-search" role="search" @submit.prevent="submitSearch">
        <el-input
          v-model.trim="keyword"
          prefix-icon="el-icon-search"
          clearable
          aria-label="搜索帮助文章"
          placeholder="搜索挂号、就诊人、支付或退款问题"
          @clear="submitSearch"
        />
        <el-button native-type="submit" type="primary">搜索答案</el-button>
      </form>
    </section>

    <div class="service-path" aria-label="帮助中心使用步骤">
      <div class="path-item">
        <span class="path-index">1</span>
        <div><strong>选择问题分类</strong><small>定位预约服务环节</small></div>
      </div>
      <span class="path-arrow el-icon-arrow-right" aria-hidden="true" />
      <div class="path-item">
        <span class="path-index">2</span>
        <div><strong>查看处理步骤</strong><small>按指引核对和操作</small></div>
      </div>
      <span class="path-arrow el-icon-arrow-right" aria-hidden="true" />
      <div class="path-item">
        <span class="path-index">3</span>
        <div><strong>继续办理业务</strong><small>完成挂号或就诊准备</small></div>
      </div>
    </div>

    <div class="help-layout">
      <aside class="category-panel" aria-label="帮助分类">
        <div class="panel-title">问题分类</div>
        <button
          type="button"
          class="category-item"
          :class="{ active: selectedCategory === '' }"
          @click="selectCategory('')"
        >
          <span><i class="el-icon-menu" />全部问题</span>
          <em>{{ articleList.length }}</em>
        </button>
        <button
          v-for="category in categories"
          :key="category.code"
          type="button"
          class="category-item"
          :class="{ active: selectedCategory === category.code }"
          @click="selectCategory(category.code)"
        >
          <span><i :class="categoryIcon(category.code)" />{{ category.name }}</span>
          <em>{{ category.count }}</em>
        </button>
      </aside>

      <main class="article-panel">
        <div v-if="helpUnavailable" class="help-state" role="status">
          <i class="el-icon-warning-outline" />
          <h2>帮助内容暂时无法加载</h2>
          <p>请稍后刷新页面，或返回首页继续选择医院。</p>
          <el-button type="primary" plain @click="reloadPage">重新加载</el-button>
        </div>

        <article v-else-if="currentArticle" class="article-detail">
          <button type="button" class="back-button" @click="closeArticle">
            <i class="el-icon-back" />返回问题列表
          </button>
          <span class="category-label">{{ currentArticle.categoryName }}</span>
          <h2>{{ currentArticle.title }}</h2>
          <p v-if="currentArticle.summary" class="article-lead">{{ currentArticle.summary }}</p>
          <div class="article-divider" />
          <div class="article-content">{{ currentArticle.content }}</div>
          <div class="article-footer">
            <span><i class="el-icon-circle-check" />以上内容由平台运营端维护并发布</span>
            <button type="button" @click="closeArticle">查看其他问题</button>
          </div>
        </article>

        <template v-else>
          <div class="article-heading">
            <div>
              <span class="article-kicker">{{ activeCategoryName }}</span>
              <h2>{{ keyword ? '搜索结果' : '常见问题' }}</h2>
            </div>
            <span class="result-count">共 {{ filteredArticles.length }} 条</span>
          </div>
          <p v-if="keyword" class="search-summary">
            与“{{ keyword }}”相关的帮助内容
            <button type="button" @click="clearSearch">清除搜索</button>
          </p>
          <div v-if="filteredArticles.length" class="article-list">
            <button
              v-for="article in filteredArticles"
              :key="article.id"
              type="button"
              class="article-row"
              @click="openArticle(article)"
            >
              <span class="article-row-icon"><i :class="categoryIcon(article.categoryCode)" /></span>
              <span class="article-row-copy">
                <strong>{{ article.title }}</strong>
                <small>{{ article.summary || excerpt(article.content) }}</small>
              </span>
              <span class="article-row-category">{{ article.categoryName }}</span>
              <i class="el-icon-arrow-right article-row-arrow" />
            </button>
          </div>
          <div v-else class="help-state" role="status">
            <i class="el-icon-search" />
            <h2>没有找到相关问题</h2>
            <p>可以更换关键词，或切换到全部问题继续查找。</p>
            <el-button type="primary" plain @click="resetFilters">查看全部问题</el-button>
          </div>
        </template>
      </main>
    </div>
  </div>
</template>

<script>
import helpApi from '@/api/help.js'

export default {
  asyncData({ query }) {
    return Promise.all([
      helpApi.getPublishedList().catch(() => null),
      helpApi.getCategories().catch(() => null)
    ]).then(([articleResponse, categoryResponse]) => {
      const articleList = articleResponse ? articleResponse.data.list || [] : []
      let categories = categoryResponse ? categoryResponse.data.list || [] : []
      if (!categories.length && articleList.length) {
        const categoryMap = articleList.reduce((result, article) => {
          if (!result[article.categoryCode]) {
            result[article.categoryCode] = {
              code: article.categoryCode,
              name: article.categoryName,
              count: 0
            }
          }
          result[article.categoryCode].count += 1
          return result
        }, {})
        categories = Object.keys(categoryMap).map(code => categoryMap[code])
      }
      return {
        articleList,
        categories,
        helpUnavailable: !articleResponse,
        keyword: typeof query.keyword === 'string' ? query.keyword : '',
        selectedCategory: typeof query.category === 'string' ? query.category : '',
        selectedArticleId: typeof query.article === 'string' ? query.article : ''
      }
    })
  },
  data() {
    return {
      articleList: [],
      categories: [],
      helpUnavailable: false,
      keyword: '',
      selectedCategory: '',
      selectedArticleId: ''
    }
  },
  head() {
    return {
      title: '帮助中心 - 医院预约挂号平台',
      meta: [
        { hid: 'description', name: 'description', content: '预约挂号、实名认证、就诊人、支付退款和就诊服务帮助指南' }
      ]
    }
  },
  computed: {
    filteredArticles() {
      const normalizedKeyword = this.keyword.trim().toLowerCase()
      return this.articleList.filter(article => {
        const matchesCategory = !this.selectedCategory || article.categoryCode === this.selectedCategory
        if (!matchesCategory || !normalizedKeyword) {
          return matchesCategory
        }
        const searchable = [article.title, article.summary, article.keywords, article.content]
          .filter(Boolean)
          .join(' ')
          .toLowerCase()
        return searchable.includes(normalizedKeyword)
      })
    },
    currentArticle() {
      if (!this.selectedArticleId) {
        return null
      }
      return this.articleList.find(article => String(article.id) === String(this.selectedArticleId)) || null
    },
    activeCategoryName() {
      if (!this.selectedCategory) {
        return '全部帮助'
      }
      const category = this.categories.find(item => item.code === this.selectedCategory)
      return category ? category.name : '帮助内容'
    }
  },
  watch: {
    '$route.query': {
      handler(query) {
        this.keyword = typeof query.keyword === 'string' ? query.keyword : ''
        this.selectedCategory = typeof query.category === 'string' ? query.category : ''
        this.selectedArticleId = typeof query.article === 'string' ? query.article : ''
      },
      deep: true
    }
  },
  methods: {
    categoryIcon(code) {
      const icons = {
        registration: 'el-icon-date',
        account: 'el-icon-user',
        patient: 'el-icon-user-solid',
        payment: 'el-icon-bank-card',
        cancel: 'el-icon-refresh-left',
        visit: 'el-icon-first-aid-kit'
      }
      return icons[code] || 'el-icon-question'
    },
    excerpt(content) {
      if (!content) {
        return ''
      }
      return content.replace(/\s+/g, ' ').slice(0, 90)
    },
    submitSearch() {
      this.selectedArticleId = ''
      this.updateRoute()
    },
    clearSearch() {
      this.keyword = ''
      this.updateRoute()
    },
    selectCategory(code) {
      this.selectedCategory = code
      this.selectedArticleId = ''
      this.updateRoute()
    },
    openArticle(article) {
      this.selectedArticleId = String(article.id)
      this.updateRoute()
    },
    closeArticle() {
      this.selectedArticleId = ''
      this.updateRoute()
    },
    resetFilters() {
      this.keyword = ''
      this.selectedCategory = ''
      this.selectedArticleId = ''
      this.updateRoute()
    },
    updateRoute() {
      const query = {}
      if (this.keyword) query.keyword = this.keyword
      if (this.selectedCategory) query.category = this.selectedCategory
      if (this.selectedArticleId) query.article = this.selectedArticleId
      this.$router.replace({ path: '/help', query }).catch(() => {})
    },
    reloadPage() {
      window.location.reload()
    }
  }
}
</script>

<style scoped>
.help-center {
  color: #303133;
}

.help-hero {
  display: flex;
  min-height: 184px;
  padding: 36px 48px;
  background: #eef6ff;
  border-bottom: 1px solid #dceafb;
  align-items: center;
  justify-content: space-between;
}

.help-hero-copy {
  max-width: 500px;
}

.help-eyebrow,
.article-kicker {
  color: #4490f1;
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 2px;
}

.help-hero h1 {
  margin: 10px 0 8px;
  color: #1f2d3d;
  font-size: 32px;
  line-height: 1.25;
}

.help-hero p {
  margin: 0;
  color: #6b7888;
  font-size: 16px;
  line-height: 1.7;
}

.help-search {
  display: flex;
  width: 510px;
  padding: 8px;
  background: #fff;
  border: 1px solid #d9e7f7;
  box-shadow: 0 10px 28px rgba(68, 144, 241, .1);
}

.help-search .el-input {
  flex: 1;
}

.help-search .el-button {
  min-width: 104px;
  margin-left: 8px;
}

.help-search >>> .el-input__inner {
  border: 0;
}

.service-path {
  display: flex;
  min-height: 92px;
  padding: 20px 48px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  align-items: center;
  justify-content: center;
}

.path-item {
  display: flex;
  min-width: 220px;
  align-items: center;
}

.path-index {
  display: inline-flex;
  width: 32px;
  height: 32px;
  margin-right: 12px;
  color: #4490f1;
  background: #edf5ff;
  border: 1px solid #cfe3fb;
  border-radius: 50%;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.path-item strong,
.path-item small {
  display: block;
}

.path-item strong {
  margin-bottom: 4px;
  font-size: 14px;
}

.path-item small {
  color: #909399;
  font-size: 12px;
}

.path-arrow {
  margin: 0 28px 0 8px;
  color: #b7c4d4;
}

.help-layout {
  display: grid;
  padding: 32px 0 48px;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 24px;
}

.category-panel,
.article-panel {
  background: #fff;
  border: 1px solid #ebeef5;
}

.category-panel {
  align-self: start;
  padding: 12px;
}

.panel-title {
  padding: 12px 14px 16px;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.category-item {
  display: flex;
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  color: #606266;
  background: transparent;
  border: 0;
  border-left: 3px solid transparent;
  cursor: pointer;
  align-items: center;
  justify-content: space-between;
  text-align: left;
}

.category-item span {
  display: flex;
  align-items: center;
}

.category-item i {
  width: 24px;
  color: #8a98a9;
  font-size: 16px;
}

.category-item em {
  color: #a5afbb;
  font-size: 12px;
  font-style: normal;
}

.category-item:hover,
.category-item:focus,
.category-item.active {
  color: #2678df;
  background: #f2f7fe;
  border-left-color: #4490f1;
  outline: none;
}

.category-item.active i,
.category-item.active em {
  color: #4490f1;
}

.article-panel {
  min-height: 480px;
  padding: 32px 36px;
}

.article-heading {
  display: flex;
  margin-bottom: 20px;
  align-items: flex-end;
  justify-content: space-between;
}

.article-heading h2 {
  margin: 6px 0 0;
  font-size: 24px;
}

.result-count {
  color: #909399;
  font-size: 13px;
}

.search-summary {
  margin: -4px 0 20px;
  padding: 12px 16px;
  color: #5d6b7a;
  background: #f7f9fc;
  border-left: 3px solid #8dbcf7;
}

.search-summary button,
.article-footer button {
  color: #4490f1;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.article-list {
  border-top: 1px solid #ebeef5;
}

.article-row {
  display: grid;
  width: 100%;
  min-height: 92px;
  padding: 18px 12px;
  color: #303133;
  background: #fff;
  border: 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  grid-template-columns: 40px minmax(0, 1fr) 96px 20px;
  align-items: center;
  text-align: left;
}

.article-row:hover,
.article-row:focus {
  background: #f8fbff;
  outline: none;
}

.article-row-icon {
  display: inline-flex;
  width: 32px;
  height: 32px;
  color: #4490f1;
  background: #edf5ff;
  align-items: center;
  justify-content: center;
}

.article-row-copy strong,
.article-row-copy small {
  display: block;
}

.article-row-copy strong {
  margin-bottom: 7px;
  font-size: 16px;
}

.article-row-copy small {
  overflow: hidden;
  color: #909399;
  font-size: 13px;
  line-height: 20px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-row-category {
  color: #7d8998;
  font-size: 12px;
  text-align: right;
}

.article-row-arrow {
  color: #c0c8d2;
  text-align: right;
}

.back-button {
  min-height: 40px;
  margin: -8px 0 24px;
  padding: 0;
  color: #606266;
  background: transparent;
  border: 0;
  cursor: pointer;
}

.back-button i {
  margin-right: 8px;
}

.back-button:hover,
.back-button:focus {
  color: #4490f1;
}

.category-label {
  display: inline-block;
  padding: 5px 10px;
  color: #2678df;
  background: #edf5ff;
  font-size: 12px;
}

.article-detail h2 {
  margin: 14px 0 10px;
  color: #1f2d3d;
  font-size: 28px;
}

.article-lead {
  margin: 0;
  color: #7d8998;
  font-size: 15px;
  line-height: 1.8;
}

.article-divider {
  height: 1px;
  margin: 24px 0;
  background: #ebeef5;
}

.article-content {
  min-height: 180px;
  color: #404b59;
  font-size: 15px;
  line-height: 2;
  white-space: pre-line;
}

.article-footer {
  display: flex;
  margin-top: 36px;
  padding: 16px 0;
  color: #8a98a9;
  border-top: 1px solid #ebeef5;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.article-footer i {
  margin-right: 6px;
  color: #67c23a;
}

.help-state {
  padding: 72px 20px;
  color: #909399;
  text-align: center;
}

.help-state > i {
  color: #9bbce4;
  font-size: 42px;
}

.help-state h2 {
  margin: 16px 0 8px;
  color: #4b5968;
  font-size: 20px;
}

.help-state p {
  margin: 0 0 20px;
}

@media (max-width: 900px) {
  .help-hero {
    display: block;
    padding: 28px 24px;
  }

  .help-search {
    width: auto;
    margin-top: 24px;
  }

  .help-layout {
    grid-template-columns: 1fr;
  }

  .category-panel {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
  }

  .panel-title {
    grid-column: 1 / -1;
  }
}
</style>
