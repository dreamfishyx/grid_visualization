<template>
  <dashboard-layout>
    <!-- 搜索和筛选区域 -->
    <div class="filter-container">
      <el-input v-model="searchQuery" placeholder="搜索设备ID" class="search-input" clearable prefix-icon="Search" />
      <el-select v-model="statusFilter" placeholder="设备状态" class="filter-select">
        <el-option label="全部" :value="null" />
        <el-option label="在线" :value="0" />
        <el-option label="离线" :value="1" />
        <el-option label="异常" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch" class="action-btn search-btn">
        <el-icon>
          <Search />
        </el-icon>
        查询
      </el-button>
      <el-button type="success" @click="showAddDeviceDialog" class="action-btn add-btn">
        <el-icon>
          <Plus />
        </el-icon>
        添加设备
      </el-button>
    </div>

    <!-- 设备表格 -->
    <el-card class="device-table-card">
      <div class="table-header">
        <h3 class="table-title">设备信息</h3>
      </div>
      <el-table :data="devices" style="width: 100%" border stripe v-loading="loading" :cell-style="{ fontSize: '14px' }"
        :header-cell-style="{ backgroundColor: '#f1f5f9', color: '#334155', fontWeight: 600 }">
        <el-table-column prop="deviceId" label="设备ID" min-width="150" />
        <el-table-column prop="location" label="安装位置" min-width="200">
          <template #default="scope">
            {{ scope.row.fullAddress || scope.row.location || '未知位置' }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <div class="status-tag" :class="getStatusClass(scope.row.status)">
              <el-icon :size="16" class="status-icon" v-if="getStatusCode(scope.row.status) === 0">
                <CircleCheck />
              </el-icon>
              <el-icon :size="16" class="status-icon" v-else-if="getStatusCode(scope.row.status) === 1">
                <CircleClose />
              </el-icon>
              <el-icon :size="16" class="status-icon" v-else-if="getStatusCode(scope.row.status) === 2">
                <Warning />
              </el-icon>
              <el-icon :size="16" class="status-icon" v-else>
                <Tools />
              </el-icon>
              {{ getStatusText(scope.row.status) }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <div class="action-buttons">
              <el-button @click="showDeviceStats(scope.row)" class="action-btn data-btn" size="small">
                <el-icon>
                  <DataLine />
                </el-icon>
                数据
              </el-button>
              <el-button @click="deleteDevice(scope.row.deviceId)" class="action-btn delete-btn" size="small">
                <el-icon>
                  <Delete />
                </el-icon>
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :page-sizes="[3, 5, 10, 15]"
          layout="total, sizes, prev, pager, next, jumper" :total="totalDevices"
          @update:current-page="handleCurrentChange" @update:page-size="handleSizeChange" background />
      </div>
    </el-card>

    <!-- 新增设备对话框 -->
    <el-dialog v-model="addDeviceDialogVisible" title="添加设备" width="550px" :append-to-body="true" :lock-scroll="true"
      :center="true" destroy-on-close class="device-form-dialog">
      <el-form ref="deviceFormRef" :model="deviceForm" :rules="deviceRules" label-width="100px" label-position="left">
        <el-form-item label="设备ID" prop="deviceId">
          <el-input v-model="deviceForm.deviceId" placeholder="请输入设备ID" />
        </el-form-item>
        <el-form-item label="设备描述" prop="description">
          <el-input v-model="deviceForm.description" type="textarea" :rows="3" placeholder="请输入设备描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="addDeviceDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitDeviceForm" :loading="loading">确认</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 设备电阻统计图对话框 -->
    <el-dialog v-model="statsDialogVisible" :title="`设备电阻数据统计`" width="60%" max-width="700px" top="10vh" center
      :fullscreen="false" destroy-on-close class="stats-dialog" append-to-body>
      <div class="stats-header">
        <div class="time-selector">
          <el-radio-group v-model="timeRange" size="small" @change="updateChart">
            <el-radio-button value="hour">1小时</el-radio-button>
            <el-radio-button value="half-day">12小时</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <div id="resistance-chart" class="chart-container" v-loading="chartLoading"></div>

      <div class="chart-legend">
        <div class="legend-item">
          <div class="legend-color" style="background-color: #409EFF;"></div>
          <span>电阻值 (Ω)</span>
        </div>
      </div>
    </el-dialog>
  </dashboard-layout>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import { Search, ArrowUp, ArrowDown, Minus, DataLine, Close, CircleCheck, CircleClose, Warning, Delete, Edit, Tools, Plus } from '@element-plus/icons-vue';
import * as echarts from 'echarts';
import { getDevicesByPage, getDeviceData, addDevice, deleteDevice as apiDeleteDevice } from '@/api/device';
import { EventSourcePolyfill } from 'event-source-polyfill';
import { useAuthStore } from '@/store/useAuthStore';
import { debounce } from 'lodash-es';

// 导入共享布局组件
import DashboardLayout from '@/components/layout/DashboardLayout.vue';

// 认证状态
const authStore = useAuthStore();

// SSE连接
let eventSource = null;
let deviceDataEventSource = null;

// 表格数据加载状态
const loading = ref(false);
const chartLoading = ref(false);

// 设备列表数据
const devices = ref([]);
const searchQuery = ref('');
const statusFilter = ref(null);
const currentPage = ref(1);
const pageSize = ref(5);
const totalDevices = ref(0);

// 新增设备对话框
const addDeviceDialogVisible = ref(false);
const deviceFormRef = ref(null);
const deviceForm = ref({
  deviceId: '',
  description: ''
});

// 设备详情对话框
const deviceDetailVisible = ref(false);
const selectedDevice = ref(null);

// 表单验证规则
const deviceRules = {
  deviceId: [{ required: true, message: '请输入设备ID', trigger: 'blur' }],
  description: [{ required: true, message: '请输入设备描述', trigger: 'blur' }]
};

// 设备状态映射
const statusMap = {
  0: { text: '在线', type: 'success' },
  1: { text: '离线', type: 'danger' },
  2: { text: '异常', type: 'warning' }
};

// 图表相关
const statsDialogVisible = ref(false);
const timeRange = ref('hour');
let resistanceChart = null;

// 防抖处理的搜索函数
const debouncedSearch = debounce(() => {
  currentPage.value = 1;
  fetchDevices();
}, 300);

// 监听搜索条件变化，自动触发搜索
watch(searchQuery, (newVal) => {
  if (newVal === '' || newVal === null) {
    debouncedSearch();
  }
});

// 获取设备列表
const fetchDevices = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value,
      size: pageSize.value
    };

    // 只有在有值时才添加参数，避免发送undefined或空字符串
    if (searchQuery.value) {
      params.search = searchQuery.value;
    }

    if (statusFilter.value !== null) {
      params.status = statusFilter.value;
    }

    const response = await getDevicesByPage(params);

    if (response && response.data) {
      devices.value = response.data.list || [];
      totalDevices.value = response.data.total || 0;

      // 如果当前页没有数据且不是第一页，回到上一页
      if (devices.value.length === 0 && currentPage.value > 1) {
        currentPage.value--;
        fetchDevices();
      }
    } else {
      handleEmptyResponse('获取设备列表失败，返回数据格式错误');
    }
  } catch (error) {
    console.error('获取设备列表失败:', error);
    handleApiError(error, '获取设备列表失败');
  } finally {
    loading.value = false;
  }
};

