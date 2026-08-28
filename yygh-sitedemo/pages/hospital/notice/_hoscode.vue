<template>
  <div class="nav-container page-component">
    <div class="nav left-nav">
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/'+hoscode+'\''">预约挂号</span>
      </div>
      <div class="nav-item">
        <span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/detail/'+hoscode+'\''">医院详情</span>
      </div>
      <div class="nav-item selected">
        <span class="v-link selected dark" :onclick="'javascript:window.location=\'/hospital/notice/'+hoscode+'\''">预约须知</span>
      </div>
      <div class="nav-item"><span class="v-link clickable dark" :onclick="'javascript:window.location=\'/hospital/suspend/'+hoscode+'\''">停诊信息</span></div>
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

      <div class="hospital-notice notice-content">
        <h2>预约须知</h2>
        <div class="notice-summary">
          <div><span class="label">预约周期：</span>{{ bookingRule.cycle || '-' }}天</div>
          <div><span class="label">放号时间：</span>{{ bookingRule.releaseTime || '-' }}</div>
          <div><span class="label">停挂时间：</span>{{ bookingRule.stopTime || '-' }}</div>
          <div><span class="label">退号时间：</span>{{ quitTimeText }}</div>
        </div>

        <div class="notice-section">
          <div class="section-title"><span class="block"></span>医院预约规则</div>
          <ol v-if="bookingRule.rule && bookingRule.rule.length">
            <li v-for="item in bookingRule.rule" :key="item">{{ item }}</li>
          </ol>
          <p v-else class="empty-text">暂未发布预约规则</p>
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
      },
      bookingRule: {
        rule: []
      }
    }
  },
  computed: {
    quitTimeText() {
      if (this.bookingRule.quitDay === -1) {
        return '就诊前一工作日' + (this.bookingRule.quitTime || '') + '前取消'
      }
      if (this.bookingRule.quitDay === 0) {
        return '就诊当天' + (this.bookingRule.quitTime || '') + '前取消'
      }
      return '-'
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
        this.bookingRule = response.data.bookingRule || { rule: [] }
      })
    }
  }
}
</script>

<style>
.notice-content {
  padding: 30px 40px 60px;
}

.notice-content h2 {
  margin-bottom: 35px;
}

.notice-summary {
  display: grid;
  grid-template-columns: repeat(2, minmax(220px, 1fr));
  gap: 18px 40px;
  padding: 24px 30px;
  background: #f7faff;
  color: #666;
  line-height: 22px;
}

.notice-summary .label {
  color: #333;
  margin-right: 8px;
}

.notice-section {
  margin-top: 42px;
  color: #666;
  line-height: 26px;
}

.section-title {
  display: flex;
  align-items: center;
  color: #333;
  font-size: 16px;
  font-weight: 700;
}

.section-title .block {
  width: 4px;
  height: 16px;
  margin-right: 10px;
  background: #3f8ef7;
}

.notice-section ol {
  margin: 20px 0 0 22px;
  padding-left: 20px;
}

.notice-section li {
  padding-left: 8px;
  margin-bottom: 10px;
}

.empty-text {
  color: #999;
}

@media (max-width: 768px) {
  .notice-content {
    padding: 24px 18px 40px;
  }

  .notice-summary {
    grid-template-columns: 1fr;
    padding: 20px;
  }
}
</style>
