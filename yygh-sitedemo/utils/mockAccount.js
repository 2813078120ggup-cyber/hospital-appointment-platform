import cookie from 'js-cookie'

export const MOCK_LOGIN_TOKEN_PREFIX = 'mock-token-'

export const mockUserInfo = {
  id: 26,
  phone: '1350000000',
  name: '张老三',
  certificatesType: '身份证',
  certificatesNo: '220721200007205001',
  certificatesUrl: 'http://192.168.6.100:9000/yygh/2021/08/01/p1.jpg',
  authStatus: 2,
  status: 1,
  param: {
    authStatusString: '认证成功',
    statusString: '正常'
  }
}

export function isMockLogin() {
  return (cookie.get('token') || '').startsWith(MOCK_LOGIN_TOKEN_PREFIX)
}
