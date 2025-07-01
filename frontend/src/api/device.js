import request from '@/utils/request'

/**
 * 获取所有设备列表（不分页）
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function getAllDevices(params) {
  console.log('API调用 getAllDevices，参数:', params);
  return request({
    url: '/device/list',
    method: 'get',
    params
  }).then(response => {
    console.log('API返回数据:', response);
    return response;
  }).catch(error => {
    console.error('API调用错误:', error);
    throw error;
  })
}

/**
 * 获取设备分页列表
 * @param {Object} params - 分页和查询参数
 * @returns {Promise}
 */
export function getDevicesByPage(params) {
  return request({
    url: '/device/list-page',
    method: 'get',
    params
  })
}

/**
 * 获取设备详情
 * @param {Number} deviceId - 设备ID
 * @returns {Promise}
 */
export function getDeviceInfo(deviceId) {
  return request({
    url: '/device/info',
    method: 'get',
    params: { deviceId }
  })
}

/**
 * 添加设备
 * @param {Object} data - 设备数据
 * @returns {Promise}
 */
export function addDevice(data) {
  return request({
    url: '/device/add',
    method: 'post',
    data
  })
}

/**
 * 删除设备
 * @param {Number} deviceId - 设备ID
 * @returns {Promise}
 */
export function deleteDevice(deviceId) {
  return request({
    url: '/device/delete',
    method: 'get',
    params: { deviceId }
  })
}

/**
 * 获取设备历史数据
 * @param {Number} deviceId - 设备ID
 * @param {String} mod - 查询模式：hour/half-day
 * @returns {Promise}
 */
export function getDeviceData(deviceId, mod = 'hour') {
  return request({
    url: '/device/query-data',
    method: 'get',
    params: { deviceId, mod }
  })
} 