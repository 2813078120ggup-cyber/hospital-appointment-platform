<template>
  <div class="app-container">
    <h2>{{ isEdit ? '编辑医院设置' : '新增医院设置' }}</h2>
    <el-alert
      v-if="!isEdit"
      class="bootstrap-alert"
      title="新增医院会同步平台生成的签名信息"
      description="请先在平台医院服务与对应医院系统配置相同的 YYGH_HOSPITAL_BOOTSTRAP_TOKEN，再提交新增。"
      type="info"
      show-icon
      :closable="false"
    />

    <el-form ref="hospitalSetForm" :model="hospitalSet" :rules="rules" label-width="120px">
      <el-form-item label="医院名称" prop="hosname">
        <el-input v-model.trim="hospitalSet.hosname" maxlength="100" />
      </el-form-item>
      <el-form-item label="医院编号" prop="hoscode">
        <el-input v-model.trim="hospitalSet.hoscode" :disabled="isEdit" maxlength="30" />
      </el-form-item>
      <el-form-item label="API 地址" prop="apiUrl">
        <el-input v-model.trim="hospitalSet.apiUrl" placeholder="例如：http://localhost:9998" />
      </el-form-item>
      <el-form-item label="联系人" prop="contactsName">
        <el-input v-model.trim="hospitalSet.contactsName" maxlength="50" />
      </el-form-item>
      <el-form-item label="联系电话" prop="contactsPhone">
        <el-input v-model.trim="hospitalSet.contactsPhone" maxlength="30" />
      </el-form-item>
      <el-form-item>
        <el-button :loading="saving" type="primary" @click="saveOrUpdate">保存</el-button>
        <el-button @click="$router.push('/yygh/hospset/list')">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script>
import hospitalSetApi from '@/api/yygh/hospset'

export default {
  name: 'HospitalSetForm',
  data() {
    return {
      hospitalSet: {},
      saving: false,
      rules: {
        hosname: [{ required: true, message: '请输入医院名称', trigger: 'blur' }],
        hoscode: [{ required: true, message: '请输入医院编号', trigger: 'blur' }],
        apiUrl: [
          { required: true, message: '请输入医院 API 地址', trigger: 'blur' },
          { type: 'url', message: '请输入完整的 HTTP 或 HTTPS 地址', trigger: 'blur' }
        ],
        contactsName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactsPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }]
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
      hospitalSetApi.getHospSetById(this.$route.params.id).then(response => {
        this.hospitalSet = response.data.hospitalSet || {}
      })
    }
  },
  methods: {
    saveOrUpdate() {
      this.$refs.hospitalSetForm.validate(valid => {
        if (!valid) {
          return
        }
        this.saving = true
        const request = this.isEdit
          ? hospitalSetApi.updateHospSet(this.hospitalSet)
          : hospitalSetApi.addHospSet(this.hospitalSet)
        request.then(() => {
          this.$message.success(this.isEdit ? '修改成功' : '新增成功')
          this.$router.push('/yygh/hospset/list')
        }).finally(() => {
          this.saving = false
        })
      })
    }
  }
}
</script>

<style scoped>
.bootstrap-alert {
  max-width: 720px;
  margin: 16px 0 24px;
}

.el-form {
  max-width: 720px;
}
</style>