// 处理空响应
const handleEmptyResponse = (message) => {
  ElMessage.error(message);
  devices.value = [];
  totalDevices.value = 0;
};

// 处理API错误
const handleApiError = (error, defaultMessage) => {
  let errorMessage = defaultMessage;

  if (error.response) {
    // 服务器响应了，但状态码不在2xx范围
    errorMessage = `${defaultMessage}: ${error.response.data?.message || error.response.statusText || '未知错误'}`;
  } else if (error.request) {
    // 请求已发送但没有收到响应
    errorMessage = `${defaultMessage}: 服务器无响应`;
  } else {
    // 请求设置时出错
    errorMessage = `${defaultMessage}: ${error.message || '未知错误'}`;
  }

  ElMessage.error(errorMessage);
  console.error(errorMessage, error);
};

// 显示添加设备对话框
const showAddDeviceDialog = () => {
  deviceForm.value = {
    deviceId: '',
    description: ''
  };
  addDeviceDialogVisible.value = true;
};

// 提交设备表单
const submitDeviceForm = () => {
  if (!deviceFormRef.value) return;

  deviceFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true;
      try {
        const response = await addDevice(deviceForm.value);
        if (response.status === 200) {
          ElMessage.success('设备添加成功');
          addDeviceDialogVisible.value = false;
          resetForm();
          fetchDevices();
        } else {
          console.log(response);
          ElMessage.error(`添加设备失败：${response.message || '未知错误'}`);
        }
      } catch (error) {
        console.error('添加设备失败:', error);
        ElMessage.error(''+error);
      } finally {
        loading.value = false;
      }
    }
  });
};

