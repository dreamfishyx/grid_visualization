<template>
  <!-- 首页概览组件 -->
  <div class="dashboard-overview">
    <!-- 顶部设备状态卡片 -->
    <el-row :gutter="20">
      <el-col :xs="24" :sm="8" :md="8" :lg="8" :xl="8" v-for="(card, index) in statusCards" :key="index">
        <status-card :card-data="card" @card-click="handleCardClick" />
      </el-col>
    </el-row>

    <!-- 设备分布地图概览 -->
    <china-map :loading="mapLoading" :options="mapOptions" :province-data="provinceData"
      @province-click="handleProvinceClick" />

    <!-- 省份详情对话框 -->
    <province-detail v-model="provinceDetailVisible" :province="selectedProvince" @view-detail="viewProvinceDetail" />
  </div>
</template>

<script setup>
import { ref, onMounted, computed, onBeforeUnmount } from 'vue';
import { ElMessage } from 'element-plus';
import { EventSourcePolyfill } from 'event-source-polyfill';

import StatusCard from '@/components/dashboard/StatusCard.vue';
import ChinaMap from '@/components/dashboard/ChinaMap.vue';
import ProvinceDetail from '@/components/dashboard/ProvinceDetail.vue';
import { getAllDevices } from '@/api/device';
import { useAuthStore } from '@/store/useAuthStore';

// 认证状态
const authStore = useAuthStore();

// 加载状态
const loading = ref(false);
const mapLoading = ref(false);

// 设备数据
const devices = ref([]);

// SSE连接
let eventSource = null;

// 地图配置对象
const mapOptions = {
  visualMap: {
    min: 0,
    max: 10, // 降低最大值，使差异更明显
    text: ['设备多', '设备少'],
    realtime: true,
    calculable: true,
    inRange: {
      // 使用更明显的色彩差异
      color: ['#edf8fb', '#b2e2e2', '#66c2a4', '#2ca25f', '#006d2c']
    },
    itemWidth: 30,
    itemHeight: 120,
    textStyle: {
      color: '#333'
    }
  },
  series: {
    itemStyle: {
      areaColor: '#f5f5f5',
      borderColor: '#cbd5e1',
      borderWidth: 0.5
    },
    emphasis: {
      itemStyle: {
        areaColor: '#3b82f6',
        shadowColor: 'rgba(0, 0, 0, 0.5)',
        shadowBlur: 10,
        shadowOffsetX: 0,
        shadowOffsetY: 5
      },
      label: {
        color: '#1e293b'
      }
    }
  }
};

// 省份详情状态
const provinceDetailVisible = ref(false);
const selectedProvince = ref(null);

// 处理卡片点击
const handleCardClick = (type) => {
  if (type === 'online') {
    ElMessage.info(`在线设备统计: ${onlineCount.value}台`);
  } else if (type === 'offline') {
    ElMessage.info(`离线设备统计: ${offlineCount.value}台`);
  } else {
    ElMessage.info(`异常设备统计: ${abnormalCount.value}台`);
  }
};

// 处理省份点击
const handleProvinceClick = (province) => {
  selectedProvince.value = province;
  provinceDetailVisible.value = true;
};

// 查看省份详情
const viewProvinceDetail = () => {
  provinceDetailVisible.value = false;
};

// 获取设备数据
const fetchDevices = async () => {
  loading.value = true;
  mapLoading.value = true;

  try {
    const response = await getAllDevices();
    console.log(response);
    devices.value = response.data || [];
    console.log('设备数据:', devices.value);
    processDeviceData();
  } catch (error) {
    console.error('获取设备列表失败:', error);
    ElMessage.error('获取设备列表失败，请稍后重试');
  } finally {
    loading.value = false;
    mapLoading.value = false;
  }
};

// 处理设备数据，计算统计信息和地图数据
const processDeviceData = () => {
  // 更新统计卡片数据
  updateStatusCards();

  // 更新地图数据
  updateProvinceData();
};

// 在线设备数量
const onlineCount = computed(() => {
  return devices.value.filter(device =>
    device.status === 'NORMAL' ||
    device.status === 0 ||
    device.status?.code === 0
  ).length;
});

// 离线设备数量
const offlineCount = computed(() => {
  return devices.value.filter(device =>
    device.status === 'OFFLINE' ||
    device.status === 1 ||
    device.status?.code === 1
  ).length;
});

// 异常设备数量
const abnormalCount = computed(() => {
  return devices.value.filter(device =>
    device.status === 'ABNORMAL' ||
    device.status === 3 ||
    device.status?.code === 3
  ).length;
});

// 总设备数量
const totalCount = computed(() => {
  return devices.value.length;
});

// 状态卡片数据
const statusCards = ref([
  {
    type: 'online',
    title: '在线设备',
    value: 0,
    percentage: 0,
    color: '#10b981'
  },
  {
    type: 'offline',
    title: '故障设备',
    value: 0,
    percentage: 0,
    color: '#f43f5e'
  },
  {
    type: 'anomaly',
    title: '异常设备',
    value: 0,
    percentage: 0,
    color: '#f59e0b'
  }
]);

