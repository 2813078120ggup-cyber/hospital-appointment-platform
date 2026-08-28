<template>
  <div class="app-container">
    <div class="form-heading">
      <h2>{{ isEdit ? '编辑公告' : '新建公告' }}</h2>
      <p>保存后默认为草稿，请在公告列表确认内容后发布。</p>
    </div>

    <el-form
      ref="noticeForm"
      :model="notice"
      :rules="rules"
      label-width="110px"
      class="notice-form"
    >
      <el-form-item label="公告类型" prop="noticeType">
        <el-radio-group v-model="notice.noticeType" @change="handleTypeChange">
          <el-radio-button :label="1">平台公告</el-radio-button>
          <el-radio-button :label="2">停诊公告</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="公告标题" prop="title">
        <el-input v-model.trim="notice.title" maxlength="200" show-word-limit />
      </el-form-item>
      <template v-if="notice.noticeType === 2">
        <el-form-item label="医院名称" prop="hosname" required>
          <el-input v-model.trim="notice.hosname" maxlength="100" />
        </el-form-item>
        <el-form-item label="医院编号">
          <el-input v-model.trim="notice.hoscode" maxlength="30" placeholder="可选，用于关联医院页面" />
        </el-form-item>
      </template>
      <el-form-item label="公告摘要">
        <el-input
          v-model.trim="notice.summary"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="用于首页列表和公告概览，可不填"
        />
      </el-form-item>
      <el-form-item label="公告内容" prop="content">
        <el-input
          v-model="notice.content"
          type="textarea"
          :rows="10"
          maxlength="5000"
          show-word-limit
        />
      </el-form-item>
      <el-form-item label="展示排序">
        <el-input-number v-model="notice.sort" :min="0" :max="9999" />
        <span class="form-tip">数值越大越靠前</span>
      </el-form-item>
      <el-form-item>
        <el-button :loading="saving" type="primary" @click="save">保存</el-button>
        <el-button @click="$router.push('/content/notice/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import noticeApi from '@/api/yygh/notice'

export default {
  name: 'NoticeForm',
  data() {
    return {
      saving: false,
      notice: {
        noticeType: 1,
        title: '',
        summary: '',
        content: '',
        hoscode: '',
        hosname: '',
        sort: 0
      },
      rules: {
        noticeType: [{ required: true, message: '请选择公告类型', trigger: 'change' }],
        title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isEdit() {
      return Boolean(this.$route.params && this.$route.params.id)
    }
  },
  created() {
    if (this.isEdit) {
      noticeApi.show(this.$route.params.id).then(response => {
        this.notice = response.data.notice || this.notice
      })
    }
  },
  methods: {
    handleTypeChange(noticeType) {
      if (noticeType === 1) {
        this.notice.hoscode = ''
        this.notice.hosname = ''
      }
    },
    save() {
      this.$refs.noticeForm.validate(valid => {
        if (!valid) {
          return
        }
        if (this.notice.noticeType === 2 && !this.notice.hosname) {
          this.$message.warning('停诊公告必须填写医院名称')
          return
        }
        this.saving = true
        const request = this.isEdit ? noticeApi.update(this.notice) : noticeApi.save(this.notice)
        request.then(() => {
          this.$message.success(this.isEdit ? '保存成功' : '草稿已创建')
          this.$router.push('/content/notice/list')
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

.notice-form {
  max-width: 820px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
}
</style>