// 重置表单
const resetForm = () => {
  if (deviceFormRef.value) {
    deviceFormRef.value.resetFields();
  }
  deviceForm.value = {
    deviceId: '',
    description: ''
  };
};

// 删除设备
const deleteDevice = (deviceId) => {
  ElMessageBox.confirm(
    '此操作将永久删除该设备，是否继续？',
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      const response = await apiDeleteDevice(deviceId);
      if (response.status === 200) {
        ElMessage.success('设备删除成功');
        fetchDevices(); // 刷新列表
      } else {
        ElMessage.error(`删除设备失败：${response.message || '未知错误'}`);
      }
    } catch (error) {
      console.error('删除设备失败:', error);
      ElMessage.error('删除设备失败，请稍后重试');
    }
  }).catch(() => {
    ElMessage.info('已取消删除');
  });
};

// 获取设备状态码
const getStatusCode = (status) => {
  // 处理空值
  if (status === null || status === undefined) {
    return 0;
  }

  // 处理可能的对象结构
  if (typeof status === 'object') {
    // 可能是 {code: 0} 格式
    if (status.code !== undefined) {
      return status.code;
    }
    // 可能是枚举对象格式
    else if (status.name !== undefined) {
      if (status.name === 'NORMAL') return 0;
      if (status.name === 'OFFLINE') return 1;
      if (status.name === 'ABNORMAL') return 2;
    }
  } else if (typeof status === 'number') {
    return status;
  } else if (typeof status === 'string') {
    // 尝试解析字符串格式的状态
    if (status === 'NORMAL') return 0;
    if (status === 'OFFLINE') return 1;
    if (status === 'ABNORMAL') return 2;
  }

  return 0;
};

// 获取设备状态文本
const getStatusText = (status) => {
  const code = getStatusCode(status);
  return statusMap[code]?.text || '未知';
};

// 获取设备状态对应的标签类型
const getStatusType = (status) => {
  const code = getStatusCode(status);
  return statusMap[code]?.type || '';
};

// 获取设备状态对应的类名
const getStatusClass = (status) => {
  const code = getStatusCode(status);
  if (code === 0) return 'online';
  if (code === 1) return 'offline';
  if (code === 2) return 'warning';
  return 'other';
};

// 获取趋势类名
const getTrendClass = (trend) => {
  if (trend === 'up') return 'trend-up';
  if (trend === 'down') return 'trend-down';
  return 'trend-stable';
};

// 获取趋势文本
const getTrendText = (trend) => {
  if (trend === 'up') return '上升';
  if (trend === 'down') return '下降';
  return '稳定';
};

// 初始化电阻图表
const initResistanceChart = async () => {
  const chartDom = document.getElementById('resistance-chart');
  if (!chartDom) return;

  chartLoading.value = true;

  // 清理旧实例
  if (resistanceChart) {
    resistanceChart.dispose();
  }

  // 创建ECharts实例
  resistanceChart = echarts.init(chartDom);

  try {
    // 显示加载中状态
    resistanceChart.showLoading({
      text: '正在加载数据...',
      maskColor: 'rgba(255, 255, 255, 0.8)',
      color: '#409EFF'
    });

    // 获取真实数据
    const response = await getDeviceData(selectedDevice.value.deviceId, timeRange.value);
    // 隐藏加载状态
    resistanceChart.hideLoading();

    // 处理并显示数据
    renderChartData(response);
  } catch (error) {
    console.error('获取设备数据失败:', error);
    handleApiError(error, '获取设备数据失败');

    // 隐藏加载状态
    resistanceChart.hideLoading();

    // 显示错误状态
    showChartError('获取数据失败，请重试');
  } finally {
    chartLoading.value = false;
  }

  // 绑定窗口调整事件
  window.addEventListener('resize', handleChartResize);
};

