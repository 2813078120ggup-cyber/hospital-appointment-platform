<template>
    <div class="app-container">
        <!--查询表单-->
        <el-form  :inline="true" class="demo-form-inline">
            <el-form-item>
                <el-input v-model="searchObj.keyword" placeholder="姓名/手机"/>
            </el-form-item>
            <el-form-item>
                <el-select v-model="searchObj.status" clearable placeholder="用户状态">
                    <el-option label="正常" :value="1"/>
                    <el-option label="锁定" :value="0"/>
                </el-select>
            </el-form-item>
            <el-form-item  label="创建时间">
                <el-date-picker
                v-model="searchObj.createTimeBegin"
                type="datetime"
                placeholder="选择开始时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                default-time="00:00:00"
                />
            </el-form-item>
            至
            <el-form-item>
                <el-date-picker
                v-model="searchObj.createTimeEnd"
                type="datetime"
                placeholder="选择截止时间"
                value-format="yyyy-MM-dd HH:mm:ss"
                default-time="00:00:00"
                />
            </el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="fetchData()">查询</el-button>
            <el-button type="default" @click="resetData()">清空</el-button>
        </el-form>
        <!-- 列表 -->
        <el-table
        v-loading="listLoading"
        :data="list"
        stripe
            style="width: 100%">
            <el-table-column
            label="序号"
            width="70"
            align="center">
                <template slot-scope="scope">
                        {{ (page - 1) * limit + scope.$index + 1 }}
                </template>
            </el-table-column>
            <el-table-column prop="phone" label="手机号"/>
            <el-table-column prop="nickName" label="昵称"/>
            <el-table-column prop="name" label="姓名"/>
            <el-table-column label="状态" width="90" align="center">
                <template slot-scope="scope">
                    <el-tag :type="Number(scope.row.status) === 1 ? 'success' : 'danger'">
                        {{ scope.row.param.statusString }}
                    </el-tag>
                </template>
            </el-table-column>
            <el-table-column label="认证状态" prop="param.authStatusString"/>
            <el-table-column prop="createTime" label="创建时间"/>
            <el-table-column label="操作" width="180" align="center">
            <template slot-scope="scope">
                <router-link :to="'/user/userInfo/show/'+scope.row.id">
                    <el-button type="primary" size="mini">查看</el-button>
                </router-link>
                <el-button
                    v-if="Number(scope.row.status) === 1"
                    type="danger"
                    size="mini"
                    @click="updateStatus(scope.row, 0)"
                >锁定</el-button>
                <el-button
                    v-else
                    type="success"
                    size="mini"
                    @click="updateStatus(scope.row, 1)"
                >解锁</el-button>
            </template>
            </el-table-column>
        </el-table>
        <!-- 分页组件 -->
        <el-pagination
        :current-page="page"
        :total="total"
        :page-size="limit"
        :page-sizes="[5, 10, 20, 30, 40, 50, 100]"
        style="padding: 30px 0; text-align: center;"
        layout="sizes, prev, pager, next, jumper, ->, total, slot"
        @current-change="fetchData"
        @size-change="changeSize"
        />
    </div>
</template>
<script>
import userInfoApi from '@/api/yygh/userinfo'
export default {
    // 定义数据
    data() {
        return {
            listLoading: true, // 数据是否正在加载
            list: [], // 用户列表
            total: 0, // 数据库中的总记录数
            page: 1, // 默认页码
            limit: 10, // 每页记录数
            searchObj: {} // 查询表单对象
        }
    },
    // 当页面加载时获取数据
    created() {
        this.fetchData()
    },
    methods: {
    // 调用api层获取数据库中的数据
    fetchData(page = 1) {
        this.listLoading = true
        this.page = page
        userInfoApi.getPageList(this.page, this.limit, this.searchObj).then(response => {
            this.list = response.data.pageModel.records
            this.total = response.data.pageModel.total
        }).finally(() => {
            this.listLoading = false
        })
    },
    // 当页码发生改变的时候
    changeSize(size) {
        this.limit = size
        this.fetchData(1)
    },
    // 重置查询表单
    resetData() {
        this.searchObj = {}
        this.fetchData()
    },
    updateStatus(user, status) {
        const action = status === 0 ? '锁定' : '解锁'
        this.$confirm(`确定要${action}用户“${user.name || user.nickName || user.phone}”吗？`, '用户状态确认', {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
        }).then(() => userInfoApi.updateStatus(user.id, status)).then(() => {
            this.$message.success(`${action}成功`)
            this.fetchData(this.page)
        }).catch(() => {})
    }
  }
}
</script>
