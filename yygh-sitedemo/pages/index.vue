<template>
  <div class="home page-component">
    <el-carousel indicator-position="outside">
      <el-carousel-item v-for="item in 2" :key="item">
        <img src="~assets/images/web-banner1.png" alt="">
      </el-carousel-item>
    </el-carousel>
    <!-- 搜索 -->
    <div class="search-container">
    <div class="search-wrapper">
    <div class="hospital-search">
      <el-autocomplete
      class="search-input"
      prefix-icon="el-icon-search"
      v-model="hosname"
      :fetch-suggestions="querySearchAsync"
      placeholder="点击输入医院名称"
      @select="handleSelect"
      >
        <span slot="suffix" class="search-btn v-link highlight clickable selected">搜索 </span>
      </el-autocomplete>
    </div>
    </div>
    </div>
    <!-- bottom -->
    <div class="bottom">
    <div class="left">
    <div class="home-filter-wrapper">
    <div class="title"> 医院</div>
    <div>
      <div class="filter-wrapper">
        <span
        class="label">等级：</span>
        <div class="condition-wrapper">
          <span class="item v-link clickable" 
            :class="hostypeActiveIndex == index ? 'selected' : ''"
             v-for="(item,index) in hostypeList" :key="item.id" 
             @click="hostypeSelect(item.value, index)">{{ item.name }}</span>
       </div>
      </div>
    <div class="filter-wrapper">
      <span
      class="label">地区：</span>
      <div class="condition-wrapper">
        <span class="item v-link clickable"
          :class="provinceActiveIndex == index ? 'selected' : ''"
          v-for="(item,index) in districtList" :key="item.id"
          @click="districtSelect(item.value, index)">{{ item.name }}</span>
      </div>
      </div>
    </div>
    </div>
    <div class="v-scroll-list hospital-list">
      <div class="v-card clickable list-item" v-for="item in list" :key="item.id">
        <div class="">
          <div class="hospital-list-item hos-item" index="0" @click="show(item.hoscode)">
            <div class="wrapper">
            <div class="hospital-title"> {{ item.hosname }}</div>
            <div class="bottom-container">
            <div class="icon-wrapper">
              <span class="iconfont"></span>{{ item.param.hostypeString }}
            </div>
          <div class="icon-wrapper">
          <span class="iconfont"></span>每天{{ item.bookingRule.releaseTime }}放号
          </div>
          </div>
          </div>
          <img :src="'data:image/jpeg;base64,'+item.logoData"
           :alt="item.hosname"
           class="hospital-img">
      </div>
    </div>
    </div>
    </div>
    </div>
    <div class="right">
      <div class="common-dept">
      <div class="header-wrapper">
      <div class="title"> 常见科室</div>
      <div class="all-wrapper"><span>全部</span>
      <span class="iconfont icon"></span>
      </div>
      </div>
      <div class="content-wrapper">
      <span class="item v-link clickable dark" v-for="department in commonDepartments" :key="department" @click="selectCommonDepartment(department)">{{ department }}</span>
      </div>
    </div>
    <div class="space">
      <div class="header-wrapper">
      <div class="title-wrapper">
      <div class="icon-wrapper"><span
      class="iconfont title-icon"></span>
      </div>
      <span class="title">平台公告</span>
      </div>
      <div class="all-wrapper" @click="showNoticeList('平台公告')">
      <span>全部</span>
      <span class="iconfont icon"></span>
      </div>
      </div>
      <div class="content-wrapper">
      <div v-for="notice in platformNotices.slice(0, 3)" :key="notice.id" class="notice-wrapper">
        <div class="point"></div>
        <span class="notice v-link clickable dark" @click="showNotice(notice, '平台公告')">{{ notice.title }}</span>
      </div>
      <div v-if="platformNotices.length === 0" class="notice-empty">暂无平台公告</div>
      </div>
    </div>
    <div class="suspend-notice-list space">
    <div class="header-wrapper">
    <div class="title-wrapper">
      <div class="icon-wrapper">
      <span class="iconfont title-icon"></span>
      </div>
      <span class="title">停诊公告</span>
      </div>
      <div class="all-wrapper" @click="showNoticeList('停诊公告')">
      <span>全部</span>
      <span class="iconfont icon"></span>
      </div>
      </div>
      <div class="content-wrapper">
      <div v-for="notice in suspendNotices.slice(0, 3)" :key="notice.id" class="notice-wrapper">
        <div class="point"></div>
        <span class="notice v-link clickable dark" @click="showNotice(notice, '停诊公告')">{{ notice.title }}</span>
      </div>
      <div v-if="suspendNotices.length === 0" class="notice-empty">暂无停诊公告</div>
    </div>
    </div>
    </div>
    </div>
    <el-dialog :title="selectedNotice.type" :visible.sync="noticeDialogVisible" width="520px">
    <div v-if="selectedNotice.title" class="notice-dialog-content">
      <div class="notice-dialog-title">{{ selectedNotice.title }}</div>
      <p>{{ selectedNotice.content }}</p>
      <div v-if="selectedNotice.publishTime" class="notice-dialog-time">发布时间：{{ selectedNotice.publishTime }}</div>
    </div>
    </el-dialog>
    <el-dialog :title="noticeListType" :visible.sync="noticeListDialogVisible" width="560px">
      <div v-if="selectedNoticeList.length" class="notice-dialog-list">
        <button
          v-for="notice in selectedNoticeList"
          :key="notice.id"
          type="button"
          class="notice-list-item"
          @click="openNoticeFromList(notice)"
        >
          <span>{{ notice.title }}</span>
          <small>{{ notice.publishTime || '' }}</small>
        </button>
      </div>
      <div v-else class="notice-empty">暂无公告</div>
    </el-dialog>
  </div>