// 渲染图表数据
const renderChartData = (response) => {
  let resistanceData = [];
  let xAxisData = [];

  const hours = timeRange.value === 'hour' ? 1 : 12;
  const now = new Date();
  if (response && response.data && response.data.length > 0) {
    resistanceData = response.data;
    if (hours === 1) {
      // 1小时视图：每分钟一个点
      const points = 60;
      const interval = 60 * 1000;
      xAxisData = resistanceData.map((_, index) => {
        const time = new Date(now.getTime() - (resistanceData.length - 1 - index) * interval);
        return time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
      });
    } else {
      // 12小时视图：每小时一个点，从当前时间往前推，共13个点
      const points = 13;
      xAxisData = [];
      for (let i = points - 1; i >= 0; i--) {
        const time = new Date(now.getTime() - i * 60 * 60 * 1000);
        xAxisData.push(time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }));
      }
      // 补齐数据长度
      if (resistanceData.length < points) {
        resistanceData = Array(points - resistanceData.length).fill(null).concat(resistanceData);
      } else if (resistanceData.length > points) {
        resistanceData = resistanceData.slice(-points);
      }
    }
  } else {
    // 如果接口返回为空，设置空数据
    xAxisData = generateEmptyTimeData(hours);
    resistanceData = Array(xAxisData.length).fill(null);
  }
  // 图表配置
  const option = createChartOption(xAxisData, resistanceData);
  // 如果数据为空，显示无数据提示
  if (!resistanceData.some(v => v !== null && v !== undefined)) {
    option.graphic = [
      {
        type: 'text',
        left: 'center',
        top: 'middle',
        style: {
          text: '暂无数据',
          fontSize: 20,
          fill: '#999'
        }
      }
    ];
  }
  resistanceChart.setOption(option);
};

// 创建图表配置选项
const createChartOption = (xAxisData, resistanceData) => {
  return {
    title: {
      text: `${selectedDevice.value.deviceId} 电阻变化趋势`,
      left: 'center',
      textStyle: {
        color: '#333',
        fontSize: 16,
        fontWeight: 'normal'
      }
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'line',
        lineStyle: {
          color: '#409EFF'
        }
      },
      formatter: '{b}<br/>{a}: {c} Ω',
      backgroundColor: 'rgba(255, 255, 255, 0.9)',
      borderColor: '#e2e8f0',
      textStyle: {
        color: '#334155'
      },
      padding: [8, 12]
    },
    grid: {
      left: '5%',
      right: '5%',
      bottom: '10%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: xAxisData,
      axisLine: {
        lineStyle: {
          color: '#ddd'
        }
      },
      axisLabel: {
        color: '#666',
        fontSize: 12,
        rotate: timeRange.value === 'half-day' ? 30 : 0
      }
    },
    yAxis: {
      type: 'value',
      name: '电阻值 (Ω)',
      nameTextStyle: {
        color: '#666',
        fontSize: 12,
        padding: [0, 0, 0, 30]
      },
      axisLine: {
        show: true,
        lineStyle: {
          color: '#ddd'
        }
      },
      axisLabel: {
        color: '#666',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: '#eee'
        }
      }
    },
    series: [
      {
        name: '电阻值',
        type: 'line',
        smooth: true,
        data: resistanceData.length > 0 ? resistanceData : [],
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF'
        },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.1)' }
          ])
        },
        symbolSize: 6,
        showSymbol: false
      }
    ]
  };
};

// 显示图表错误
const showChartError = (message) => {
  if (resistanceChart) {
    resistanceChart.setOption({
      title: {
        text: '获取数据失败',
        left: 'center',
        textStyle: {
          color: '#f56c6c',
          fontSize: 16
        }
      },
      graphic: [
        {
          type: 'text',
          left: 'center',
          top: 'middle',
          style: {
            text: message,
            fontSize: 20,
            fill: '#f56c6c'
          }
        }
      ]
    });
  }
};

