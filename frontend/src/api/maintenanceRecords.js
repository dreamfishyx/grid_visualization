import request from '@/utils/request'

/**
 * 获取维修记录列表
 * @param {Number} pageNum - 页码
 * @param {Number} pageSize - 每页条数
 * @param {String} search - 搜索关键词
 * @param {Number} status - 维修状态
 * @param {String} startTime - 开始时间
 * @param {String} endTime - 结束时间
 * @returns {Promise}
 */
export function getMaintenanceRecords(pageNum = 1, pageSize = 6, search = '', status = null, startTime = null, endTime = null) {
  console.log('维修记录请求参数:', { pageNum, pageSize, search, status, startTime, endTime });
  return request({
    url: '/maintenanceRecords/list',
    method: 'get',
    params: {
      pageNum,
      pageSize,
      search: search || undefined,
      status,
      startTime,
      endTime
    }
  })
}

/**
 * 根据ID获取维修记录详情
 * @param {Number} id - 维修记录ID
 * @returns {Promise} - 返回包含维修记录详情的Promise
 */
export function getMaintenanceRecordById(id) {
  console.log('请求维修记录详情:', id);
  return request({
    url: '/maintenanceRecords/detail',
    method: 'get',
    params: { id }
  })
}

/**
 * 上传/更新维修记录信息
 * @param {Object} data - 维修记录数据
 * @param {Number} data.id - 维修记录ID
 * @param {String} data.description - 故障描述
 * @param {String} data.process - 维修过程
 * @returns {Promise} - 返回包含上传结果的Promise
 */
export function uploadMaintenanceRecord(data) {
  console.log('上传维修记录:', data);
  return request({
    url: '/maintenanceRecords/push',
    method: 'post',
    data
  })
}
