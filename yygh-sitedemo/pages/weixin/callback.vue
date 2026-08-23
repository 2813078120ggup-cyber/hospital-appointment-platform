<template>
  <!-- header -->
  <div>
  </div>
  <!-- footer -->
</template>
<script>
export default {
  layout: "empty",
  data() {
    return {
    }
  },
  mounted() {
    //  /hosp/list/10000     this.$route.params.id

    //  /weixin/callback?name=lucy&token=123&openid=123
    //    this.$route.query.name
    //    this.$route.query.token 
    //    this.$route.query.openid 

    let token = this.$route.query.token
    let name = this.$route.query.name
    let openid = this.$route.query.openid
    // 调用父vue方法
    window.parent['loginCallback'](name, token, openid)
  }
}
</script>