// 处理页面大小变化
const handleSizeChange = (val) => {
  pageSize.value = val;
  fetchDevices();
};

// 处理页码变化
const handleCurrentChange = (val) => {
  currentPage.value = val;
  fetchDevices();
};

// 显示设备电阻统计图
const showDeviceStats = (device) => {
  selectedDevice.value = device;
  statsDialogVisible.value = true;

  // 等待DOM更新后初始化图表
  setTimeout(() => {
    initResistanceChart();
  }, 100);

  // 开始监听当前设备的数据更新
  setupDeviceDataSSE(device.deviceId);
};

// SSE状态管理
const sseStatus = ref({
  deviceList: {
    connected: false,
    reconnecting: false,
    error: null
  },
  deviceData: {
    connected: false,
    reconnecting: false,
    error: null,
    deviceId: null
  }
});

// 建立设备SSE连接
const setupSSEConnection = () => {
  // 关闭已有连接
  closeSSEConnection();

  // 创建新的SSE连接
  const userId = authStore.userId;
  const token = authStore.accessToken;

  if (!userId) {
    console.warn('未找到用户ID，无法建立SSE连接');
    sseStatus.value.deviceList.error = '未找到用户ID';
    return;
  }

  if (!token) {
    console.warn('未找到认证令牌，无法建立SSE连接');
    sseStatus.value.deviceList.error = '未找到认证令牌';
    return;
  }

  try {
    console.log('正在建立设备列表SSE连接...');
    sseStatus.value.deviceList.reconnecting = true;

    // 使用EventSourcePolyfill，支持添加自定义头部
    eventSource = new EventSourcePolyfill('http://localhost:8080/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000, // 心跳超时时间60秒
      connectionTimeout: 30000, // 连接超时30秒
      reconnectInterval: 5000  // 重连间隔5秒
    });

    // 连接建立时的处理
    eventSource.onopen = () => {
      console.log('设备列表SSE连接已建立');
      sseStatus.value.deviceList.connected = true;
      sseStatus.value.deviceList.reconnecting = false;
      sseStatus.value.deviceList.error = null;
    };

    // 接收到消息时的处理
    eventSource.addEventListener(userId.toString(), (event) => {
      console.log('设备列表收到设备更新消息:', event.data);

      // 当消息为设备更新时，重新获取设备数据
      if (event.data === 'device-update') {
        console.log('检测到设备更新，使用当前筛选条件重新获取数据');

        // 显示正在更新的提示
        ElMessage({
          message: '检测到设备更新，正在刷新数据...',
          type: 'info',
          duration: 2000
        });

        // 使用当前筛选条件重新获取设备数据
        fetchDevices();
      }
    });

    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      // 只在开发环境输出心跳日志
      if (process.env.NODE_ENV === 'development') {
        console.log('设备列表收到心跳消息:', event.data);
      }
    });

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);
      sseStatus.value.deviceList.connected = false;
      sseStatus.value.deviceList.error = '连接错误';

      // 检查是否为跨域错误
      if (error && error.status === 0) {
        console.error('可能存在跨域问题，请检查CORS配置');
        sseStatus.value.deviceList.error = '跨域请求被拒绝';
      }

      closeSSEConnection();

      // 尝试重新连接
      setTimeout(() => {
        console.log('正在尝试重新连接SSE...');
        setupSSEConnection();
      }, 5000);
    };
  } catch (error) {
    console.error('建立SSE连接失败:', error);
    sseStatus.value.deviceList.connected = false;
    sseStatus.value.deviceList.reconnecting = false;
    sseStatus.value.deviceList.error = error.message || '连接失败';
  }
};

