<template>
  <div class="app-container">
    <div class="form-heading">
      <h2>{{ isEdit ? '编辑帮助文章' : '新建帮助文章' }}</h2>
      <p>保存后默认为草稿，请在列表确认内容后发布到用户端帮助中心。</p>
    </div>

    <el-form
      ref="articleForm"
      :model="article"
      :rules="rules"
      label-width="110px"
      class="article-form"
    >
      <el-form-item label="帮助分类" prop="categoryCode">
        <el-select v-model="article.categoryCode" placeholder="请选择帮助分类">
          <el-option
            v-for="category in categories"
            :key="category.code"
            :label="category.name"
            :value="category.code"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="文章标题" prop="title">
        <el-input v-model.trim="article.title" maxlength="200" show-word-limit />
      </el-form-item>
      <el-form-item label="文章摘要">
        <el-input
          v-model.trim="article.summary"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="用于问题列表概览，可不填"
        />
      </el-form-item>
      <el-form-item label="搜索关键词">
        <el-input
          v-model.trim="article.keywords"
          maxlength="500"
          show-word-limit
          placeholder="用空格分隔，例如：退款 到账 支付渠道"
        />
      </el-form-item>
      <el-form-item label="文章内容" prop="content">
        <el-input
          v-model="article.content"
          type="textarea"
          :rows="14"
          maxlength="10000"
          show-word-limit
          placeholder="使用清晰的步骤和短段落说明处理方法"
        />
      </el-form-item>
      <el-form-item label="展示排序">
        <el-input-number v-model="article.sort" :min="0" :max="9999" />
        <span class="form-tip">数值越大越靠前</span>
      </el-form-item>
      <el-form-item>
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
        <el-button @click="$router.push('/content/help/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import helpApi from '@/api/yygh/help'

export default {
  name: 'HelpArticleForm',
  data() {
    return {
      saving: false,
      categories: [],
      article: {
        categoryCode: '',
        title: '',
        summary: '',
        keywords: '',
        content: '',
        sort: 0
      },
      rules: {
        categoryCode: [{ required: true, message: '请选择帮助分类', trigger: 'change' }],
        title: [{ required: true, message: '请输入文章标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入文章内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isEdit() {
      return Boolean(this.$route.params && this.$route.params.id)
    }
  },
  created() {
    helpApi.getCategories().then(response => {
      this.categories = response.data.list || []
    })
    if (this.isEdit) {
      helpApi.show(this.$route.params.id).then(response => {
        this.article = response.data.article || this.article
      })
    }
  },
  methods: {
    save() {
      this.$refs.articleForm.validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        const request = this.isEdit ? helpApi.update(this.article) : helpApi.save(this.article)
        request.then(() => {
          this.$message.success(this.isEdit ? '保存成功' : '草稿已创建')
          this.$router.push('/content/help/list')
        }).finally(() => {
          this.saving = false
        })
      })
    }
  }
}
</script>

<style scoped>
.form-heading {
  margin-bottom: 24px;
}

.form-heading h2 {
  margin: 0 0 6px;
}

.form-heading p,
.form-tip {
  color: #909399;
}

.form-heading p {
  margin: 0;
}

.article-form {
  max-width: 860px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
}
</style>
