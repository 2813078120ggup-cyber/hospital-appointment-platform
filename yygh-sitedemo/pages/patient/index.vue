<template>
    <!-- header -->
    <div class="nav-container page-component">
        <!--左侧导航 #start -->
        <div class="nav left-nav">
            <div class="nav-item ">
                <span class="v-link clickable dark" onclick="javascript:window.location='/user'">实名认证 </span>
                </div>
                <div class="nav-item ">
                <span class="v-link clickable dark" onclick="javascript:window.location='/order'"> 挂号订单 </span>
                </div>
                <div class="nav-item selected">
                <span class="v-link selected dark" onclick="javascript:window.location='/patient'"> 就诊人管理 </span>
                </div>
                <div class="nav-item ">
                <span class="v-link clickable dark" onclick="javascript:window.location='/user/feedback'"> 意见反馈 </span>
            </div>
        </div>
        <!-- 左侧导航 #end -->
        <!-- 右侧内容 #start -->
        <div class="page-container">
        <div class="personal-patient">
          <div class="header-wrapper">
            <div class="title"> 就诊人管理</div>
          </div>

          <div v-if="!isLoggedIn" class="patient-state">
            <i class="el-icon-user-solid patient-state-icon"></i>
            <div class="patient-state-title">登录后管理就诊人</div>
            <div class="patient-state-description">登录后即可查看和维护当前账号下的就诊人</div>
            <div class="v-button patient-state-button" @click="openLogin">立即登录</div>
          </div>

          <div v-else-if="loading" class="patient-state">
            <i class="el-icon-loading patient-state-icon is-loading"></i>
            <div class="patient-state-title">正在加载就诊人</div>
            <div class="patient-state-description">请稍候，正在获取当前账号的就诊人信息</div>
          </div>

          <div v-else-if="errorMessage" class="patient-state patient-state-error">
            <i class="el-icon-warning-outline patient-state-icon"></i>
            <div class="patient-state-title">就诊人加载失败</div>
            <div class="patient-state-description">{{ errorMessage }}</div>
            <div class="v-button patient-state-button" @click="findPatientList">重新加载</div>
          </div>

          <div v-else class="content-wrapper">
          <div v-if="patientList.length === 0" class="patient-state patient-empty-state">
            <i class="el-icon-user patient-state-icon"></i>
            <div class="patient-state-title">暂无就诊人</div>
            <div class="patient-state-description">添加就诊人后即可预约挂号</div>
          </div>
          <el-card class="patient-card" shadow="always" v-for="item in patientList" :key="item.id">
            <div slot="header" class="clearfix">
                <div>
                    <span class="name">{{ item.name }}</span>
                    <span>{{ item.certificatesNo }} {{ certificatesTypeText(item) }}</span>
                    <div  class="detail" @click="show(item.id)"> 查看详情 <span  class="iconfont"></span></div>
                </div>
            </div>
            <div class="card SELF_PAY_CARD">
                <div class="info">
                    <span class="type">{{ item.isInsure == 0 ? '自费' : '医保'}}</span>
                    <span class="card-no">{{ item.certificatesNo }}</span>
                    <span class="card-view">{{ certificatesTypeText(item) }}</span>
                </div>
                <span class="operate"></span>
            </div>
            <div class="card">
                <div class="text bind-card"></div>
            </div>
        </el-card>
        <div class="item-add-wrapper v-card clickable" @click="add()">
            <div class="">
                <div>+ 添加就诊人</div>
            </div>
        </div>
          </div>
        </div>
    </div>
    <!-- 右侧内容 #end -->
    </div>
    <!-- footer -->
</template>
<script>
import cookie from 'js-cookie'
import '~/assets/css/hospital_personal.css'
import '~/assets/css/hospital.css'
import '~/assets/css/personal.css'
import patientApi from '@/api/patient'
export default {
    data() {
        return {
            isLoggedIn: false,
            loading: false,
            errorMessage: '',
            patientList: []
        }
    },
    mounted() {
        this.isLoggedIn = !!cookie.get('token')
        if (this.isLoggedIn) {
            this.findPatientList()
        }
    },
    methods: {
        findPatientList() {
            if (!this.isLoggedIn) return
            this.loading = true
            this.errorMessage = ''
            patientApi.findList().then(response => {
                this.patientList = (response && response.data && response.data.list) || []
            }).catch(error => {
                this.patientList = []
                this.errorMessage = (error && error.message) || '暂时无法获取就诊人，请稍后重试'
            }).finally(() => {
                this.loading = false
            })
        },
        openLogin() {
            if (typeof window !== 'undefined' && window.loginEvent) {
                window.loginEvent.$emit('loginDialogEvent')
            } else {
                window.location.href = '/'
            }
        },
        certificatesTypeText(item) {
            return (item && item.param && item.param.certificatesTypeString) || ''
        },
        add() {
            window.location.href = '/patient/add'
        },
        show(id) {
            window.location.href = '/patient/show?id=' + id
            }
        }
    }
</script>
<style>
  .header-wrapper .title {
    font-size: 16px;
    margin-top: 0;
  }
  .content-wrapper {
    margin-left: 0;
  }
  .patient-state {
    min-height: 340px;
    padding-top: 70px;
    text-align: center;
    color: #999;
  }
  .patient-empty-state {
    min-height: 220px;
    padding-top: 35px;
  }
  .patient-state-icon {
    display: block;
    color: #b8d5f7;
    font-size: 48px;
    margin-bottom: 20px;
  }
  .patient-state-icon.is-loading {
    color: #4990f1;
  }
  .patient-state-title {
    color: #333;
    font-size: 16px;
    line-height: 24px;
  }
  .patient-state-description {
    margin-top: 8px;
    color: #999;
    font-size: 13px;
  }
  .patient-state-button {
    width: 120px;
    height: 36px;
    margin: 24px auto 0;
    font-size: 13px;
  }
  .patient-state-error .patient-state-icon {
    color: #f2b35b;
  }
  .patient-card .el-card__header .detail{
    font-size: 14px;
  }
</style>