// 建立设备数据SSE连接
const setupDeviceDataSSE = (deviceId) => {
  // 关闭已有连接
  closeDeviceDataSSE();

  const userId = authStore.userId;
  const token = authStore.accessToken;

  if (!userId || !deviceId) {
    console.warn('未找到用户ID或设备ID，无法建立SSE连接');
    sseStatus.value.deviceData.error = '未找到用户ID或设备ID';
    return;
  }

  if (!token) {
    console.warn('未找到认证令牌，无法建立SSE连接');
    sseStatus.value.deviceData.error = '未找到认证令牌';
    return;
  }

  try {
    console.log(`正在建立设备(${deviceId})数据监听SSE连接...`);
    sseStatus.value.deviceData.reconnecting = true;
    sseStatus.value.deviceData.deviceId = deviceId;

    // 使用EventSourcePolyfill，支持添加自定义头部
    deviceDataEventSource = new EventSourcePolyfill('http://localhost:8080/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000,
      connectionTimeout: 30000,
      reconnectInterval: 5000
    });

    // 连接建立时的处理
    deviceDataEventSource.onopen = () => {
      console.log(`设备(${deviceId})数据监听SSE连接已建立`);
      sseStatus.value.deviceData.connected = true;
      sseStatus.value.deviceData.reconnecting = false;
      sseStatus.value.deviceData.error = null;
    };

    // 接收到消息时的处理
    deviceDataEventSource.addEventListener(deviceId.toString(), (event) => {
      console.log(`收到设备${deviceId}数据更新消息:`, event.data);

      // 检测到设备数据更新时，刷新图表
      // 接收的消息格式为 device-data-update:设备ID
      const expectedMessage = `device-data-update:${deviceId}`;
      if (event.data === expectedMessage) {
        console.log(`检测到设备(${deviceId})数据更新，正在刷新图表...`);

        // 更新图表
        if (statsDialogVisible.value && selectedDevice.value && selectedDevice.value.deviceId === deviceId) {
          updateChart();
        }
      }
    });

    // 接收心跳消息
    deviceDataEventSource.addEventListener('heartbeat', (event) => {
      // 只在开发环境输出心跳日志
      if (process.env.NODE_ENV === 'development') {
        console.log(`设备(${deviceId})数据SSE收到心跳消息:`, event.data);
      }
    });

    // 错误处理
    deviceDataEventSource.onerror = (error) => {
      console.error('设备数据SSE连接错误:', error);
      sseStatus.value.deviceData.connected = false;
      sseStatus.value.deviceData.error = '连接错误';

      closeDeviceDataSSE();

      // 如果图表仍然显示，尝试重新连接
      if (statsDialogVisible.value && selectedDevice.value && selectedDevice.value.deviceId === deviceId) {
        setTimeout(() => {
          console.log(`正在尝试重新连接设备(${deviceId})数据SSE...`);
          setupDeviceDataSSE(deviceId);
        }, 5000);
      }
    };
  } catch (error) {
    console.error('建立设备数据SSE连接失败:', error);
    sseStatus.value.deviceData.connected = false;
    sseStatus.value.deviceData.reconnecting = false;
    sseStatus.value.deviceData.error = error.message || '连接失败';
  }
};

// 关闭SSE连接
const closeSSEConnection = () => {
  if (eventSource) {
    console.log('正在关闭设备列表SSE连接...');
    eventSource.close();
    eventSource = null;
  }
};

// 关闭设备数据SSE连接
const closeDeviceDataSSE = () => {
  if (deviceDataEventSource) {
    console.log('正在关闭设备数据SSE连接...');
    deviceDataEventSource.close();
    deviceDataEventSource = null;
  }
};

// 更新电阻统计图
const updateChart = () => {
  initResistanceChart();
};

// 处理图表大小调整
const handleChartResize = () => {
  if (resistanceChart) {
    resistanceChart.resize();
  }
};

// 组件销毁前清理
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleChartResize);
  if (resistanceChart) {
    resistanceChart.dispose();
  }
  closeSSEConnection();
  closeDeviceDataSSE();
});

// 页面加载时获取设备列表和建立SSE连接
onMounted(() => {
  fetchDevices();
  setupSSEConnection();
});

// 处理搜索
const handleSearch = () => {
  currentPage.value = 1;
  fetchDevices();
};