</template>
<script>
import hospApi from '@/api/hospital.js'
import dictApi from '@/api/dict.js'
import noticeApi from '@/api/notice.js'
export default {
  //服务端渲染异步，显示医院列表
  asyncData({ params, error }) {
    //调用
    return Promise.all([
      hospApi.getPageList(1,10,null),
      noticeApi.getPublishedList(1).catch(() => null),
      noticeApi.getPublishedList(2).catch(() => null)
    ]).then(([hospitalResponse, platformResponse, suspendResponse]) => {
        return {
          list: hospitalResponse.data.pages.content,
          pages: hospitalResponse.data.pages.totalPages,
          platformNotices: platformResponse ? platformResponse.data.list || [] : [],
          suspendNotices: suspendResponse ? suspendResponse.data.list || [] : []
        }
      })
  },
  data() {
    return {
      searchObj: {},
      page: 1,
      limit: 10,
      hosname: '', //医院名称
      hostypeList: [{ id: 'all-hostype', name: '全部', value: '' }], //医院等级集合
      districtList: [{ id: 'all-district', name: '全部', value: '' }], //地区集合
      hostypeActiveIndex: 0,
      provinceActiveIndex: 0
      ,commonDepartments: ['神经内科', '消化内科', '呼吸内科', '内科', '神经外科', '妇科', '产科', '儿科']
      ,platformNotices: []
      ,suspendNotices: []
      ,noticeDialogVisible: false
      ,noticeListDialogVisible: false
      ,noticeListType: ''
      ,selectedNoticeList: []
      ,selectedNotice: {
        type: '',
        title: '',
        content: '',
        publishTime: ''
      }
    }
  },
  created() {
    this.init()
  },
  methods:{
    //查询医院等级列表 和 所有地区列表
    init() {
      //查询医院等级列表
      dictApi.findByDictCode('Hostype')
        .then(response => {
          //hostypeList清空
          this.hostypeList = []
          //向hostypeList添加全部值
          this.hostypeList.push({ id: 'all-hostype', name: '全部', value: '' })
          //把接口返回数据，添加到hostypeList
          const list = response.data.list || []
          for(var i=0;i<list.length;i++) {
              this.hostypeList.push(list[i])
          }
      })
      //查询地区数据
      dictApi.findByDictCode('Beijin')
        .then(response => {
          this.districtList = []
          this.districtList.push({ id: 'all-district', name: '全部', value: '' })
          const list = response.data.list || []
          for(let i in list) {
            this.districtList.push(list[i])
          }
        })
    },
    //查询医院列表
    getList() {
      hospApi.getPageList(this.page,this.limit,this.searchObj)
        .then(response => {
          for(let i in response.data.pages.content) {
            this.list.push(response.data.pages.content[i])
          }
          this.page = response.data.pages.totalPages
        })
    },
    //根据医院等级查询
    hostypeSelect(hostype,index) {
      //准备数据
      this.list = []
      this.page = 1
      this.hostypeActiveIndex = index
      this.searchObj.hostype = hostype
      //调用查询医院列表方法
      this.getList()
    },
    //根据地区查询医院
    districtSelect(districtCode, index) {
      this.list = []
      this.page = 1
      this.provinceActiveIndex = index
      this.searchObj.districtCode = districtCode
      this.getList();
    },
    //在输入框输入值，弹出下拉框，显示相关内容 callback
    querySearchAsync(queryString, cb) {
      this.searchObj = []
      if(queryString == '') return
      hospApi.getByHosname(queryString).then(response => {
        for (let i = 0, len = response.data.list.length; i <len; i++) {
          response.data.list[i].value = response.data.list[i].hosname
        }
        cb(response.data.list)
      })
    },
    //在下拉框选择某一个内容，执行下面方法，跳转到详情页面中
    handleSelect(item) {
      window.location.href = '/hospital/' + item.hoscode
    },
    //点击某个医院名称，跳转到详情页面中
    show(hoscode) {
      window.location.href = '/hospital/' + hoscode
    },
    selectCommonDepartment(department) {
      this.$message.info('请先选择医院，再进入' + department + '预约挂号')
    },
    showNotice(notice, type) {
      this.selectedNotice = {
        type: type,
        title: notice.title,
        content: notice.content || notice.summary || '',
        publishTime: notice.publishTime || ''
      }
      this.noticeDialogVisible = true
    },
    showNoticeList(type) {
      this.noticeListType = type
      this.selectedNoticeList = type === '停诊公告' ? this.suspendNotices : this.platformNotices
      this.noticeListDialogVisible = true
    },
    openNoticeFromList(notice) {
      this.noticeListDialogVisible = false
      this.showNotice(notice, this.noticeListType)
    }
  }
}
</script>

<style scoped>
.notice-empty {
  padding: 8px 0;
  color: #999;
  font-size: 13px;
}

.notice-dialog-content p {
  line-height: 1.8;
  white-space: pre-line;
}

.notice-dialog-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 600;
}

.notice-dialog-time {
  margin-top: 20px;
  color: #999;
  font-size: 12px;
  text-align: right;
}

.notice-list-item {
  display: flex;
  width: 100%;
  min-height: 44px;
  padding: 12px 0;
  color: #333;
  background: transparent;
  border: 0;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  align-items: center;
  justify-content: space-between;
  text-align: left;
}

.notice-list-item:hover,
.notice-list-item:focus {
  color: #4490f1;
}

.notice-list-item small {
  margin-left: 16px;
  color: #999;
  white-space: nowrap;
}
</style>
