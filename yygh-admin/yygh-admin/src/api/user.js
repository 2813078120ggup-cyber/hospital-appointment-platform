import request from '@/utils/request'

export function login(data) {
  return request({
    //url: '/admin/hosp/hospitalSet/login',
    url: '/user/hosp/login',
    method: 'post',
    data
  })
}

export function getInfo(token) {
  return request({
    //url: '/admin/hosp/hospitalSet/info',
    url: '/user/hosp/info',
    method: 'get',
    params: { token }
  })
}

export function logout() {
  return request({
    url: '/vue-admin-template/user/logout',
    method: 'post'
  })
}