// 生成空时间轴数据
const generateEmptyTimeData = (hours) => {
  const data = [];
  const now = new Date();
  if (hours === 1) {
    // 1小时视图：每分钟一个点
    const points = 60;
    for (let i = points; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60 * 1000);
      data.push(time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }));
    }
  } else {
    // 12小时视图：每小时一个点，从当前时间往前推，共13个点
    const points = 13;
    for (let i = points - 1; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 60 * 60 * 1000);
      data.push(time.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' }));
    }
  }
  return data;
};

// 监听对话框变化
watch(statsDialogVisible, (newVal) => {
  if (!newVal && deviceDataEventSource) {
    // 当图表对话框关闭时，关闭数据监听
    console.log('设备图表对话框关闭，关闭数据监听');
    closeDeviceDataSSE();
  }
});
</script>

<style scoped>
.page-title {
  margin-top: 0;
  margin-bottom: 28px;
  color: #1e293b;
  font-size: 28px;
  font-weight: 700;
  position: relative;
  padding-left: 18px;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  height: 75%;
  width: 6px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 4px;
}

.filter-container {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 28px;
  flex-wrap: wrap;
  background-color: white;
  padding: 20px;
  border-radius: 14px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  border-top: 4px solid #3b82f6;
}

.filter-container:hover {
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08);
}

.search-input {
  width: 320px;
}

.filter-select {
  width: 150px;
}

.device-table-card {
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.07);
  margin-bottom: 30px;
  border: none;
  overflow: hidden;
  transition: all 0.3s ease;
}

.device-table-card:hover {
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.1);
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 20px;
}

.table-title {
  margin: 0;
  font-size: 20px;
  color: #1e293b;
  font-weight: 600;
  position: relative;
  padding-left: 16px;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.table-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 4px;
  height: 75%;
  width: 5px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 3px;
}

.stats-button {
  background: linear-gradient(135deg, #3b82f6, #60a5fa);
  border: none;
  padding: 10px 20px;
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.3);
  transition: all 0.3s ease;
  font-weight: 500;
  border-radius: 8px;
}

.stats-button:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(59, 130, 246, 0.4);
}

.pagination-container {
  margin-top: 28px;
  display: flex;
  justify-content: flex-end;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
}

.status-tag {
  padding: 5px 12px;
  border-radius: 6px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}

.status-tag.online {
  color: #10b981;
}

.status-tag.offline {
  color: #f43f5e;
}

.status-tag.warning {
  color: #f59e0b;
}

.status-tag.other {
  color: #6366f1;
}

.status-icon {
  margin-right: 3px;
  vertical-align: middle;
}

.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
}

.action-btn {
  min-width: 72px !important;
  height: 34px !important;
  padding: 0 12px !important;
  font-size: 13px !important;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  border: none !important;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.08);
  border-radius: 6px !important;
  gap: 4px;
  font-weight: 500 !important;
  letter-spacing: 0.5px;
}

.data-btn {
  background: linear-gradient(145deg, #3b82f6, #60a5fa) !important;
  color: white !important;
}

.delete-btn {
  background: linear-gradient(145deg, #ef4444, #f87171) !important;
  color: white !important;
}

.refresh-btn {
  background: linear-gradient(145deg, #0ea5e9, #38bdf8) !important;
  color: white !important;
}

.action-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
  filter: brightness(105%);
}

.action-btn:active {
  transform: translateY(0);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  filter: brightness(95%);
}

/* 移除冲突的按钮样式 */
.filter-container .el-button {
  height: 40px;
}

.chart-container {
  width: 100%;
  height: 350px;
  margin-top: 16px;
  border-radius: 10px;
  overflow: hidden;
  background-color: #f8fafc;
  box-shadow: inset 0 0 8px rgba(0, 0, 0, 0.04);
  border: 1px solid #f1f5f9;
}

.stats-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 16px;
}

.time-selector {
  background-color: #f8fafc;
  padding: 8px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 20px;
  flex-wrap: wrap;
  padding: 12px 16px;
  background-color: #f8fafc;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #334155;
  font-weight: 500;
}

.legend-color {
  width: 18px;
  height: 18px;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.15);
}

/* 统计对话框样式 */
:deep(.stats-dialog .el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  animation: dialogFadeIn 0.4s ease-out;
}

