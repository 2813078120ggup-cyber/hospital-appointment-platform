<template>
  <!-- header -->
  <div class="nav-container page-component">
    <!--左侧导航 #start -->
    <div class="nav left-nav">
      <div class="nav-item ">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/'+hoscode+'\''">预约挂号 </span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark" :onclick="'javascript:window.location=\'/hospital/detail/'+hoscode+'\''"> 医院详情 </span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/notice/'+hoscode+'\''"> 预约须知 </span>
      </div>
      <div class="nav-item "><span
        class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/suspend/'+hoscode+'\''"> 停诊信息 </span>
      </div>
      <div class="nav-item "><span
        class="v-link clickable dark"> 查询/取消 </span>
      </div>
    </div>
    <!-- 左侧导航 #end -->
    <!-- 右侧内容 #start -->
    <div class="page-container">
      <div class="hospital-detail">
        <div class="common-header">
          <div class="title-wrapper"><span class="hospital-title">{{ hospital.hosname }}</span>
            <div class="icon-wrapper"><span class="iconfont"></span> {{ hospital.param.hostypeString }}</div>
          </div>
        </div>
        <div class="info-wrapper"><img :src="'data:image/jpeg;base64,'+hospital.logoData" :alt="hospital.hosname" style="width: 80px; height: 80px;">
          <div class="content-wrapper">
            <div class="icon-text-wrapper" v-if="hospital.param.fullAddress">
              <span class="iconfont prefix-icon"></span>
              <span class="text">地址：{{ hospital.param.fullAddress }}</span>
            </div>
            <div class="icon-text-wrapper" v-if="hospital.route">
              <span class="iconfont prefix-icon"></span>
              <span class="text">路线：{{ hospital.route }}</span>
            </div>
          </div>
        </div>
        <div class="title mt40"> 医院介绍</div>
        <div class="detail-content mt40"><p>{{ hospital.intro }}</p></div>
      </div>
    </div>
    <!-- 右侧内容 #end -->
  </div>
  <!-- footer -->
</template>
<script>
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'
import hospitalApi from '@/api/hospital'
export default {
  data() {
    return {
      hoscode: null,
      hospital: {
        param: {}
      }
    }
  },
  created() {
    this.hoscode = this.$route.params.hoscode
    this.init()
  },
  methods: {
    init() {
      hospitalApi.show(this.hoscode).then(response => {
        this.hospital = {
          ...response.data.hospital,
          param: response.data.hospital.param || {}
        }
      })
    }
  }
}
</script>
<style>
  .hospital-detail .info-wrapper {
    width: 100%;
    padding-left: 0;
    padding-top: 0;
    margin-top: 0;
    flex-direction: inherit;
  }
  .hospital-detail .info-wrapper .text {
    font-size: 14px;
  }
  .hospital-detail .content-wrapper p {
    text-indent: 0;
  }
</style>