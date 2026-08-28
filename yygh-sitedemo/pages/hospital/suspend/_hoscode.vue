<template>
  <div class="nav-container page-component">
    <div class="nav left-nav">
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/'+hoscode+'\''">预约挂号</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/detail/'+hoscode+'\''">医院详情</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/notice/'+hoscode+'\''">预约须知</span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark" :onclick="'javascript:window.location=\'/hospital/suspend/'+hoscode+'\''">停诊信息</span>
      </div>
      <div class="nav-item"><span class="v-link clickable dark">查询/取消</span></div>
    </div>

    <div class="page-container">
      <div class="common-header">
        <div class="title-wrapper">
          <span class="hospital-title">{{ hospital.hosname }}</span>
          <div class="icon-wrapper">
            <span class="iconfont"></span>{{ hospital.param.hostypeString }}
          </div>
        </div>
      </div>

      <div class="hospital-suspend suspend-content">
        <div class="suspend-heading">
          <div class="title">停诊信息</div>
          <span class="suspend-status">当前暂无停诊记录</span>
        </div>
        <div class="suspend-empty">
          <i class="el-icon-document-checked suspend-icon"></i>
          <div class="suspend-empty-title">暂无停诊信息</div>
          <div class="suspend-empty-text">当前医院未发布停诊通知，请以医院现场公告为准。</div>
        </div>
      </div>
    </div>
  </div>
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
        const hospital = response.data.hospital || {}
        this.hospital = {
          ...hospital,
          param: hospital.param || {}
        }
      })
    }
  }
}
</script>

<style>
.suspend-content {
  padding: 30px 40px 70px;
}

.suspend-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #edf1f7;
  padding-bottom: 20px;
}

.suspend-heading .title {
  color: #333;
  font-size: 18px;
  font-weight: 700;
}

.suspend-status {
  color: #999;
  font-size: 13px;
}

.suspend-empty {
  min-height: 280px;
  padding-top: 70px;
  text-align: center;
  color: #999;
}

.suspend-icon {
  color: #b8d5f7;
  font-size: 52px;
  margin-bottom: 20px;
}

.suspend-empty-title {
  color: #333;
  font-size: 16px;
  margin-bottom: 12px;
}

.suspend-empty-text {
  font-size: 13px;
}

@media (max-width: 768px) {
  .suspend-content {
    padding: 24px 18px 40px;
  }
}
</style>
