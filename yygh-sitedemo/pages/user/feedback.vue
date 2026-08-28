<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item">
        <span class="v-link clickable dark" onclick="javascript:window.location='/user'">实名认证 </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" onclick="javascript:window.location='/order'"> 挂号订单 </span>
      </div>
      <div class="nav-item ">
        <span class="v-link clickable dark" onclick="javascript:window.location='/patient'"> 就诊人管理 </span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark"> 意见反馈 </span>
      </div>
    </div>
    <!-- 左侧导航 #end -->
    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div>
        <div class="title"> 意见反馈</div>
        <div class="tips"><span class="iconfont"></span>
          您的意见与建议将帮助我们不断改进服务。平台反馈由平台管理员接收，医院反馈由对应医院接收。
        </div>
        <div class="form-wrapper">
          <el-form :model="formData" label-width="110px" label-position="left">
            <el-form-item prop="type" label="反馈类型：" class="form-normal">
              <el-radio-group v-model="formData.type">
                <el-radio :label="1">平台反馈</el-radio>
                <el-radio :label="2">医院反馈</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item v-if="formData.type == 2" prop="hospital" label="选择医院：" class="form-normal">
              <el-select
                v-model="selectedHospital"
                filterable
                remote
                :remote-method="searchHospital"
                :loading="hospitalLoading"
                placeholder="请输入医院名称搜索"
                class="input v-input"
                style="width: 100%">
                <el-option
                  v-for="item in hospitalList"
                  :key="item.hoscode"
                  :label="item.hosname"
                  :value="item">
                </el-option>
              </el-select>
            </el-form-item>
            <el-form-item prop="phone" label="联系电话：" class="form-normal">
              <el-input v-model="formData.phone" placeholder="请填写联系电话（选填）" class="input v-input"/>
            </el-form-item>
            <el-form-item prop="content" label="反馈内容：" class="form-normal">
              <el-input
                type="textarea"
                :rows="6"
                maxlength="500"
                show-word-limit
                v-model="formData.content"
                placeholder="请描述您的意见或建议（必填，最多500字）"
                class="input v-input"/>
            </el-form-item>
          </el-form>
          <div class="bottom-wrapper">
            <div class="button-wrapper">
              <div class="v-button" @click="save()">{{ submitBnt }}</div>
            </div>
          </div>
        </div>
      </div>
    </div><!-- 右侧内容 #end -->
  </div>
  <!-- footer -->
</template>
<script>
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'
import '~/assets/css/personal.css'
import feedbackApi from '@/api/feedback'
import hospitalApi from '@/api/hospital'
import userInfoApi from '@/api/userInfo'
import { isMockLogin, mockUserInfo } from '@/utils/mockAccount'
export default {
  data() {
    return {
      formData: {
        type: 1,
        phone: '',
        content: ''
      },
      userInfo: {
        param: {}
      },
      selectedHospital: null,
      hospitalList: [],
      hospitalLoading: false,
      submitBnt: '提交'
    }
  },
  created() {
    this.getUserInfo()
  },
  methods: {
    getUserInfo() {
      if (isMockLogin()) {
        this.userInfo = mockUserInfo
        return
      }
      userInfoApi.getUserInfo().then(response => {
        this.userInfo = response.data.userInfo
      })
    },
    searchHospital(query) {
      if (!query) {
        this.hospitalList = []
        return
      }
      this.hospitalLoading = true
      hospitalApi.getByHosname(query).then(response => {
        this.hospitalList = response.data.list || []
      }).finally(() => {
        this.hospitalLoading = false
      })
    },
    save() {
      if (this.submitBnt == '正在提交...') {
        this.$message.info('重复提交')
        return
      }
      if (!this.formData.content) {
        this.$message.error('请填写反馈内容')
        return
      }
      if (this.formData.type == 2 && !this.selectedHospital) {
        this.$message.error('请选择医院')
        return
      }
      const feedback = {
        type: this.formData.type,
        phone: this.formData.phone,
        content: this.formData.content
      }
      const displayName = this.userInfo.name || this.userInfo.nickName || ''
      if (displayName) {
        feedback.userName = displayName
      }
      if (this.formData.type == 2) {
        feedback.hoscode = this.selectedHospital.hoscode
        feedback.hosname = this.selectedHospital.hosname
      }
      this.submitBnt = '正在提交...'
      feedbackApi.save(feedback).then(response => {
        this.$message.success("反馈提交成功，感谢您的建议")
        this.formData.content = ''
        this.formData.phone = ''
        this.selectedHospital = null
        this.submitBnt = '提交'
      }).catch(e => {
        this.submitBnt = '提交'
      })
    }
  }
}
</script>
<style>
  .header-wrapper .title {
    font-size: 16px;
    margin-top: 0;
  }
  .page-container .title {
    letter-spacing: 1px;
    font-weight: 700;
    color: #333;
    font-size: 16px;
    margin-top: 0;
    margin-bottom: 20px;
  }
  .page-container .tips {
    width: 100%;
    padding-left: 0;
    margin-bottom: 20px;
  }
  .page-container .form-wrapper {
    padding-left: 92px;
    width: 580px;
  }
  .form-normal {
    height: auto;
    margin-bottom: 20px;
  }
  .bottom-wrapper{
    width: 100%;
    padding: 0;
    margin-top: 20px;
  }
</style>