// 更新统计卡片数据
const updateStatusCards = () => {
  const total = totalCount.value || 1; // 避免除以零

  statusCards.value[0].value = onlineCount.value;
  statusCards.value[0].percentage = Math.round((onlineCount.value / total) * 100);

  statusCards.value[1].value = offlineCount.value;
  statusCards.value[1].percentage = Math.round((offlineCount.value / total) * 100);

  statusCards.value[2].value = abnormalCount.value;
  statusCards.value[2].percentage = Math.round((abnormalCount.value / total) * 100);
};

// 省份数据 (用于地图展示)
const provinceData = ref([]);

// 更新地图数据
const updateProvinceData = () => {
  // 按城市分组设备
  const cityGroups = {};

  console.log('开始按城市统计设备数据，总数:', devices.value.length);

  // 先按城市名称分组统计设备数量
  devices.value.forEach(device => {
    if (!device.city) {
      console.log('设备无城市信息:', device);
      return;
    }

    // 使用完整城市名称作为键
    const cityName = device.city;
    console.log('处理设备:', device.id, '城市:', cityName);

    if (!cityGroups[cityName]) {
      cityGroups[cityName] = {
        devices: [],
        online: 0,
        offline: 0,
        anomaly: 0
      };
    }

    cityGroups[cityName].devices.push(device);

    // 统计不同状态的设备数量
    if (device.status === 'NORMAL' || device.status === 0 || device.status?.code === 0) {
      cityGroups[cityName].online++;
    } else if (device.status === 'OFFLINE' || device.status === 1 || device.status?.code === 1) {
      cityGroups[cityName].offline++;
    } else if (device.status === 'ABNORMAL' || device.status === 3 || device.status?.code === 3) {
      cityGroups[cityName].anomaly++;
    }
  });

  console.log('城市分组统计结果:', cityGroups);

  // 转换为地图需要的数据格式
  provinceData.value = Object.keys(cityGroups).map(name => {
    const group = cityGroups[name];
    const totalDevices = group.devices.length;

    return {
      name, // 使用完整城市名称
      value: totalDevices,
      online: group.online,
      offline: group.offline,
      anomaly: group.anomaly
    };
  });

  console.log('地图数据统计结果:', provinceData.value);
};

// 建立SSE连接
const setupSSEConnection = () => {

  // if (eventSource && eventSource.readyState !== 2) {
  //   console.log('SSE连接已存在，复用现有连接');
  //   return;
  // }

  // 关闭已有连接
  closeSSEConnection();

  // 创建新的SSE连接
  const userId = authStore.userId;
  const token = authStore.accessToken;

  if (!userId) {
    console.warn('未找到用户ID，无法建立SSE连接');
    return;
  }

  if (!token) {
    console.warn('未找到认证令牌，无法建立SSE连接');
    return;
  }

  try {
    console.log('首页概览正在建立SSE连接...');

    // 使用EventSourcePolyfill，支持添加自定义头部
    eventSource = new EventSourcePolyfill('http://localhost:8080/sse/subscribe', {
      headers: {
        'Authorization': `Bearer ${token}`
      },
      heartbeatTimeout: 60000, // 增加心跳超时时间到60秒
      connectionTimeout: 30000, // 连接超时30秒
      reconnectInterval: 5000 // 重连间隔5秒
    });

    // 连接建立时的处理
    eventSource.onopen = () => {
      console.log('SSE连接已建立');
    };

    // 接收到消息时的处理
    eventSource.addEventListener(userId.toString(), (event) => {
      console.log('收到设备更新消息:', event.data);

      // 当消息为设备更新时，重新获取设备数据
      if (event.data === 'device-update') {
        console.log('设备数据已更新，正在重新获取...');
        fetchDevices();
      }
    });

    // 接收心跳消息
    eventSource.addEventListener('heartbeat', (event) => {
      console.log('首页概览收到心跳消息:', event.data);
    });

    // 错误处理
    eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);

      // 检查是否为跨域错误
      if (error && error.status === 0) {
        console.error('可能存在跨域问题，请检查CORS配置');
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
  }
};

// 关闭SSE连接
const closeSSEConnection = () => {
  if (eventSource) {
    console.log('正在关闭SSE连接...');
    eventSource.close();
    eventSource = null;
  }
};

// 组件挂载时获取数据并建立SSE连接
onMounted(() => {
  fetchDevices();
  setupSSEConnection();
});

// 组件卸载前的清理工作
onBeforeUnmount(() => {
  closeSSEConnection();
});
</script>

<style scoped>
.dashboard-overview {
  width: 100%;
}

.page-title {
  margin-top: 0;
  margin-bottom: 28px;
  color: #1e293b;
  font-size: 26px;
  font-weight: 700;
  position: relative;
  padding-left: 12px;
}

.page-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 6px;
  height: 70%;
  width: 4px;
  background: linear-gradient(180deg, #3b82f6 0%, #60a5fa 100%);
  border-radius: 4px;
}
</style>