@keyframes dialogFadeIn {
  from {
    opacity: 0;
    transform: translateY(30px) scale(0.95);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

:deep(.stats-dialog .el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 20px 24px;
  margin: 0;
  background: linear-gradient(90deg, #f8fafc, #f1f5f9);
}

:deep(.stats-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
  background: linear-gradient(90deg, #1e293b, #334155);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

:deep(.stats-dialog .el-dialog__body) {
  padding: 24px 28px;
}

:deep(.stats-dialog .el-dialog__headerbtn) {
  top: 20px;
  right: 20px;
}

:deep(.stats-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #64748b;
  font-size: 18px;
  transition: all 0.3s ease;
}

:deep(.stats-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
  color: #3b82f6;
  transform: rotate(90deg);
}

/* 表格样式增强 */
:deep(.el-table) {
  border-radius: 10px;
  overflow: hidden;
  border: 1px solid #f1f5f9;
}

:deep(.el-table th) {
  background-color: #f8fafc !important;
  font-weight: 600 !important;
  color: #1e293b !important;
  padding: 16px 0;
  border-bottom: 2px solid #e2e8f0;
}

:deep(.el-table--border .el-table__cell) {
  border-right: 1px solid #e2e8f0;
}

:deep(.el-table tr) {
  transition: all 0.3s ease;
}

:deep(.el-table tr:hover) {
  background-color: #f1f5f9 !important;
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.05);
}

:deep(.el-button) {
  transition: all 0.3s ease;
}

:deep(.el-button:not(.stats-button)) {
  border-radius: 6px;
}

:deep(.el-button:not(.stats-button):hover) {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

/* 模态遮罩增强 */
:deep(.el-overlay) {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  z-index: 2000;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  overflow: auto;
}

@media (max-width: 768px) {
  .stats-header {
    flex-direction: column;
    gap: 16px;
  }

  .time-selector {
    margin-left: 0;
    width: 100%;
    display: flex;
    justify-content: center;
  }

  .filter-container {
    flex-direction: column;
    align-items: stretch;
    padding: 16px;
  }

  .search-input,
  .filter-select {
    width: 100%;
  }

  .chart-container {
    height: 300px;
  }

  .table-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }

  .stats-button {
    width: 100%;
  }

  .device-detail-actions {
    flex-direction: column;
    align-items: center;
  }
}

/* 状态文本样式 */
.status-text {
  display: flex;
  align-items: center;
  gap: 5px;
  font-weight: 500;
  white-space: nowrap;
}

.status-text.online {
  color: #10b981;
}

.status-text.offline {
  color: #f43f5e;
}

.status-text.warning {
  color: #f59e0b;
}

.status-text.other {
  color: #6366f1;
}

.status-icon {
  vertical-align: middle;
}

/* 对话框样式 */
:deep(.device-form-dialog),
:deep(.device-detail-dialog),
:deep(.stats-dialog) {
  display: flex;
  align-items: center;
  justify-content: center;
}

:deep(.device-form-dialog .el-dialog),
:deep(.device-detail-dialog .el-dialog),
:deep(.stats-dialog .el-dialog) {
  margin: 0 auto !important;
  position: relative;
  border-radius: 12px;
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.1), 0 2px 6px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

:deep(.device-form-dialog .el-dialog__header),
:deep(.device-detail-dialog .el-dialog__header),
:deep(.stats-dialog .el-dialog__header) {
  padding: 20px 24px;
  margin: 0;
  border-bottom: 1px solid #f1f5f9;
  background: #f8fafc;
}

:deep(.device-form-dialog .el-dialog__title),
:deep(.device-detail-dialog .el-dialog__title),
:deep(.stats-dialog .el-dialog__title) {
  font-weight: 600;
  font-size: 18px;
  color: #1e293b;
}

:deep(.device-form-dialog .el-dialog__body),
:deep(.device-detail-dialog .el-dialog__body),
:deep(.stats-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.device-form-dialog .el-dialog__footer),
:deep(.device-detail-dialog .el-dialog__footer),
:deep(.stats-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid #f1f5f9;
  background: #f8fafc;
}
</style>