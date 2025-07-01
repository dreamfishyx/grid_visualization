import request from '@/utils/request'

// 用户登录
export function login(data) {
  return request({
    url: '/user/login',
    method: 'post',
    // data: data
    data
  })
}

// 用户注册
export function register(data) {
  return request({
    url: '/user/register',
    method: 'post',
    data
  })
}

// 刷新token
export function tokenRefresh(data) {
  return request({
    url: '/user/refresh-token',
    method: 'post',
    data
  })
}

// 用户信息
export function info() {
  return request({
    url: '/user/info',
    method: 'get'
  })
}

// 用户登出
export function logout() {
  return request({
    url: '/user/logout',
    method: 'get'
  })
} 

// 发送验证码到用户邮箱(注册)
export function sendVerifyCode(email) {
  return request({
    url: `/user/send-code?email=${email}`,
    method: 'get',
    timeout: 100000
  })
}

// 发送验证码到用户邮箱(重置密码)
export function sendVerifyCodeDuringResetPassword(email) {
  return request({
    url: `/user/send-reset-code`,
    method: 'get',
    timeout: 100000
  })
}

// 重置密码
export function resetPassword(data) {
  return request({
    url: '/user/reset-password',
    method: 'post',
    data
  })
} 