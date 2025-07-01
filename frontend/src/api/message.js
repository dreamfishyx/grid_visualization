import request from '@/utils/request'

/**
 * 获取未读消息列表
 * @returns {Promise} - 返回未读消息列表的Promise
 */
export function getUnreadMessages() {
  return request({
    url: '/message/list',
    method: 'get'
  })
}

/**
 * 将消息标记为已读
 * @param {Number} messageId - 消息ID
 * @returns {Promise} - 返回操作结果的Promise
 */
export function markMessageAsRead(messageId) {
  return request({
    url: '/message/read',
    method: 'get',
    params: { messageId }
  })
} 