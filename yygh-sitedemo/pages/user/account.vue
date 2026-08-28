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
        <span class="v-link selected dark"> 修改账号信息 </span>
      </div>
      <div class="nav-item ">
        <span class="v-link clickable dark" onclick="javascript:window.location='/user/feedback'"> 意见反馈 </span>
      </div>
    </div>
    <!-- 左侧导航 #end -->
    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div>
        <div class="title"> 修改账号信息</div>
        <div class="tips"><span class="iconfont"></span>
          手机号是登录账号，暂不支持在线修改；如需更换手机号请联系平台客服。
        </div>
        <div class="form-wrapper">
          <el-form :model="formData" label-width="110px" label-position="left">
            <el-form-item prop="phone" label="手机号：" class="form-normal">
              <div class="name-input">{{ userInfo.phone }}</div>
            </el-form-item>
            <el-form-item prop="nickName" label="昵称：" class="form-normal">
              <el-input v-model="formData.nickName" placeholder="请输入昵称" class="input v-input"/>
            </el-form-item>
            <el-form-item prop="name" label="姓名：" class="form-normal">
              <el-input v-model="formData.name" placeholder="请输入姓名" class="input v-input"/>
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
import userInfoApi from '@/api/userInfo'
import { isMockLogin, mockUserInfo } from '@/utils/mockAccount'
export default {
  asyncData({ redirect }) {
    // 账号修改功能暂时隐藏，保留页面代码便于后续恢复。
    redirect('/user')
  },
  data() {
    return {
      formData: {
        name: '',
        nickName: ''
      },
      userInfo: {
        param: {}
      },
      submitBnt: '保存'
    }
  },
  created() {
    this.getUserInfo()
  },
  methods: {
    getUserInfo() {
      if (isMockLogin()) {
        this.userInfo = mockUserInfo
        this.formData.name = mockUserInfo.name || ''
        return
      }
      userInfoApi.getUserInfo().then(response => {
        this.userInfo = response.data.userInfo
        this.formData.name = this.userInfo.name || ''
        this.formData.nickName = this.userInfo.nickName || ''
      })
    },
    save() {
      if (this.submitBnt == '正在提交...') {
        this.$message.info('重复提交')
        return
      }
      if (!this.formData.name && !this.formData.nickName) {
        this.$message.error('请填写要修改的内容')
        return
      }
      this.submitBnt = '正在提交...'
      userInfoApi.updateUserInfo(this.formData).then(response => {
        this.$message.success("保存成功")
        window.location.reload()
      }).catch(e => {
        this.submitBnt = '保存'
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
    height: 40px;
  }
  .name-input {
    line-height: 40px;
  }
  .bottom-wrapper{
    width: 100%;
    padding: 0;
    margin-top: 20px;
  }
</style>
