import request from '@/utils/request'

/**
 * 获取维修人员列表
 * @param {Number} pageNum - 当前页码
 * @param {Number} pageSize - 每页条数
 * @param {String} name - 搜索名称
 * @param {Number} gender - 性别筛选（0:男，1:女）
 * @returns {Promise} - 请求Promise
 */
export function getWorkerList(pageNum = 1, pageSize = 6, name = '', gender = null) {
  console.log('请求参数:', { pageNum, pageSize, name, gender });
  return request({
    url: '/worker/list',
    method: 'get',
    params: {
      pageNum,
      pageSize,
      name,
      // 根据后端WorkerController，参数名应该是gender而不是status
      gender: gender
    }
  })
}

/**
 * 获取维修人员详情
 * @param {Number} workerId - 维修人员ID
 * @returns {Promise} - 请求Promise
 */
export function getWorkerInfo(workerId) {
  return request({
    url: '/worker/info',
    method: 'get',
    params: {
      workerId
    }
  })
}

/**
 * 删除维修人员
 * @param {Number} workerId - 维修人员ID
 * @returns {Promise} - 请求Promise
 */
export function deleteWorker(workerId) {
  return request({
    url: '/worker/delete',
    method: 'get',
    params: {
      workerId
    }
  })
}

/**
 * 添加维修人员
 * @param {Object} workerData - 维修人员数据
 * @returns {Promise} - 请求Promise
 */
export function addWorker(workerData) {
  return request({
    url: '/worker/add',
    method: 'post',
    data: workerData
  })
}

/**
 * 更新维修人员信息
 * @param {Object} workerData - 维修人员数据（必须包含workerId字段）
 * @returns {Promise} - 请求Promise
 */
export function updateWorker(workerData) {
  console.log('发送更新请求，数据:', workerData);
  return request({
    url: '/worker/update',
    method: 'post',
    data: workerData
  })
} 